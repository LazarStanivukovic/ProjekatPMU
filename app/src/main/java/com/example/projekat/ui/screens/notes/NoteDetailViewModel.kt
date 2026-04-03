package com.example.projekat.ui.screens.notes

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.Note
import com.example.projekat.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class NoteDetailUiState(
    val id: String? = null,
    val title: String = "",
    val content: String = "",
    val imageUris: List<String> = emptyList(),
    val checklistItems: List<ChecklistItem> = emptyList(),
    val colorIndex: Int = 0,
    val isBookmarked: Boolean = false,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val showColorPicker: Boolean = false,
    val showUndoDialog: Boolean = false,
    val hasUnsavedChanges: Boolean = false
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
    
    // Store the ORIGINAL state when screen was opened (for shake-to-undo)
    // This is different from auto-save state - it's the state when user first opened the note
    private var originalState: NoteDetailUiState? = null
    
    private companion object {
        const val AUTO_SAVE_DELAY_MS = 800L
    }

    init {
        if (noteId != null) {
            loadNote(noteId)
        } else {
            // New note - save empty state as original
            originalState = _uiState.value
        }
    }

    private fun loadNote(noteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val note = noteRepository.getNoteById(noteId)
            if (note != null) {
                persistedNoteId = note.id
                val loadedState = NoteDetailUiState(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    imageUris = note.imageUris,
                    checklistItems = note.checklistItems,
                    colorIndex = note.colorIndex,
                    isBookmarked = note.isBookmarked,
                    isNew = false,
                    isLoading = false
                )
                _uiState.value = loadedState
                originalState = loadedState  // Save original state for shake-to-undo
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
                originalState = _uiState.value  // Save empty state as original for new notes
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

    // ---- Checklist management ----
    fun addChecklistItem() {
        val currentItems = _uiState.value.checklistItems
        val newItem = ChecklistItem(
            id = UUID.randomUUID().toString(),
            text = "",
            isChecked = false,
            order = currentItems.size
        )
        _uiState.value = _uiState.value.copy(
            checklistItems = currentItems + newItem
        )
        scheduleAutoSave()
    }

    fun toggleChecklistItem(itemId: String) {
        val updatedItems = _uiState.value.checklistItems.map { item ->
            if (item.id == itemId) item.copy(isChecked = !item.isChecked) else item
        }
        _uiState.value = _uiState.value.copy(checklistItems = updatedItems)
        scheduleAutoSave()
    }

    fun updateChecklistItemText(itemId: String, newText: String) {
        val updatedItems = _uiState.value.checklistItems.map { item ->
            if (item.id == itemId) item.copy(text = newText) else item
        }
        _uiState.value = _uiState.value.copy(checklistItems = updatedItems)
        scheduleAutoSave()
    }

    fun deleteChecklistItem(itemId: String) {
        val updatedItems = _uiState.value.checklistItems
            .filter { it.id != itemId }
            .mapIndexed { index, item -> item.copy(order = index) }
        _uiState.value = _uiState.value.copy(checklistItems = updatedItems)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        _uiState.value = _uiState.value.copy(hasUnsavedChanges = true)
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
        // Don't save empty notes (but DO save if there are images or checklist items)
        if (state.title.isBlank() && state.content.isBlank() && state.imageUris.isEmpty() && state.checklistItems.isEmpty()) return

        val now = System.currentTimeMillis()

        if (persistedNoteId == null) {
            // First time saving a new note
            val note = Note(
                title = state.title,
                content = state.content,
                imageUris = state.imageUris,
                checklistItems = state.checklistItems,
                colorIndex = state.colorIndex,
                isBookmarked = state.isBookmarked,
                createdAt = now,
                updatedAt = now
            )
            noteRepository.insertNote(note)
            persistedNoteId = note.id
            _uiState.value = state.copy(id = note.id, isNew = false, hasUnsavedChanges = false)
            // Note: We don't update originalState here - it stays as the initial state
        } else {
            val existingNote = noteRepository.getNoteById(persistedNoteId!!) ?: return
            noteRepository.updateNote(
                existingNote.copy(
                    title = state.title,
                    content = state.content,
                    imageUris = state.imageUris,
                    checklistItems = state.checklistItems,
                    colorIndex = state.colorIndex,
                    isBookmarked = state.isBookmarked
                )
            )
            _uiState.value = state.copy(hasUnsavedChanges = false)
            // Note: We don't update originalState here - it stays as the initial state
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

    // ---- Undo functionality ----
    
    /** Show undo confirmation dialog (called when shake is detected) */
    fun showUndoDialog() {
        val hasChanges = hasChangesSinceOriginal()
        Log.d("NoteDetailViewModel", "showUndoDialog called: hasChangesSinceOriginal=$hasChanges")
        
        // Show dialog if there are any changes compared to original state
        if (hasChanges) {
            Log.d("NoteDetailViewModel", "Showing undo dialog")
            _uiState.value = _uiState.value.copy(showUndoDialog = true)
        } else {
            Log.d("NoteDetailViewModel", "No changes to undo, dialog not shown")
        }
    }

    /** Hide undo confirmation dialog */
    fun dismissUndoDialog() {
        _uiState.value = _uiState.value.copy(showUndoDialog = false)
    }

    /** Revert to original state (when screen was first opened) */
    fun revertToLastSaved() {
        autoSaveJob?.cancel()
        
        val original = originalState
        if (original != null) {
            Log.d("NoteDetailViewModel", "Reverting to original state")
            // Revert to original state
            _uiState.value = original.copy(
                showUndoDialog = false,
                hasUnsavedChanges = false
            )
            // Also need to update the database to revert the auto-saved changes
            viewModelScope.launch {
                if (persistedNoteId != null) {
                    val existingNote = noteRepository.getNoteById(persistedNoteId!!)
                    if (existingNote != null) {
                        noteRepository.updateNote(
                            existingNote.copy(
                                title = original.title,
                                content = original.content,
                                imageUris = original.imageUris,
                                checklistItems = original.checklistItems,
                                colorIndex = original.colorIndex,
                                isBookmarked = original.isBookmarked
                            )
                        )
                    }
                }
            }
        } else {
            Log.d("NoteDetailViewModel", "No original state to revert to")
        }
    }

    /** Check if there are differences between current state and original state */
    private fun hasChangesSinceOriginal(): Boolean {
        val original = originalState ?: return false
        val current = _uiState.value
        return original.title != current.title ||
               original.content != current.content ||
               original.imageUris != current.imageUris ||
               original.checklistItems != current.checklistItems ||
               original.colorIndex != current.colorIndex ||
               original.isBookmarked != current.isBookmarked
    }
}
