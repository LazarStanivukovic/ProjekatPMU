package com.example.projekat.ui.screens.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.Note
import com.example.projekat.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteDetailUiState(
    val id: String? = null,
    val title: String = "",
    val content: String = "",
    val imageUris: List<String> = emptyList(),
    val colorIndex: Int = 0,
    val isBookmarked: Boolean = false,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val showColorPicker: Boolean = false
)

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId: String? = savedStateHandle.get<String>("noteId")

    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()

    // Track whether the note has been persisted at least once (for new notes)
    private var persistedNoteId: String? = null
    private var autoSaveJob: Job? = null
    private companion object {
        const val AUTO_SAVE_DELAY_MS = 800L
    }

    init {
        if (noteId != null) {
            loadNote(noteId)
        }
    }

    private fun loadNote(noteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                persistedNoteId = note.id
                _uiState.value = NoteDetailUiState(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    imageUris = note.imageUris,
                    colorIndex = note.colorIndex,
                    isBookmarked = note.isBookmarked,
                    isNew = false,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    fun updateContent(content: String) {
        _uiState.value = _uiState.value.copy(content = content)
        scheduleAutoSave()
    }

    fun updateColorIndex(index: Int) {
        _uiState.value = _uiState.value.copy(colorIndex = index)
        scheduleAutoSave()
    }

    fun addImageUri(uri: String) {
        _uiState.value = _uiState.value.copy(
            imageUris = _uiState.value.imageUris + uri
        )
        scheduleAutoSave()
    }

    fun removeImageUri(uri: String) {
        _uiState.value = _uiState.value.copy(
            imageUris = _uiState.value.imageUris - uri
        )
        scheduleAutoSave()
    }

    fun toggleBookmark() {
        _uiState.value = _uiState.value.copy(isBookmarked = !_uiState.value.isBookmarked)
        scheduleAutoSave()
    }

    fun toggleColorPicker() {
        _uiState.value = _uiState.value.copy(showColorPicker = !_uiState.value.showColorPicker)
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            performSave()
        }
    }

    /** Called when user navigates back — flush any pending save immediately. */
    fun saveOnExit() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            performSave()
        }
    }

    private suspend fun performSave() {
        val state = _uiState.value
        // Don't save empty notes
        if (state.title.isBlank() && state.content.isBlank()) return

        val now = System.currentTimeMillis()

        if (persistedNoteId == null) {
            // First time saving a new note
            val note = Note(
                title = state.title,
                content = state.content,
                imageUris = state.imageUris,
                colorIndex = state.colorIndex,
                isBookmarked = state.isBookmarked,
                createdAt = now,
                updatedAt = now
            )
            noteRepository.insertNote(note)
            persistedNoteId = note.id
            _uiState.value = state.copy(id = note.id, isNew = false)
        } else {
            val existingNote = noteRepository.getNoteById(persistedNoteId!!) ?: return
            noteRepository.updateNote(
                existingNote.copy(
                    title = state.title,
                    content = state.content,
                    imageUris = state.imageUris,
                    colorIndex = state.colorIndex,
                    isBookmarked = state.isBookmarked
                )
            )
        }
    }

    fun deleteNote() {
        viewModelScope.launch {
            val state = _uiState.value
            autoSaveJob?.cancel()
            if (persistedNoteId != null) {
                val note = noteRepository.getNoteById(persistedNoteId!!) ?: return@launch
                noteRepository.softDeleteNote(note)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
