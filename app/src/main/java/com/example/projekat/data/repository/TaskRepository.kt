package com.example.projekat.data.repository

import com.example.projekat.data.local.TaskDao
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

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(
        task.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTaskById(taskId: String) = taskDao.deleteTaskById(taskId)

    suspend fun toggleTaskStatus(task: Task) {
        val newStatus = when (task.status) {
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
        }
        taskDao.updateTask(
            task.copy(
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
