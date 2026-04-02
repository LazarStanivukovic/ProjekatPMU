package com.example.projekat.data.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Direct client for Pollinations.ai API.
 * Replaces the Ktor server middleman by calling the AI API directly from Android.
 */
@Singleton
class PollinationsAiClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)  // AI can be slow
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val apiUrl = "https://text.pollinations.ai"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val TAG = "PollinationsAiClient"
    }

    /**
     * Generate an AI-optimized schedule for the given tasks.
     * 
     * @param tasks List of tasks with name, priority, and deadline
     * @return List of scheduled tasks with suggested dates
     * @throws Exception if the API call fails or response is invalid
     */
    suspend fun generateSchedule(tasks: List<TaskItem>): List<ScheduledTask> {
        val prompt = buildPrompt(tasks)
        val requestBody = buildRequestBody(prompt)

        Log.d(TAG, "Sending request to Pollinations.ai with ${tasks.size} tasks")

        val request = Request.Builder()
            .url(apiUrl)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        
        if (!response.isSuccessful) {
            throw Exception("Pollinations.ai API error: ${response.code}")
        }

        val responseText = response.body?.string() 
            ?: throw Exception("Empty response from Pollinations.ai")

        Log.d(TAG, "Received response: $responseText")

        return parseAiResponse(responseText, tasks)
    }

    /**
     * Build the prompt for the AI to schedule tasks.
     */
    private fun buildPrompt(tasks: List<TaskItem>): String {
        val taskListText = tasks.mapIndexed { index, task ->
            "${index + 1}. \"${task.name}\" - prioritet: ${task.priority}, krajnji rok: ${task.deadline}"
        }.joinToString("\n")

        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = dateFormat.format(calendar.time)

        return """
Napravi optimalan raspored za sledece taskove.
Danasnji datum je: $today

Taskovi:
$taskListText

Pravila za raspored:
- Najraniji moguc datum za raspored je $tomorrow (NIKADA nemoj staviti danasnji datum $today)
- HIGH prioritet: zavrsi sto pre (najblize sutrasnjim datumu $tomorrow)
- MEDIUM prioritet: rasporedi ravnomerno izmedju $tomorrow i krajnjeg roka
- LOW prioritet: moze da saceka ali mora biti pre krajnjeg roka
- Svaki scheduledDate mora biti izmedju $tomorrow i krajnjeg roka taska (ukljucivo)
- Ne stavljaj previse taskova na isti dan

Odgovori SAMO sa JSON nizom, bez ikakvog dodatnog teksta, objasnjenja ili markdown formatiranja.
Format: [{"name":"tacno ime taska","scheduledDate":"YYYY-MM-DD"},{"name":"tacno ime taska 2","scheduledDate":"YYYY-MM-DD"}]
Mora biti niz sa ${tasks.size} elemenata, po jedan za svaki task.
        """.trimIndent()
    }

    /**
     * Build the JSON request body for Pollinations.ai API.
     */
    private fun buildRequestBody(prompt: String): okhttp3.RequestBody {
        val request = PollinationsRequest(
            messages = listOf(
                Message(
                    role = "system",
                    content = "Ti si API koji vraca ISKLJUCIVO JSON nizove. Nikada ne dodajes tekst, objasnjenja ili markdown. Uvek vratis kompletan niz sa svim taskovima."
                ),
                Message(
                    role = "user",
                    content = prompt
                )
            ),
            model = "openai",
            seed = (1..999999999).random(),
            private = true
        )

        val json = gson.toJson(request)
        return json.toRequestBody(jsonMediaType)
    }

    /**
     * Parse AI response with robust error handling.
     * Handles: arrays, objects with nested arrays, markdown code blocks, extra text.
     */
    private fun parseAiResponse(responseText: String, originalTasks: List<TaskItem>): List<ScheduledTask> {
        try {
            val jsonText = extractJson(responseText)
            val jsonElement = JsonParser.parseString(jsonText)

            val taskList = when {
                // Case 1: Response is a JSON array - ideal case
                jsonElement.isJsonArray -> {
                    parseJsonArray(jsonElement.asJsonArray)
                }
                // Case 2: Response is a single JSON object
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    // Check if it contains a nested array (scheduledTasks, tasks, schedule)
                    val innerArray = obj.getAsJsonArray("scheduledTasks")
                        ?: obj.getAsJsonArray("tasks")
                        ?: obj.getAsJsonArray("schedule")

                    if (innerArray != null) {
                        parseJsonArray(innerArray)
                    } else {
                        // Single object with name/scheduledDate
                        listOf(parseSingleTask(obj))
                    }
                }
                else -> throw Exception("Unexpected JSON element type")
            }

            // If we got fewer results than tasks, fill in missing ones with deadline fallback
            return fillMissingTasks(taskList, originalTasks)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response: ${e.message}")
            Log.e(TAG, "Raw response: $responseText")

            // Fallback: return original tasks with their deadlines
            return originalTasks.map { task ->
                ScheduledTask(
                    name = task.name,
                    scheduledDate = task.deadline
                )
            }
        }
    }

    /**
     * Parse a JSON array into a list of ScheduledTask.
     */
    private fun parseJsonArray(array: JsonArray): List<ScheduledTask> {
        return array.mapNotNull { element ->
            try {
                if (element.isJsonObject) {
                    parseSingleTask(element.asJsonObject)
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse task: ${e.message}")
                null
            }
        }
    }

    /**
     * Parse a single task JSON object.
     */
    private fun parseSingleTask(obj: JsonObject): ScheduledTask {
        val name = obj.get("name")?.asString ?: ""
        val scheduledDate = obj.get("scheduledDate")?.asString ?: ""
        return ScheduledTask(name, scheduledDate)
    }

    /**
     * Fill in missing tasks with their original deadlines.
     */
    private fun fillMissingTasks(
        taskList: List<ScheduledTask>,
        originalTasks: List<TaskItem>
    ): List<ScheduledTask> {
        if (taskList.size >= originalTasks.size) {
            return taskList
        }

        val resultNames = taskList.map { it.name }.toSet()
        val missing = originalTasks
            .filter { it.name !in resultNames }
            .map { task ->
                ScheduledTask(name = task.name, scheduledDate = task.deadline)
            }

        return taskList + missing
    }

    /**
     * Extract JSON from AI response text.
     * The AI might return a JSON array, a JSON object, wrap it in markdown code blocks, or add extra text.
     */
    private fun extractJson(text: String): String {
        val cleaned = text.trim()

        // Case 1: If it starts with [ or { directly - it's already JSON
        if (cleaned.startsWith("[") || cleaned.startsWith("{")) {
            val startChar = cleaned[0]
            val endChar = if (startChar == '[') ']' else '}'
            val endIndex = cleaned.lastIndexOf(endChar)
            if (endIndex >= 0) {
                return cleaned.substring(0, endIndex + 1)
            }
        }

        // Case 2: Try to extract from markdown code block ```json ... ``` or ``` ... ```
        val codeBlockRegex = Regex("```(?:json)?\\s*\\n?([\\[{].*?[}\\]])\\s*\\n?```", RegexOption.DOT_MATCHES_ALL)
        codeBlockRegex.find(cleaned)?.let {
            return it.groupValues[1]
        }

        // Case 3: Try to find any JSON array in the text
        val arrayRegex = Regex("\\[\\s*\\{.*}\\s*]", RegexOption.DOT_MATCHES_ALL)
        arrayRegex.find(cleaned)?.let {
            return it.value
        }

        // Case 4: Try to find any JSON object in the text
        val objectRegex = Regex("\\{.*}", RegexOption.DOT_MATCHES_ALL)
        objectRegex.find(cleaned)?.let {
            return it.value
        }

        return cleaned
    }
}
