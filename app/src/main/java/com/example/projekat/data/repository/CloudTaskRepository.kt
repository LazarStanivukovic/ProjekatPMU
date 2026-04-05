package com.example.projekat.data.repository

import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskPriority
import com.example.projekat.data.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for syncing tasks with Firebase Firestore.
 */
@Singleton
class CloudTaskRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private fun tasksCollection(uid: String) = firestore.collection("users").document(uid).collection("tasks")

    /**
     * Upload a task to Firestore.
     */
    suspend fun uploadTask(task: Task): Result<String> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            val taskData = hashMapOf(
                "id" to task.id,
                "title" to task.title,
                "description" to task.description,
                "status" to task.status.name,
                "priority" to task.priority.name,
                "deadline" to task.deadline,
                "noteId" to task.noteId,
                "colorIndex" to task.colorIndex,
                "checklistItems" to task.checklistItems.map { item ->
                    mapOf(
                        "id" to item.id,
                        "text" to item.text,
                        "isChecked" to item.isChecked,
                        "order" to item.order
                    )
                },
                "locationLat" to task.locationLat,
                "locationLng" to task.locationLng,
                "locationName" to task.locationName,
                "locationRadius" to task.locationRadius,
                "createdAt" to task.createdAt,
                "updatedAt" to task.updatedAt
            )

            // Use local ID as document ID for easy lookup
            tasksCollection(uid).document(task.id).set(taskData).await()

            Result.success(task.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch a task from Firestore by ID.
     */
    suspend fun fetchTask(taskId: String): Result<Task?> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            val doc = tasksCollection(uid).document(taskId).get().await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                val task = documentToTask(doc.data!!, doc.id)
                Result.success(task)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch all tasks from Firestore.
     */
    suspend fun fetchAllTasks(): Result<List<Task>> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            val snapshot = tasksCollection(uid).get().await()
            val tasks = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { documentToTask(it, doc.id) }
            }
            Result.success(tasks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a task from Firestore.
     */
    suspend fun deleteTask(taskId: String): Result<Unit> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            tasksCollection(uid).document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert Firestore document data to Task entity.
     */
    @Suppress("UNCHECKED_CAST")
    private fun documentToTask(data: Map<String, Any>, docId: String): Task {
        val checklistMaps = (data["checklistItems"] as? List<Map<String, Any>>) ?: emptyList()
        val checklistItems = checklistMaps.map { map ->
            ChecklistItem(
                id = (map["id"] as? String) ?: "",
                text = (map["text"] as? String) ?: "",
                isChecked = (map["isChecked"] as? Boolean) ?: false,
                order = ((map["order"] as? Number)?.toInt()) ?: 0
            )
        }

        val statusStr = (data["status"] as? String) ?: "IN_PROGRESS"
        val priorityStr = (data["priority"] as? String) ?: "MEDIUM"

        return Task(
            id = (data["id"] as? String) ?: docId,
            title = (data["title"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            status = try { TaskStatus.valueOf(statusStr) } catch (e: Exception) { TaskStatus.IN_PROGRESS },
            priority = try { TaskPriority.valueOf(priorityStr) } catch (e: Exception) { TaskPriority.MEDIUM },
            deadline = (data["deadline"] as? Number)?.toLong(),
            noteId = data["noteId"] as? String,
            colorIndex = ((data["colorIndex"] as? Number)?.toInt()) ?: 0,
            checklistItems = checklistItems,
            locationLat = (data["locationLat"] as? Number)?.toDouble(),
            locationLng = (data["locationLng"] as? Number)?.toDouble(),
            locationName = data["locationName"] as? String,
            locationRadius = ((data["locationRadius"] as? Number)?.toInt()) ?: 100,
            createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
            updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED,
            cloudId = docId
        )
    }
}
