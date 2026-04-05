package com.example.projekat.data.repository

import com.example.projekat.data.local.TaskDao
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> = taskDao.getTasksByStatus(status)

    fun getTasksWithDeadline(): Flow<List<Task>> = taskDao.getTasksWithDeadline()

    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<Task>> =
        taskDao.getTasksForDay(startOfDay, endOfDay)

    suspend fun getTaskById(taskId: String): Task? = taskDao.getTaskById(taskId)

    suspend fun insertTask(task: Task) {
        // New tasks start as LOCAL_ONLY, will be synced later
        taskDao.insertTask(task.copy(syncStatus = SyncStatus.LOCAL_ONLY))
    }

    suspend fun updateTask(task: Task) {
        // Mark as PENDING_UPLOAD when modified (unless it's still LOCAL_ONLY)
        val newSyncStatus = if (task.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        taskDao.updateTask(
            task.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = newSyncStatus
            )
        )
    }

    suspend fun deleteTask(task: Task) {
        // If synced, mark for cloud deletion; otherwise just delete locally
        if (task.syncStatus == SyncStatus.LOCAL_ONLY) {
            taskDao.deleteTask(task)
        } else {
            taskDao.updateTask(task.copy(syncStatus = SyncStatus.PENDING_DELETE))
        }
    }

    suspend fun deleteTaskById(taskId: String) {
        val task = taskDao.getTaskById(taskId)
        if (task != null) {
            deleteTask(task)
        }
    }

    suspend fun toggleTaskStatus(task: Task) {
        val newStatus = when (task.status) {
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
        }
        val newSyncStatus = if (task.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        taskDao.updateTask(
            task.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis(),
                syncStatus = newSyncStatus
            )
        )
    }
}
