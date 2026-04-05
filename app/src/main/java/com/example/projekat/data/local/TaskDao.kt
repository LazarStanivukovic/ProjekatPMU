package com.example.projekat.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE syncStatus != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = :status AND syncStatus != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE deadline IS NOT NULL AND syncStatus != 'PENDING_DELETE' ORDER BY deadline ASC")
    fun getTasksWithDeadline(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE deadline BETWEEN :startOfDay AND :endOfDay AND syncStatus != 'PENDING_DELETE' ORDER BY deadline ASC")
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    // Sync-related queries
    @Query("SELECT * FROM tasks WHERE syncStatus = :status")
    suspend fun getTasksBySyncStatus(status: SyncStatus): List<Task>

    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    suspend fun getAllTasksList(): List<Task>
}
