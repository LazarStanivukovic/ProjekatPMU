package com.example.projekat.data.repository

import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskPriority
import com.example.projekat.data.remote.ScheduleApi
import com.example.projekat.data.remote.ScheduleRequestDto
import com.example.projekat.data.remote.ScheduledTaskDto
import com.example.projekat.data.remote.TaskItemDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiScheduleRepository @Inject constructor(
    private val scheduleApi: ScheduleApi
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Sends tasks to the AI backend and returns a list of (taskId -> scheduledDate) pairs.
     * Only tasks with a deadline and IN_PROGRESS status are sent.
     */
    suspend fun requestSchedule(tasks: List<Task>): Result<List<ScheduleResult>> {
        return try {
            val taskItems = tasks.map { task ->
                TaskItemDto(
                    name = task.title,
                    priority = task.priority.name,
                    deadline = formatDeadline(task.deadline)
                )
            }

            val request = ScheduleRequestDto(tasks = taskItems)
            val response = scheduleApi.getSchedule(request)

            // Match response items back to original tasks by name
            val results = tasks.map { task ->
                val scheduled = response.scheduledTasks.find {
                    it.name.equals(task.title, ignoreCase = true)
                }
                ScheduleResult(
                    taskId = task.id,
                    taskName = task.title,
                    scheduledDate = scheduled?.scheduledDate ?: formatDeadline(task.deadline),
                    originalDeadline = formatDeadline(task.deadline)
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if the backend server is reachable.
     */
    suspend fun isServerAvailable(): Boolean {
        return try {
            val response = scheduleApi.healthCheck()
            response["status"] == "ok"
        } catch (e: Exception) {
            false
        }
    }

    private fun formatDeadline(deadlineMillis: Long?): String {
        if (deadlineMillis == null) return dateFormat.format(Date())
        return dateFormat.format(Date(deadlineMillis))
    }
}

/**
 * Represents a single AI-scheduled task result.
 */
data class ScheduleResult(
    val taskId: String,
    val taskName: String,
    val scheduledDate: String, // YYYY-MM-DD
    val originalDeadline: String // YYYY-MM-DD
)
