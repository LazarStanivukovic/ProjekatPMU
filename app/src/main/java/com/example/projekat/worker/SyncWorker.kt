package com.example.projekat.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.projekat.data.repository.AuthRepository
import com.example.projekat.data.sync.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that syncs notes and tasks with Firebase.
 * Only runs when user is authenticated.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Only sync if user is authenticated
        if (!authRepository.isAuthenticated) {
            return Result.success()
        }

        return try {
            val result = syncManager.syncAll()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            // Retry on failure (up to WorkManager's retry limit)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sync_worker"
    }
}
