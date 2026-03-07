package com.example.projekat.data.remote

import com.google.gson.annotations.SerializedName

/**
 * Request body for POST /api/schedule
 */
data class ScheduleRequestDto(
    val tasks: List<TaskItemDto>
)

data class TaskItemDto(
    val name: String,
    val priority: String, // HIGH, MEDIUM, LOW
    val deadline: String  // YYYY-MM-DD format
)

/**
 * Response body from POST /api/schedule
 */
data class ScheduleResponseDto(
    val scheduledTasks: List<ScheduledTaskDto>
)

data class ScheduledTaskDto(
    val name: String,
    val scheduledDate: String // YYYY-MM-DD format
)
