package com.example.projekat.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service layer for AI scheduling operations.
 * Provides high-level business logic on top of PollinationsAiClient.
 */
@Singleton
class AiScheduleService @Inject constructor(
    private val aiClient: PollinationsAiClient
) {
    companion object {
        private const val TAG = "AiScheduleService"
    }

    /**
     * Request an AI-generated schedule for tasks.
     * 
     * @param tasks List of tasks to schedule
     * @return Result containing scheduled tasks or error
     */
    suspend fun requestSchedule(tasks: List<TaskItem>): Result<List<ScheduledTask>> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate input
                if (tasks.isEmpty()) {
                    return@withContext Result.failure(Exception("Lista taskova je prazna"))
                }

                Log.d(TAG, "Requesting schedule for ${tasks.size} tasks")
                tasks.forEach { task ->
                    Log.d(TAG, "  - ${task.name} (${task.priority}, deadline: ${task.deadline})")
                }

                // Call AI client
                val scheduledTasks = aiClient.generateSchedule(tasks)

                Log.d(TAG, "Received ${scheduledTasks.size} scheduled tasks")
                scheduledTasks.forEach { task ->
                    Log.d(TAG, "  - ${task.name} → ${task.scheduledDate}")
                }

                Result.success(scheduledTasks)

            } catch (e: Exception) {
                Log.e(TAG, "Error requesting schedule: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Check if the AI service is available.
     * For direct API calls, we can't easily check without making a request.
     * This is a placeholder that always returns true.
     */
    suspend fun isServiceAvailable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // For now, assume service is available if we have internet
                // Could add a lightweight ping request if needed
                true
            } catch (e: Exception) {
                Log.e(TAG, "Service availability check failed: ${e.message}")
                false
            }
        }
    }
}
