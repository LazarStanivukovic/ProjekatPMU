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
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    /**
     * Sends tasks to the AI service and returns a list of (taskId -> scheduledDate) pairs.
     * Only tasks with a start date and IN_PROGRESS status are sent.
     */
    suspend fun requestSchedule(tasks: List<Task>, customPrompt: String? = null): Result<List<ScheduleResult>> {
        return try {
            // Convert Task entities to TaskItem DTOs
            val taskItems = tasks.map { task ->
                TaskItem(
                    name = task.title,
                    priority = task.priority.name,
                    startDateTime = formatDateTime(task.startDate, task.hasTime),
                    endDateTime = task.endDate?.let { formatDateTime(it, task.hasTime) }
                        ?: formatDateTime(task.startDate, task.hasTime),
                    hasTime = task.hasTime
                )
            }

            // Call AI service (no longer goes through Ktor server)
            val result = aiScheduleService.requestSchedule(taskItems, customPrompt)
            
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }

            val scheduledTasks = result.getOrNull() ?: emptyList()

            // Match response items back to original tasks by name
            val results = tasks.map { task ->
                val scheduled = scheduledTasks.find {
                    it.name.equals(task.title, ignoreCase = true)
                }
                
                val origStart = formatDateTime(task.startDate, task.hasTime)
                val origEnd = task.endDate?.let { formatDateTime(it, task.hasTime) }

                ScheduleResult(
                    taskId = task.id,
                    taskName = task.title,
                    scheduledStartDateTime = scheduled?.newStartDateTime ?: origStart,
                    scheduledEndDateTime = scheduled?.newEndDateTime ?: origEnd,
                    originalStartDateTime = origStart,
                    originalEndDateTime = origEnd,
                    hasTime = scheduled?.hasTime ?: task.hasTime
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

    private fun formatDateTime(millis: Long?, hasTime: Boolean): String {
        if (millis == null) return dateFormat.format(Date())
        return if (hasTime) dateTimeFormat.format(Date(millis)) else dateFormat.format(Date(millis))
    }
}

/**
 * Represents a single AI-scheduled task result.
 */
data class ScheduleResult(
    val taskId: String,
    val taskName: String,
    val scheduledStartDateTime: String, 
    val scheduledEndDateTime: String?,  
    val originalStartDateTime: String,  
    val originalEndDateTime: String?,   
    val hasTime: Boolean
)
