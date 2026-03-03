package com.example.projekat.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.projekat.MainActivity
import com.example.projekat.ProjekatApplication
import com.example.projekat.R
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DeadlineWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val taskTitle = inputData.getString(KEY_TASK_TITLE) ?: "Task"
        val isReminder = inputData.getBoolean(KEY_IS_REMINDER, false)

        // Verify task still exists and is still in progress with a deadline
        val task = taskRepository.getTaskById(taskId)
        if (task == null || task.status == TaskStatus.COMPLETED || task.deadline == null) {
            return Result.success()
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return Result.success()
            }
        }

        // Create intent to open the app and navigate to the task
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("taskId", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title: String
        val text: String
        if (isReminder) {
            title = "Podsetnik"
            text = "Sutra ističe rok za \"$taskTitle\""
        } else {
            title = "Rok ističe danas"
            text = "Danas ističe rok za \"$taskTitle\""
        }

        // Use different notification IDs for reminder vs expiry so both can show
        val notificationId = if (isReminder) {
            taskId.hashCode() + 1
        } else {
            taskId.hashCode()
        }

        val notification = NotificationCompat.Builder(applicationContext, ProjekatApplication.DEADLINE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)

        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TASK_TITLE = "task_title"
        const val KEY_IS_REMINDER = "is_reminder"
    }
}
