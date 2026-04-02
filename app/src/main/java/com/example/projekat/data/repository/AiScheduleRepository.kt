package com.example.projekat.data.repository

import com.example.projekat.data.ai.AiScheduleService
import com.example.projekat.data.ai.TaskItem
import com.example.projekat.data.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiScheduleRepository @Inject constructor(
    private val aiScheduleService: AiScheduleService
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Sends tasks to the AI service and returns a list of (taskId -> scheduledDate) pairs.
     * Only tasks with a deadline and IN_PROGRESS status are sent.
     */
    suspend fun requestSchedule(tasks: List<Task>): Result<List<ScheduleResult>> {
        return try {
            // Convert Task entities to TaskItem DTOs
            val taskItems = tasks.map { task ->
                TaskItem(
                    name = task.title,
                    priority = task.priority.name,
                    deadline = formatDeadline(task.deadline)
                )
            }

            // Call AI service (no longer goes through Ktor server)
            val result = aiScheduleService.requestSchedule(taskItems)
            
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val scheduledTasks = result.getOrNull() ?: emptyList()

            // Match response items back to original tasks by name
            val results = tasks.map { task ->
                val scheduled = scheduledTasks.find {
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
     * Check if the AI service is available.
     */
    suspend fun isServerAvailable(): Boolean {
        return try {
            aiScheduleService.isServiceAvailable()
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
