package com.example.projekat.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.projekat.MainActivity
import com.example.projekat.ProjekatApplication
import com.example.projekat.R
import com.example.projekat.data.local.TaskDao
import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.RepeatInterval
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedTaskListener @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val taskDao: TaskDao,
    @ApplicationContext private val context: Context
) {
    private var registration: ListenerRegistration? = null

    fun startListening() {
        val email = auth.currentUser?.email ?: return
        stopListening()

        Log.i("SharedTaskListener", "Starting listener for $email")

        registration = firestore.collection("shared_tasks")
            .whereArrayContains("pendingInvites", email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SharedTaskListener", "Listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                        val task = documentToTask(change.document.data, change.document.id)
                        CoroutineScope(Dispatchers.IO).launch {
                            taskDao.insertTask(task)
                            Log.i("SharedTaskListener", "Inserted shared task: ${task.title}")
                            showNotification(task)
                        }
                    }
                }
            }
    }

    fun stopListening() {
        registration?.remove()
        registration = null
        Log.i("SharedTaskListener", "Listener stopped")
    }

    @Suppress("UNCHECKED_CAST")
    private fun documentToTask(data: Map<String, Any>, docId: String): Task {
        val checklistMaps = (data["checklistItems"] as? List<Map<String, Any>>) ?: emptyList()
        val checklistItems = checklistMaps.mapIndexed { index, map ->
            ChecklistItem(
                id = (map["id"] as? String) ?: "",
                text = (map["text"] as? String) ?: "",
                isChecked = (map["isChecked"] as? Boolean) ?: false,
                order = ((map["order"] as? Number)?.toInt()) ?: index
            )
        }

        val statusStr = (data["status"] as? String) ?: "IN_PROGRESS"
        val repeatIntervalStr = (data["repeatInterval"] as? String) ?: "NONE"

        return Task(
            id = (data["id"] as? String) ?: docId,
            title = (data["title"] as? String) ?: "",
            description = (data["description"] as? String) ?: "",
            status = try { TaskStatus.valueOf(statusStr) } catch (_: Exception) { TaskStatus.IN_PROGRESS },
            priorityScore = ((data["priorityScore"] as? Number)?.toInt()) ?: 5,
            startDate = (data["startDate"] as? Number)?.toLong(),
            endDate = (data["endDate"] as? Number)?.toLong(),
            hasTime = (data["hasTime"] as? Boolean) ?: false,
            repeatInterval = try { RepeatInterval.valueOf(repeatIntervalStr) } catch (_: Exception) { RepeatInterval.NONE },
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

    private fun showNotification(task: Task) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("openInbox", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val owner = task.ownerEmail ?: "Neko"
        val notification = NotificationCompat.Builder(context, ProjekatApplication.INBOX_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Novi zadatak u Inboxu")
            .setContentText("$owner vam je poslao: ${task.title}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
