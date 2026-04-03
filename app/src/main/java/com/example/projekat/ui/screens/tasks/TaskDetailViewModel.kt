package com.example.projekat.ui.screens.tasks

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.ChecklistItem
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskPriority
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.NoteRepository
import com.example.projekat.data.repository.TaskRepository
import com.example.projekat.notification.DeadlineScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TaskDetailUiState(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val deadline: Long? = null,
    val noteId: String? = null,
    val attachedNote: Note? = null,
    val colorIndex: Int = 0,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val showColorPicker: Boolean = false,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val availableNotes: List<Note> = emptyList(),
    val showUndoDialog: Boolean = false,
    val hasUnsavedChanges: Boolean = false
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    private val deadlineScheduler: DeadlineScheduler,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get<String>("taskId")

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var persistedTaskId: String? = null
    private var autoSaveJob: Job? = null
    
    // Store the ORIGINAL state when screen was opened (for shake-to-undo)
    private var originalState: TaskDetailUiState? = null
    
    private companion object {
        const val AUTO_SAVE_DELAY_MS = 800L
    }

    init {
        viewModelScope.launch {
            if (taskId != null) {
                loadTask(taskId)
            } else {
                // New task - save empty state as original after notes are loaded
                loadAvailableNotes()
                originalState = _uiState.value
            }
            if (taskId != null) {
                loadAvailableNotes()
            }
        }
    }

    private suspend fun loadTask(taskId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val task = taskRepository.getTaskById(taskId)
        if (task != null) {
            persistedTaskId = task.id
            val attachedNote = task.noteId?.let { noteRepository.getNoteById(it) }
            val loadedState = TaskDetailUiState(
                id = task.id,
                title = task.title,
                description = task.description,
                status = task.status,
                priority = task.priority,
                deadline = task.deadline,
                noteId = task.noteId,
                attachedNote = attachedNote,
                colorIndex = task.colorIndex,
                checklistItems = task.checklistItems,
                isNew = false,
                isLoading = false,
                availableNotes = _uiState.value.availableNotes
            )
            _uiState.value = loadedState
            originalState = loadedState  // Save original state for shake-to-undo
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
            originalState = _uiState.value
        }
    }

    private suspend fun loadAvailableNotes() {
        val notes = noteRepository.getAllNotesList()
        _uiState.value = _uiState.value.copy(availableNotes = notes)
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
        scheduleAutoSave()
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        scheduleAutoSave()
    }

    fun updateStatus(status: TaskStatus) {
        _uiState.value = _uiState.value.copy(status = status)
        // If task is completed, cancel any pending deadline notification
        if (status == TaskStatus.COMPLETED) {
            persistedTaskId?.let { deadlineScheduler.cancelDeadlineNotification(it) }
        } else {
            // If task is moved back to in-progress and has a deadline, re-schedule
            val deadline = _uiState.value.deadline
            if (deadline != null && persistedTaskId != null) {
                deadlineScheduler.scheduleDeadlineNotification(
                    persistedTaskId!!,
                    _uiState.value.title,
                    deadline
                )
            }
        }
        scheduleAutoSave()
    }

    fun updateDeadline(deadline: Long?) {
        _uiState.value = _uiState.value.copy(deadline = deadline)
        scheduleAutoSave()
    }

    fun updatePriority(priority: TaskPriority) {
        _uiState.value = _uiState.value.copy(priority = priority)
        scheduleAutoSave()
    }

    fun attachNote(note: Note?) {
        _uiState.value = _uiState.value.copy(
            noteId = note?.id,
            attachedNote = note
        )
        scheduleAutoSave()
    }

    fun updateColorIndex(index: Int) {
        _uiState.value = _uiState.value.copy(colorIndex = index)
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

    /**
     * Re-fetches the attached note from the database so the preview stays
     * up-to-date after the user edits/deletes images in the note screen.
     */
    fun refreshAttachedNote() {
        val noteId = _uiState.value.noteId ?: return
        viewModelScope.launch {
            val freshNote = noteRepository.getNoteById(noteId)
            _uiState.value = _uiState.value.copy(attachedNote = freshNote)
        }
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
        // Don't save empty tasks
        if (state.title.isBlank()) return

        val now = System.currentTimeMillis()

        if (persistedTaskId == null) {
            // First time saving a new task
            val task = Task(
                title = state.title,
                description = state.description,
                status = state.status,
                priority = state.priority,
                deadline = state.deadline,
                noteId = state.noteId,
                colorIndex = state.colorIndex,
                checklistItems = state.checklistItems,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insertTask(task)
            persistedTaskId = task.id
            _uiState.value = state.copy(id = task.id, isNew = false, hasUnsavedChanges = false)
            // Note: We don't update originalState here - it stays as the initial state

            // Schedule notification for new task with deadline
            if (state.deadline != null && state.status == TaskStatus.IN_PROGRESS) {
                deadlineScheduler.scheduleDeadlineNotification(
                    task.id,
                    state.title,
                    state.deadline
                )
            }
        } else {
            val existingTask = taskRepository.getTaskById(persistedTaskId!!) ?: return
            taskRepository.updateTask(
                existingTask.copy(
                    title = state.title,
                    description = state.description,
                    status = state.status,
                    priority = state.priority,
                    deadline = state.deadline,
                    noteId = state.noteId,
                    colorIndex = state.colorIndex,
                    checklistItems = state.checklistItems
                )
            )
            _uiState.value = state.copy(hasUnsavedChanges = false)
            // Note: We don't update originalState here - it stays as the initial state

            // Update deadline notification scheduling
            if (state.deadline != null && state.status == TaskStatus.IN_PROGRESS) {
                deadlineScheduler.scheduleDeadlineNotification(
                    persistedTaskId!!,
                    state.title,
                    state.deadline
                )
            } else {
                // No deadline or task completed — cancel notification
                deadlineScheduler.cancelDeadlineNotification(persistedTaskId!!)
            }
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            autoSaveJob?.cancel()
            // Cancel any pending notification before deleting
            persistedTaskId?.let { deadlineScheduler.cancelDeadlineNotification(it) }
            if (persistedTaskId != null) {
                taskRepository.deleteTaskById(persistedTaskId!!)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    // ---- Undo functionality ----
    
    /** Show undo confirmation dialog (called when shake is detected) */
    fun showUndoDialog() {
        val hasChanges = hasChangesSinceOriginal()
        Log.d("TaskDetailViewModel", "showUndoDialog called: hasChangesSinceOriginal=$hasChanges")
        
        // Show dialog if there are any changes compared to original state
        if (hasChanges) {
            Log.d("TaskDetailViewModel", "Showing undo dialog")
            _uiState.value = _uiState.value.copy(showUndoDialog = true)
        } else {
            Log.d("TaskDetailViewModel", "No changes to undo, dialog not shown")
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
            Log.d("TaskDetailViewModel", "Reverting to original state")
            // Revert to original state, preserving availableNotes
            _uiState.value = original.copy(
                showUndoDialog = false,
                hasUnsavedChanges = false,
                availableNotes = _uiState.value.availableNotes
            )
            // Also need to update the database to revert the auto-saved changes
            viewModelScope.launch {
                if (persistedTaskId != null) {
                    val existingTask = taskRepository.getTaskById(persistedTaskId!!)
                    if (existingTask != null) {
                        taskRepository.updateTask(
                            existingTask.copy(
                                title = original.title,
                                description = original.description,
                                status = original.status,
                                priority = original.priority,
                                deadline = original.deadline,
                                noteId = original.noteId,
                                colorIndex = original.colorIndex,
                                checklistItems = original.checklistItems
                            )
                        )
                    }
                }
            }
        } else {
            Log.d("TaskDetailViewModel", "No original state to revert to")
        }
    }

    /** Check if there are differences between current state and original state */
    private fun hasChangesSinceOriginal(): Boolean {
        val original = originalState ?: return false
        val current = _uiState.value
        return original.title != current.title ||
               original.description != current.description ||
               original.status != current.status ||
               original.priority != current.priority ||
               original.deadline != current.deadline ||
               original.noteId != current.noteId ||
               original.colorIndex != current.colorIndex ||
               original.checklistItems != current.checklistItems
    }
}
