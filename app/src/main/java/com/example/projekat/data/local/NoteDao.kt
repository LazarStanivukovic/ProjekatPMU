package com.example.projekat.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isBookmarked = 0 AND syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isBookmarked = 1 AND isDeleted = 0 AND syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun getBookmarkedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 AND syncStatus != 'PENDING_DELETE' ORDER BY deletedAt DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isBookmarked = 0 AND syncStatus != 'PENDING_DELETE' AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :cutoffTime")
    suspend fun deleteNotesOlderThan(cutoffTime: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun deleteAllDeletedNotes()

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    suspend fun getAllNotesList(): List<Note>

    // Sync-related queries
    @Query("SELECT * FROM notes WHERE syncStatus = :status")
    suspend fun getNotesBySyncStatus(status: SyncStatus): List<Note>

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesIncludingDeleted(): List<Note>
}
