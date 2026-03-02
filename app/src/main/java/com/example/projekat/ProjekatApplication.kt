package com.example.projekat

import android.app.Application
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
        scheduleCleanupWorker()
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
}
