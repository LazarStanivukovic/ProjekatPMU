package com.example.projekat.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiScheduleService @Inject constructor(
    private val aiClient: PollinationsAiClient
) {
    companion object {
        private const val TAG = "AiScheduleService"
    }

    suspend fun requestSchedule(tasks: List<TaskItem>, customPrompt: String? = null): Result<List<ScheduledTask>> {
        return withContext(Dispatchers.IO) {
            try {
                if (tasks.isEmpty()) {
                    return@withContext Result.failure(Exception("Lista taskova je prazna"))
                }

                Log.d(TAG, "Requesting schedule for ${tasks.size} tasks")

                val scheduledTasks = try {
                    aiClient.generateSchedule(tasks, customPrompt)
                } catch (e: Exception) {
                    Log.w(TAG, "AI failed (${e.message}), using local scheduler")
                    localSchedule(tasks)
                }

                Log.d(TAG, "Returning ${scheduledTasks.size} scheduled tasks")
                Result.success(scheduledTasks)
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun isServiceAvailable(): Boolean {
        return withContext(Dispatchers.IO) { true }
    }

    private fun localSchedule(tasks: List<TaskItem>): List<ScheduledTask> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        val sorted = tasks.sortedByDescending { it.priority.toIntOrNull() ?: 5 }

        return sorted.mapIndexed { index, task ->
            val cal = tomorrow.clone() as Calendar
            cal.add(Calendar.DAY_OF_MONTH, index)

            val newStart: String
            val newEnd: String?
            if (task.hasTime && task.startDateTime.contains(":")) {
                val parts = task.startDateTime.split(" ")
                val oldTime = if (parts.size > 1) parts[1] else "09:00"
                val endTime = task.endDateTime?.split(" ")?.getOrNull(1)

                val startStr = dateFormat.format(cal.time)
                newStart = "$startStr $oldTime"
                newEnd = if (endTime != null) "$startStr $endTime" else null
            } else {
                newStart = dateFormat.format(cal.time)
                newEnd = null
            }

            ScheduledTask(
                name = task.name,
                newStartDateTime = newStart,
                newEndDateTime = newEnd,
                hasTime = task.hasTime
            )
        }
    }
}
