package com.example.projekat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskStatus {
    IN_PROGRESS,
    COMPLETED
}

enum class TaskPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class RepeatInterval {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    // Date range / time fields
    val startDate: Long? = null,
    val endDate: Long? = null,
    val hasTime: Boolean = false,
    // Repeating task fields
    val repeatInterval: RepeatInterval = RepeatInterval.NONE,
    val repeatEndDate: Long? = null,
    val lastCompletedAt: Long? = null,
    val noteId: String? = null,
    val colorIndex: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    // Location-based notification fields
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationName: String? = null,
    val locationRadius: Int = 100,  // meters
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Sync fields for Firebase Cloud Sync
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val cloudId: String? = null
)
