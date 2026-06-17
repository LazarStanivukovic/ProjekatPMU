package com.example.projekat.data.repository

import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.RepeatInterval
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.Task
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
    private val rootTasksCollection = firestore.collection("shared_tasks")

    /**
     * Get the collection where this task actually lives (using ownerId if available, fallback to current user).
     */
    private fun collectionForTask(taskOwnerId: String?): com.google.firebase.firestore.CollectionReference {
        // Use a root collection for all tasks now to avoid collection group index requirements
        return rootTasksCollection
    }

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
                "priorityScore" to task.priorityScore,
                "startDate" to task.startDate,
                "endDate" to task.endDate,
                "hasTime" to task.hasTime,
                "repeatInterval" to task.repeatInterval.name,
                "repeatEndDate" to task.repeatEndDate,
                "lastCompletedAt" to task.lastCompletedAt,
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
                "updatedAt" to task.updatedAt,
                "ownerId" to task.ownerId,
                "ownerEmail" to task.ownerEmail,
                "sharedWith" to task.sharedWith,
                "pendingInvites" to task.pendingInvites
            )

            // Use local ID as document ID for easy lookup
            collectionForTask(task.ownerId).document(task.id).set(taskData).await()

            // Try to clean up legacy document if it exists, to avoid duplicates on fetch
            try {
                tasksCollection(uid).document(task.id).delete().await()
            } catch (e: Exception) {
                // Ignore
            }

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
     * Fetch a single shared task from Firestore by ID (from shared_tasks collection).
     */
    suspend fun fetchSharedTask(taskId: String): Result<Task?> {
        return try {
            val doc = rootTasksCollection.document(taskId).get().await()
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
     * Fetch all tasks from Firestore (owned by user OR shared with user OR pending invite).
     */
    suspend fun fetchAllTasks(): Result<List<Task>> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))
        val email = auth.currentUser?.email

        return try {
            val tasks = mutableSetOf<Task>()
            
            // 1. Fetch owned tasks from new root collection
            val ownedSnapshot = rootTasksCollection
                .whereEqualTo("ownerId", uid)
                .get().await()
            tasks.addAll(ownedSnapshot.documents.mapNotNull { it.data?.let { data -> documentToTask(data, it.id) } })
            
            // 2. Fetch shared tasks (if email is available)
            if (email != null) {
                val sharedSnapshot = rootTasksCollection
                    .whereArrayContains("sharedWith", email)
                    .get().await()
                tasks.addAll(sharedSnapshot.documents.mapNotNull { it.data?.let { data -> documentToTask(data, it.id) } })
                
                // 3. Fetch pending invites
                val pendingSnapshot = rootTasksCollection
                    .whereArrayContains("pendingInvites", email)
                    .get().await()
                tasks.addAll(pendingSnapshot.documents.mapNotNull { it.data?.let { data -> documentToTask(data, it.id) } })
            }

            // Fallback for legacy tasks (before sharing was implemented, tasks were just stored under user's uid)
            val legacySnapshot = tasksCollection(uid).get().await()
            tasks.addAll(legacySnapshot.documents.mapNotNull { it.data?.let { data -> documentToTask(data, it.id) } })

            Result.success(tasks.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a task from Firestore.
     */
    suspend fun deleteTask(taskId: String, ownerId: String? = null): Result<Unit> {
        return try {
            val uid = userId
            collectionForTask(ownerId).document(taskId).delete().await()
            if (uid != null) {
                // Also attempt to delete from legacy collection to avoid ghost tasks
                tasksCollection(uid).document(taskId).delete().await()
            }
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
        val repeatIntervalStr = (data["repeatInterval"] as? String) ?: "NONE"

        // Handle backward compatibility for priority -> priorityScore
        val priorityScore = if (data.containsKey("priorityScore")) {
            ((data["priorityScore"] as? Number)?.toInt()) ?: 5
        } else {
            val oldPriority = (data["priority"] as? String) ?: "MEDIUM"
            when (oldPriority) {
                "HIGH" -> 8
                "MEDIUM" -> 5
                "LOW" -> 3
                else -> 5
            }
        }

        return Task(
            id = (data["id"] as? String) ?: docId,
            title = (data["title"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            status = try { TaskStatus.valueOf(statusStr) } catch (e: Exception) { TaskStatus.IN_PROGRESS },
            priorityScore = priorityScore,
            startDate = (data["startDate"] as? Number)?.toLong()
                ?: (data["deadline"] as? Number)?.toLong(),
            endDate = (data["endDate"] as? Number)?.toLong(),
            hasTime = (data["hasTime"] as? Boolean) ?: false,
            repeatInterval = try { RepeatInterval.valueOf(repeatIntervalStr) } catch (e: Exception) { RepeatInterval.NONE },
            repeatEndDate = (data["repeatEndDate"] as? Number)?.toLong(),
            lastCompletedAt = (data["lastCompletedAt"] as? Number)?.toLong(),
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
            cloudId = docId,
            ownerId = data["ownerId"] as? String,
            ownerEmail = data["ownerEmail"] as? String,
            sharedWith = (data["sharedWith"] as? List<String>) ?: emptyList(),
            pendingInvites = (data["pendingInvites"] as? List<String>) ?: emptyList()
        )
    }
}
