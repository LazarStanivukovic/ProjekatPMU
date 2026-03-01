package com.example.projekat.data.repository

import com.example.projekat.data.local.NoteDao
import com.example.projekat.data.model.Note
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

    suspend fun insertNote(note: Note) = noteDao.insertNote(note)

    suspend fun updateNote(note: Note) = noteDao.updateNote(
        note.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun softDeleteNote(note: Note) = noteDao.updateNote(
        note.copy(
            isDeleted = true,
            deletedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun restoreNote(note: Note) = noteDao.updateNote(
        note.copy(
            isDeleted = false,
            deletedAt = null,
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun permanentlyDeleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun cleanupOldDeletedNotes() {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        noteDao.deleteNotesOlderThan(sevenDaysAgo)
    }

    suspend fun toggleBookmark(note: Note) = noteDao.updateNote(
        note.copy(
            isBookmarked = !note.isBookmarked,
            updatedAt = System.currentTimeMillis()
        )
    )

    suspend fun getAllNotesList(): List<Note> = noteDao.getAllNotesList()
}
