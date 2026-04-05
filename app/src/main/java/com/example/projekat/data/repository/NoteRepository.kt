package com.example.projekat.data.repository

import com.example.projekat.data.local.NoteDao
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getBookmarkedNotes(): Flow<List<Note>> = noteDao.getBookmarkedNotes()

    fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(noteId: String): Note? = noteDao.getNoteById(noteId)

    suspend fun insertNote(note: Note) {
        // New notes start as LOCAL_ONLY, will be synced later
        noteDao.insertNote(note.copy(syncStatus = SyncStatus.LOCAL_ONLY))
    }

    suspend fun updateNote(note: Note) {
        // Mark as PENDING_UPLOAD when modified (unless it's still LOCAL_ONLY)
        val newSyncStatus = if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        noteDao.updateNote(
            note.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = newSyncStatus
            )
        )
    }

    suspend fun softDeleteNote(note: Note) {
        val newSyncStatus = if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        noteDao.updateNote(
            note.copy(
                isDeleted = true,
                deletedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                syncStatus = newSyncStatus
            )
        )
    }

    suspend fun restoreNote(note: Note) {
        val newSyncStatus = if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        noteDao.updateNote(
            note.copy(
                isDeleted = false,
                deletedAt = null,
                updatedAt = System.currentTimeMillis(),
                syncStatus = newSyncStatus
            )
        )
    }

    suspend fun permanentlyDeleteNote(note: Note) {
        // If synced, mark for cloud deletion; otherwise just delete locally
        if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            noteDao.deleteNote(note)
        } else {
            noteDao.updateNote(note.copy(syncStatus = SyncStatus.PENDING_DELETE))
        }
    }

    suspend fun cleanupOldDeletedNotes() {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        noteDao.deleteNotesOlderThan(sevenDaysAgo)
    }

    suspend fun emptyTrash() = noteDao.deleteAllDeletedNotes()

    suspend fun toggleBookmark(note: Note) {
        val newSyncStatus = if (note.syncStatus == SyncStatus.LOCAL_ONLY) {
            SyncStatus.LOCAL_ONLY
        } else {
            SyncStatus.PENDING_UPLOAD
        }
        noteDao.updateNote(
            note.copy(
                isBookmarked = !note.isBookmarked,
                updatedAt = System.currentTimeMillis(),
                syncStatus = newSyncStatus
            )
        )
    }

    suspend fun getAllNotesList(): List<Note> = noteDao.getAllNotesList()
}
