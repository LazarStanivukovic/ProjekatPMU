package com.example.projekat.data.sync

import com.example.projekat.data.local.NoteDao
import com.example.projekat.data.local.TaskDao
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.SyncStatus
import com.example.projekat.data.model.Task
import com.example.projekat.data.repository.CloudNoteRepository
import com.example.projekat.data.repository.CloudTaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current sync state.
 */
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Orchestrates synchronization between local Room database and Firebase Firestore.
 * Implements offline-first architecture with last-write-wins conflict resolution.
 */
@Singleton
class SyncManager @Inject constructor(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val cloudNoteRepository: CloudNoteRepository,
    private val cloudTaskRepository: CloudTaskRepository
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    /**
     * Perform full sync: upload pending changes, download cloud changes, resolve conflicts.
     */
    suspend fun syncAll(): Result<Unit> {
        _syncState.value = SyncState.Syncing

        return try {
            // 1. Upload local changes (PENDING_UPLOAD items)
            uploadPendingNotes()
            uploadPendingTasks()

            // 2. Delete cloud items marked for deletion (PENDING_DELETE items)
            deletePendingNotes()
            deletePendingTasks()

            // 3. Download and merge cloud changes
            downloadAndMergeNotes()
            downloadAndMergeTasks()

            _lastSyncTime.value = System.currentTimeMillis()
            _syncState.value = SyncState.Success("Sinhronizacija uspesna")
            Result.success(Unit)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Greska pri sinhronizaciji")
            Result.failure(e)
        }
    }

    /**
     * Upload notes with PENDING_UPLOAD status.
     */
    private suspend fun uploadPendingNotes() {
        val pendingNotes = noteDao.getNotesBySyncStatus(SyncStatus.PENDING_UPLOAD)
        for (note in pendingNotes) {
            val result = cloudNoteRepository.uploadNote(note)
            if (result.isSuccess) {
                // Mark as synced
                noteDao.updateNote(note.copy(
                    syncStatus = SyncStatus.SYNCED,
                    cloudId = result.getOrNull() ?: note.id
                ))
            }
        }

        // Also upload LOCAL_ONLY notes (first-time sync)
        val localOnlyNotes = noteDao.getNotesBySyncStatus(SyncStatus.LOCAL_ONLY)
        for (note in localOnlyNotes) {
            val result = cloudNoteRepository.uploadNote(note)
            if (result.isSuccess) {
                noteDao.updateNote(note.copy(
                    syncStatus = SyncStatus.SYNCED,
                    cloudId = result.getOrNull() ?: note.id
                ))
            }
        }
    }

    /**
     * Upload tasks with PENDING_UPLOAD status.
     */
    private suspend fun uploadPendingTasks() {
        val pendingTasks = taskDao.getTasksBySyncStatus(SyncStatus.PENDING_UPLOAD)
        for (task in pendingTasks) {
            val result = cloudTaskRepository.uploadTask(task)
            if (result.isSuccess) {
                taskDao.updateTask(task.copy(
                    syncStatus = SyncStatus.SYNCED,
                    cloudId = result.getOrNull() ?: task.id
                ))
            }
        }

        // Also upload LOCAL_ONLY tasks
        val localOnlyTasks = taskDao.getTasksBySyncStatus(SyncStatus.LOCAL_ONLY)
        for (task in localOnlyTasks) {
            val result = cloudTaskRepository.uploadTask(task)
            if (result.isSuccess) {
                taskDao.updateTask(task.copy(
                    syncStatus = SyncStatus.SYNCED,
                    cloudId = result.getOrNull() ?: task.id
                ))
            }
        }
    }

    /**
     * Delete cloud notes marked for deletion.
     */
    private suspend fun deletePendingNotes() {
        val pendingDelete = noteDao.getNotesBySyncStatus(SyncStatus.PENDING_DELETE)
        for (note in pendingDelete) {
            cloudNoteRepository.deleteNote(note.id)
            noteDao.deleteNote(note)  // Remove from local DB after cloud deletion
        }
    }

    /**
     * Delete cloud tasks marked for deletion.
     */
    private suspend fun deletePendingTasks() {
        val pendingDelete = taskDao.getTasksBySyncStatus(SyncStatus.PENDING_DELETE)
        for (task in pendingDelete) {
            cloudTaskRepository.deleteTask(task.id)
            taskDao.deleteTask(task)
        }
    }

    /**
     * Download all notes from cloud and merge with local data.
     */
    private suspend fun downloadAndMergeNotes() {
        val cloudResult = cloudNoteRepository.fetchAllNotes()
        if (cloudResult.isFailure) return

        val cloudNotes = cloudResult.getOrNull() ?: return
        val localNotes = noteDao.getAllNotesList()
        val localNotesMap = localNotes.associateBy { it.id }

        for (cloudNote in cloudNotes) {
            val localNote = localNotesMap[cloudNote.id]

            if (localNote == null) {
                // Cloud note doesn't exist locally — download it
                noteDao.insertNote(cloudNote)
            } else if (localNote.syncStatus == SyncStatus.SYNCED) {
                // Both exist and local is synced — cloud might have newer changes from another device
                val resolved = ConflictResolver.resolveNoteConflict(localNote, cloudNote)
                if (resolved.id == cloudNote.id && cloudNote.updatedAt > localNote.updatedAt) {
                    noteDao.updateNote(cloudNote)
                }
            }
            // If local has PENDING_UPLOAD, we already uploaded it, so local wins
        }
    }

    /**
     * Download all tasks from cloud and merge with local data.
     */
    private suspend fun downloadAndMergeTasks() {
        val cloudResult = cloudTaskRepository.fetchAllTasks()
        if (cloudResult.isFailure) return

        val cloudTasks = cloudResult.getOrNull() ?: return
        val localTasks = taskDao.getAllTasksList()
        val localTasksMap = localTasks.associateBy { it.id }

        for (cloudTask in cloudTasks) {
            val localTask = localTasksMap[cloudTask.id]

            if (localTask == null) {
                // Cloud task doesn't exist locally — download it
                taskDao.insertTask(cloudTask)
            } else if (localTask.syncStatus == SyncStatus.SYNCED) {
                // Both exist and local is synced — check for cloud updates
                val resolved = ConflictResolver.resolveTaskConflict(localTask, cloudTask)
                if (resolved.id == cloudTask.id && cloudTask.updatedAt > localTask.updatedAt) {
                    taskDao.updateTask(cloudTask)
                }
            }
        }
    }

    /**
     * Mark a note as needing sync after local modification.
     */
    suspend fun markNoteForSync(note: Note) {
        val status = if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        noteDao.updateNote(note.copy(syncStatus = status))
    }

    /**
     * Mark a task as needing sync after local modification.
     */
    suspend fun markTaskForSync(task: Task) {
        val status = if (task.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        taskDao.updateTask(task.copy(syncStatus = status))
    }

    /**
     * Mark a note for cloud deletion.
     */
    suspend fun markNoteForDeletion(note: Note) {
        if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            // Never synced — just delete locally
            noteDao.deleteNote(note)
        } else {
            // Was synced — mark for cloud deletion
            noteDao.updateNote(note.copy(syncStatus = SyncStatus.PENDING_DELETE))
        }
    }

    /**
     * Mark a task for cloud deletion.
     */
    suspend fun markTaskForDeletion(task: Task) {
        if (task.syncStatus == SyncStatus.LOCAL_ONLY) {
            taskDao.deleteTask(task)
        } else {
            taskDao.updateTask(task.copy(syncStatus = SyncStatus.PENDING_DELETE))
        }
    }

    /**
     * Reset sync state to idle.
     */
    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }
}
