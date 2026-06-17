package com.example.projekat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.projekat.data.repository.AuthRepository
import com.example.projekat.notification.SharedTaskListener
import com.example.projekat.worker.CleanupWorker
import com.example.projekat.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ProjekatApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var sharedTaskListener: SharedTaskListener

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleCleanupWorker()
        scheduleSyncWorker()
        observeAuthState()
    }

    private fun observeAuthState() {
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.observeAuthState().collect { user ->
                if (user != null) {
                    sharedTaskListener.startListening()
                } else {
                    sharedTaskListener.stopListening()
                }
            }
        }
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

            // Inbox notifications channel
            val inboxChannel = NotificationChannel(
                INBOX_CHANNEL_ID,
                "Inbox obaveštenja",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Obaveštenja kada dobijete novi zadatak u Inboxu"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(deadlineChannel)
            notificationManager.createNotificationChannel(locationChannel)
            notificationManager.createNotificationChannel(inboxChannel)
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

    private fun scheduleSyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    companion object {
        const val DEADLINE_CHANNEL_ID = "deadline_notifications"
        const val LOCATION_CHANNEL_ID = "location_notifications"
        const val INBOX_CHANNEL_ID = "inbox_notifications"
    }
}
