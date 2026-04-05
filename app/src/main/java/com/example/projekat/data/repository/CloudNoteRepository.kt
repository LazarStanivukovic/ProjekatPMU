package com.example.projekat.data.repository

import android.net.Uri
import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for syncing notes with Firebase Firestore and Storage.
 */
@Singleton
class CloudNoteRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val userId: String?
        get() = auth.currentUser?.uid

    private fun notesCollection(uid: String) = firestore.collection("users").document(uid).collection("notes")

    /**
     * Upload a note to Firestore.
     * Images are uploaded to Firebase Storage and URLs are stored in Firestore.
     */
    suspend fun uploadNote(note: Note): Result<String> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            // Upload images to Firebase Storage and get download URLs
            val imageUrls = uploadImages(uid, note.id, note.imageUris)

            // Create Firestore document data
            val noteData = hashMapOf(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "imageUrls" to imageUrls,
                "checklistItems" to note.checklistItems.map { item ->
                    mapOf(
                        "id" to item.id,
                        "text" to item.text,
                        "isChecked" to item.isChecked,
                        "order" to item.order
                    )
                },
                "colorIndex" to note.colorIndex,
                "isBookmarked" to note.isBookmarked,
                "isDeleted" to note.isDeleted,
                "deletedAt" to note.deletedAt,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt
            )

            // Use local ID as document ID for easy lookup
            notesCollection(uid).document(note.id).set(noteData).await()

            Result.success(note.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload images to Firebase Storage and return download URLs.
     */
    private suspend fun uploadImages(userId: String, noteId: String, localUris: List<String>): List<String> {
        if (localUris.isEmpty()) return emptyList()

        val downloadUrls = mutableListOf<String>()
        val storageRef = storage.reference.child("users/$userId/notes/$noteId/images")

        for (localUri in localUris) {
            try {
                val uri = Uri.parse(localUri)
                // Skip if already a Firebase Storage URL
                if (localUri.startsWith("https://firebasestorage.googleapis.com")) {
                    downloadUrls.add(localUri)
                    continue
                }

                val fileName = "${UUID.randomUUID()}.jpg"
                val imageRef = storageRef.child(fileName)

                // Upload from local file
                val file = File(uri.path ?: continue)
                if (file.exists()) {
                    imageRef.putFile(Uri.fromFile(file)).await()
                    val downloadUrl = imageRef.downloadUrl.await().toString()
                    downloadUrls.add(downloadUrl)
                }
            } catch (e: Exception) {
                // Skip failed uploads
                continue
            }
        }

        return downloadUrls
    }

    /**
     * Fetch a note from Firestore by ID.
     */
    suspend fun fetchNote(noteId: String): Result<Note?> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            val doc = notesCollection(uid).document(noteId).get().await()
            if (!doc.exists()) {
                Result.success(null)
            } else {
                val note = documentToNote(doc.data!!, doc.id)
                Result.success(note)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetch all notes from Firestore.
     */
    suspend fun fetchAllNotes(): Result<List<Note>> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            val snapshot = notesCollection(uid).get().await()
            val notes = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { documentToNote(it, doc.id) }
            }
            Result.success(notes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a note from Firestore.
     */
    suspend fun deleteNote(noteId: String): Result<Unit> {
        val uid = userId ?: return Result.failure(Exception("Korisnik nije ulogovan"))

        return try {
            // Delete images from storage
            try {
                val storageRef = storage.reference.child("users/$uid/notes/$noteId/images")
                val items = storageRef.listAll().await()
                items.items.forEach { it.delete().await() }
            } catch (e: Exception) {
                // Ignore storage errors
            }

            // Delete Firestore document
            notesCollection(uid).document(noteId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert Firestore document data to Note entity.
     */
    @Suppress("UNCHECKED_CAST")
    private fun documentToNote(data: Map<String, Any>, docId: String): Note {
        val checklistMaps = (data["checklistItems"] as? List<Map<String, Any>>) ?: emptyList()
        val checklistItems = checklistMaps.map { map ->
            ChecklistItem(
                id = (map["id"] as? String) ?: "",
                text = (map["text"] as? String) ?: "",
                isChecked = (map["isChecked"] as? Boolean) ?: false,
                order = ((map["order"] as? Number)?.toInt()) ?: 0
            )
        }

        return Note(
            id = (data["id"] as? String) ?: docId,
            title = (data["title"] as? String) ?: "",
            content = (data["content"] as? String) ?: "",
            imageUris = (data["imageUrls"] as? List<String>) ?: emptyList(),
            checklistItems = checklistItems,
            colorIndex = ((data["colorIndex"] as? Number)?.toInt()) ?: 0,
            isBookmarked = (data["isBookmarked"] as? Boolean) ?: false,
            isDeleted = (data["isDeleted"] as? Boolean) ?: false,
            deletedAt = (data["deletedAt"] as? Number)?.toLong(),
            createdAt = ((data["createdAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
            updatedAt = ((data["updatedAt"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED,
            cloudId = docId
        )
    }
}
