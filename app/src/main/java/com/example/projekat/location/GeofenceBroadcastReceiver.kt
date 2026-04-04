package com.example.projekat.location

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.projekat.MainActivity
import com.example.projekat.ProjekatApplication
import com.example.projekat.R
import com.example.projekat.data.local.AppDatabase
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles geofence transition events.
 * When user enters a geofenced location, it fires a notification.
 * Handles both standard GeofencingEvent and manual triggers from GeofenceManager.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        const val LOCATION_CHANNEL_ID = "location_notifications"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Geofence event received, action: ${intent.action}")
        
        if (intent.action != GeofenceManager.ACTION_GEOFENCE_EVENT) {
            Log.w(TAG, "Unknown action: ${intent.action}")
            return
        }

        // Check if this is a manual trigger from GeofenceManager
        val isManualTrigger = intent.getBooleanExtra("manual_trigger", false)
        if (isManualTrigger) {
            val taskId = intent.getStringExtra("task_id")
            if (taskId != null) {
                Log.d(TAG, "Manual geofence trigger for task: $taskId")
                sendLocationNotification(context, taskId)
            }
            return
        }

        // Handle standard GeofencingEvent
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        
        if (geofencingEvent == null) {
            Log.e(TAG, "GeofencingEvent is null")
            return
        }
        
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error: $errorMessage (code: ${geofencingEvent.errorCode})")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        Log.d(TAG, "Geofence transition type: $geofenceTransition")

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            
            if (triggeringGeofences.isNullOrEmpty()) {
                Log.w(TAG, "No triggering geofences found")
                return
            }

            Log.d(TAG, "Triggering geofences count: ${triggeringGeofences.size}")

            // Process each triggered geofence
            for (geofence in triggeringGeofences) {
                val taskId = geofence.requestId
                Log.d(TAG, "User entered/dwelling in geofence for task: $taskId")
                
                // Fetch task details from database and send notification
                sendLocationNotification(context, taskId)
            }
        } else {
            Log.d(TAG, "Unhandled geofence transition: $geofenceTransition")
        }
    }

    private fun sendLocationNotification(context: Context, taskId: String) {
        // Use coroutine to fetch task from database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val task = database.taskDao().getTaskById(taskId)
                
                if (task == null) {
                    Log.w(TAG, "Task not found for geofence: $taskId")
                    return@launch
                }

                // Only notify if task is still in progress
                if (task.status == com.example.projekat.data.model.TaskStatus.COMPLETED) {
                    Log.d(TAG, "Task is already completed, skipping notification")
                    return@launch
                }

                val locationName = task.locationName ?: "nepoznata lokacija"
                val notificationTitle = "Blizu ste lokacije za task"
                val notificationText = "${task.title} - $locationName"

                Log.d(TAG, "Sending notification for task: ${task.title} at $locationName")

                // Create intent to open task detail
                val notificationIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigate_to_task", taskId)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    taskId.hashCode(),
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, LOCATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(longArrayOf(0, 500, 200, 500))
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(taskId.hashCode(), notification)

                Log.d(TAG, "Location notification sent for task: ${task.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending location notification", e)
            }
        }
    }
}
