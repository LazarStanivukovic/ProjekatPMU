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

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: Long? = null,
    val noteId: String? = null,
    val colorIndex: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
