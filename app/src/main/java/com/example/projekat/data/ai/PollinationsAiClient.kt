package com.example.projekat.data.ai

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
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

@Singleton
class PollinationsAiClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val apiUrl = "https://text.pollinations.ai"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val TAG = "PollinationsAiClient"
        private const val MAX_RETRIES = 2
        private const val INITIAL_RETRY_DELAY_MS = 3000L
    }

    suspend fun generateSchedule(tasks: List<TaskItem>, customPrompt: String? = null): List<ScheduledTask> {
        val models = listOf("openai", "mistral")
        var lastError: Exception? = null

        for (model in models) {
            try {
                return tryWithModel(tasks, customPrompt, model)
            } catch (e: Exception) {
                Log.w(TAG, "Model $model failed: ${e.message}")
                lastError = e
            }
        }

        throw lastError ?: Exception("All AI models failed")
    }

    private suspend fun tryWithModel(tasks: List<TaskItem>, customPrompt: String?, model: String): List<ScheduledTask> {
        val prompt = buildPrompt(tasks, customPrompt)
        Log.d(TAG, "Sending request to Pollinations.ai (model=$model) with ${tasks.size} tasks")

        var lastError: Exception? = null
        var delayMs = INITIAL_RETRY_DELAY_MS

        for (attempt in 1..MAX_RETRIES) {
            if (attempt > 1) {
                Log.d(TAG, "Retry $attempt/$MAX_RETRIES after ${delayMs}ms")
                delay(delayMs)
            }

            val requestBody = buildRequestBody(prompt, model)
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseText = response.body?.string()
                    ?: throw Exception("Empty response from Pollinations.ai")
                Log.d(TAG, "Response received (${responseText.length} chars)")
                return parseAiResponse(responseText, tasks)
            }

            if ((response.code == 429 || response.code == 504) && attempt < MAX_RETRIES) {
                val reason = if (response.code == 429) "Rate limited" else "Gateway timeout"
                Log.w(TAG, "$reason ($response.code), retrying...")
                lastError = Exception("$model API error: ${response.code} - $reason")
                delayMs *= 2
                continue
            }

            throw Exception("$model API error: ${response.code}")
        }

        throw lastError ?: Exception("$model failed after $MAX_RETRIES retries")
    }

    private fun buildPrompt(tasks: List<TaskItem>, customPrompt: String?): String {
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val tomorrow = dateFormat.format(calendar.time)

        val taskLines = tasks.joinToString("\n") { task ->
            val prio = when {
                task.priority.toIntOrNull() != null -> {
                    when (task.priority.toInt()) {
                        in 8..10 -> "visok"
                        in 4..7 -> "srednji"
                        else -> "nizak"
                    }
                }
                else -> "srednji"
            }
            val dates = if (task.hasTime) {
                val end = task.endDateTime?.let { " do $it" } ?: ""
                "${task.startDateTime}$end (sa vremenom)"
            } else {
                val end = task.endDateTime?.let { " do $it" } ?: ""
                "${task.startDateTime}$end"
            }
            "- \"${task.name}\" [$prio] $dates"
        }

        val custom = if (!customPrompt.isNullOrBlank()) "\nKorisnik: $customPrompt\n" else ""

        val dateExample = if (tasks.any { it.hasTime }) {
            """- sa vremenom: {"name":"task","newStartDateTime":"2026-06-20 14:00","newEndDateTime":"2026-06-20 16:00","hasTime":true}"""
        } else ""
        val dateOnlyExample = if (tasks.any { !it.hasTime }) {
            """- bez vremena: {"name":"task","newStartDateTime":"2026-06-20","newEndDateTime":"2026-06-20","hasTime":false}"""
        } else ""

        return """
Rasporedi taskove od $tomorrow pa nadalje. Danas: $today.
$custom
Taskovi:
$taskLines

Vrati SAMO JSON niz. Primer:
[$dateExample$dateOnlyExample]
Mora biti tacno ${tasks.size} elemenata, istim redosledom.
        """.trimIndent()
    }

    private fun buildRequestBody(prompt: String, model: String): okhttp3.RequestBody {
        val request = PollinationsRequest(
            messages = listOf(
                Message(
                    role = "system",
                    content = "Vracas JSON nizove. Samo JSON, bez teksta."
                ),
                Message(role = "user", content = prompt)
            ),
            model = model,
            seed = (1..999999999).random(),
            private = true
        )

        val json = gson.toJson(request)
        return json.toRequestBody(jsonMediaType)
    }

    private fun parseAiResponse(responseText: String, originalTasks: List<TaskItem>): List<ScheduledTask> {
        try {
            val jsonText = extractJson(responseText)
            val jsonElement = JsonParser.parseString(jsonText)

            val taskList = when {
                jsonElement.isJsonArray -> parseJsonArray(jsonElement.asJsonArray)
                jsonElement.isJsonObject -> {
                    val obj = jsonElement.asJsonObject
                    val innerArray = obj.getAsJsonArray("scheduledTasks")
                        ?: obj.getAsJsonArray("tasks")
                        ?: obj.getAsJsonArray("schedule")
                    if (innerArray != null) parseJsonArray(innerArray)
                    else listOf(parseSingleTask(obj))
                }
                else -> throw Exception("Unexpected JSON element type")
            }

            return fillMissingTasks(taskList, originalTasks)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AI response: ${e.message}")
            Log.e(TAG, "Raw: $responseText")
            return originalTasks.map { task ->
                ScheduledTask(
                    name = task.name,
                    newStartDateTime = task.startDateTime,
                    newEndDateTime = task.endDateTime,
                    hasTime = task.hasTime
                )
            }
        }
    }

    private fun parseJsonArray(array: JsonArray): List<ScheduledTask> {
        return array.mapNotNull { element ->
            try {
                if (element.isJsonObject) parseSingleTask(element.asJsonObject) else null
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse task: ${e.message}")
                null
            }
        }
    }

    private fun parseSingleTask(obj: JsonObject): ScheduledTask {
        val name = obj.get("name")?.asString ?: ""
        val newStartDateTime = obj.get("newStartDateTime")?.asString ?: ""
        val newEndDateTimeRaw = if (obj.has("newEndDateTime") && !obj.get("newEndDateTime").isJsonNull) {
            obj.get("newEndDateTime").asString
        } else null
        val newEndDateTime = if (newEndDateTimeRaw.isNullOrBlank()) null else newEndDateTimeRaw
        val hasTime = if (obj.has("hasTime") && !obj.get("hasTime").isJsonNull) {
            obj.get("hasTime").asBoolean
        } else newStartDateTime.contains(":")
        return ScheduledTask(name, newStartDateTime, newEndDateTime, hasTime)
    }

    private fun fillMissingTasks(
        taskList: List<ScheduledTask>,
        originalTasks: List<TaskItem>
    ): List<ScheduledTask> {
        if (taskList.size >= originalTasks.size) return taskList

        val resultNames = taskList.map { it.name }.toSet()
        val missing = originalTasks
            .filter { it.name !in resultNames }
            .map { task ->
                ScheduledTask(
                    name = task.name,
                    newStartDateTime = task.startDateTime,
                    newEndDateTime = task.endDateTime,
                    hasTime = task.hasTime
                )
            }
        return taskList + missing
    }

    private fun extractJson(text: String): String {
        val cleaned = text.trim()
        if (cleaned.startsWith("[") || cleaned.startsWith("{")) {
            val startChar = cleaned[0]
            val endChar = if (startChar == '[') ']' else '}'
            val endIndex = cleaned.lastIndexOf(endChar)
            if (endIndex >= 0) return cleaned.substring(0, endIndex + 1)
        }
        val codeBlockRegex = Regex("```(?:json)?\\s*\\n?([\\[{].*?[}\\]])\\s*\\n?```", RegexOption.DOT_MATCHES_ALL)
        codeBlockRegex.find(cleaned)?.let { return it.groupValues[1] }
        val arrayRegex = Regex("\\[\\s*\\{.*}\\s*]", RegexOption.DOT_MATCHES_ALL)
        arrayRegex.find(cleaned)?.let { return it.value }
        val objectRegex = Regex("\\{.*}", RegexOption.DOT_MATCHES_ALL)
        objectRegex.find(cleaned)?.let { return it.value }
        return cleaned
    }
}
