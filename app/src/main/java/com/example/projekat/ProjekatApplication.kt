package com.example.projekat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.projekat.worker.CleanupWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ProjekatApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleCleanupWorker()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Deadline notifications channel
            val deadlineChannel = NotificationChannel(
                DEADLINE_CHANNEL_ID,
                "Rokovi za taskove",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obavestenja kada rok za task istekne"
            }

            // Location-based notifications channel
            val locationChannel = NotificationChannel(
                LOCATION_CHANNEL_ID,
                "Lokacijska obavestenja",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obavestenja kada se priblizite lokaciji za task"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(deadlineChannel)
            notificationManager.createNotificationChannel(locationChannel)
        }
    }

    private fun scheduleCleanupWorker() {
        val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(
            repeatInterval = 24,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    companion object {
        const val DEADLINE_CHANNEL_ID = "deadline_notifications"
        const val LOCATION_CHANNEL_ID = "location_notifications"
    }
}
