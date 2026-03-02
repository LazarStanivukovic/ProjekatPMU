package com.example.projekat.notification

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.projekat.worker.DeadlineWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeadlineScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        const val NOTIFICATION_HOUR = 9 // Fire notifications at 9:00 AM local time
    }

    /**
     * Convert a UTC midnight timestamp (from DatePicker) to local-time 9:00 AM on that date.
     *
     * DatePicker returns selectedDateMillis as start-of-day in UTC (e.g. 2026-03-02T00:00:00Z).
     * We want notifications at 9:00 AM local time on that date, not at midnight UTC
     * (which would be 1:00 AM CET / 2:00 AM CEST).
     */
    private fun toLocal9AM(utcMidnightMillis: Long): Long {
        // Parse the UTC midnight to extract year/month/day
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMidnightMillis
        }
        val year = utcCal.get(Calendar.YEAR)
        val month = utcCal.get(Calendar.MONTH)
        val day = utcCal.get(Calendar.DAY_OF_MONTH)

        // Build the same date at 9:00 AM in the device's local timezone
        val localCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, NOTIFICATION_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCal.timeInMillis
    }

    /**
     * Schedule notifications for a task deadline:
     * 1. A reminder notification at 9:00 AM local time the day before the deadline
     *    ("Sutra istice rok za ...")
     * 2. The main notification at 9:00 AM local time on the deadline day
     *    ("Danas istice rok za ...")
     *
     * If the reminder time has already passed, only the deadline-day notification is scheduled.
     * If both times have passed, the deadline notification fires immediately.
     */
    fun scheduleDeadlineNotification(taskId: String, taskTitle: String, deadlineMillis: Long) {
        val now = System.currentTimeMillis()
        val wm = WorkManager.getInstance(context)

        // Convert UTC midnight deadline to local 9:00 AM on deadline day
        val deadlineLocal9AM = toLocal9AM(deadlineMillis)

        // 1. Schedule reminder (9:00 AM local time, day before deadline)
        val reminderTime = deadlineLocal9AM - ONE_DAY_MS
        if (reminderTime > now) {
            val reminderDelay = reminderTime - now
            val reminderData = Data.Builder()
                .putString(DeadlineWorker.KEY_TASK_ID, taskId)
                .putString(DeadlineWorker.KEY_TASK_TITLE, taskTitle)
                .putBoolean(DeadlineWorker.KEY_IS_REMINDER, true)
                .build()

            val reminderRequest = OneTimeWorkRequestBuilder<DeadlineWorker>()
                .setInitialDelay(reminderDelay, TimeUnit.MILLISECONDS)
                .setInputData(reminderData)
                .build()

            wm.enqueueUniqueWork(
                getReminderWorkName(taskId),
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
        } else {
            // Reminder time has passed — cancel any stale reminder
            wm.cancelUniqueWork(getReminderWorkName(taskId))
        }

        // 2. Schedule deadline-day notification (9:00 AM local time on deadline day)
        val expiryDelay = deadlineLocal9AM - now
        val actualDelay = if (expiryDelay > 0) expiryDelay else 0L

        val expiryData = Data.Builder()
            .putString(DeadlineWorker.KEY_TASK_ID, taskId)
            .putString(DeadlineWorker.KEY_TASK_TITLE, taskTitle)
            .putBoolean(DeadlineWorker.KEY_IS_REMINDER, false)
            .build()

        val expiryRequest = OneTimeWorkRequestBuilder<DeadlineWorker>()
            .setInitialDelay(actualDelay, TimeUnit.MILLISECONDS)
            .setInputData(expiryData)
            .build()

        wm.enqueueUniqueWork(
            getExpiryWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            expiryRequest
        )
    }

    /**
     * Cancel all scheduled deadline notifications (reminder + expiry) for a task.
     * Called when: deadline is removed, task is completed, or task is deleted.
     */
    fun cancelDeadlineNotification(taskId: String) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(getReminderWorkName(taskId))
        wm.cancelUniqueWork(getExpiryWorkName(taskId))
    }

    private fun getReminderWorkName(taskId: String): String = "deadline_reminder_$taskId"
    private fun getExpiryWorkName(taskId: String): String = "deadline_expiry_$taskId"
}
