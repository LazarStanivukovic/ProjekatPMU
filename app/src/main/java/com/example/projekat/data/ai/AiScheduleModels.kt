package com.example.projekat.data.ai

/**
 * Request models for AI scheduling
 */
data class AiScheduleRequest(
    val tasks: List<TaskItem>
)

data class TaskItem(
    val name: String,
    val priority: String, // HIGH, MEDIUM, LOW
    val deadline: String  // YYYY-MM-DD format
)

/**
 * Response models from AI scheduling
 */
data class AiScheduleResponse(
    val scheduledTasks: List<ScheduledTask>
)

data class ScheduledTask(
    val name: String,
    val scheduledDate: String // YYYY-MM-DD format
)

/**
 * Internal models for Pollinations.ai API
 */
internal data class PollinationsRequest(
    val messages: List<Message>,
    val model: String = "openai",
    val seed: Int,
    val private: Boolean = true
)

internal data class Message(
    val role: String, // "system" or "user"
    val content: String
)
