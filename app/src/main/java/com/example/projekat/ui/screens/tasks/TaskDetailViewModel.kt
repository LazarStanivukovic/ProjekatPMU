package com.example.projekat.ui.screens.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.NoteRepository
import com.example.projekat.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val id: String? = null,
    val title: String = "",
    val description: String = "",
    val status: TaskStatus = TaskStatus.IN_PROGRESS,
    val deadline: Long? = null,
    val noteId: String? = null,
    val attachedNote: Note? = null,
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val availableNotes: List<Note> = emptyList()
)

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val taskId: String? = savedStateHandle.get<String>("taskId")

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

    private var persistedTaskId: String? = null
    private var autoSaveJob: Job? = null
    private companion object {
        const val AUTO_SAVE_DELAY_MS = 800L
    }

    init {
        viewModelScope.launch {
            if (taskId != null) {
                loadTask(taskId)
            }
            loadAvailableNotes()
        }
    }

    private suspend fun loadTask(taskId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val task = taskRepository.getTaskById(taskId)
        if (task != null) {
            persistedTaskId = task.id
            val attachedNote = task.noteId?.let { noteRepository.getNoteById(it) }
            _uiState.value = _uiState.value.copy(
                id = task.id,
                title = task.title,
                description = task.description,
                status = task.status,
                deadline = task.deadline,
                noteId = task.noteId,
                attachedNote = attachedNote,
                isNew = false,
                isLoading = false
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
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
        scheduleAutoSave()
    }

    fun updateDeadline(deadline: Long?) {
        _uiState.value = _uiState.value.copy(deadline = deadline)
        scheduleAutoSave()
    }

    fun attachNote(note: Note?) {
        _uiState.value = _uiState.value.copy(
            noteId = note?.id,
            attachedNote = note
        )
        scheduleAutoSave()
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
        // Don't save empty tasks
        if (state.title.isBlank()) return

        val now = System.currentTimeMillis()

        if (persistedTaskId == null) {
            // First time saving a new task
            val task = Task(
                title = state.title,
                description = state.description,
                status = state.status,
                deadline = state.deadline,
                noteId = state.noteId,
                createdAt = now,
                updatedAt = now
            )
            taskRepository.insertTask(task)
            persistedTaskId = task.id
            _uiState.value = state.copy(id = task.id, isNew = false)
        } else {
            val existingTask = taskRepository.getTaskById(persistedTaskId!!) ?: return
            taskRepository.updateTask(
                existingTask.copy(
                    title = state.title,
                    description = state.description,
                    status = state.status,
                    deadline = state.deadline,
                    noteId = state.noteId
                )
            )
        }
    }

    fun deleteTask() {
        viewModelScope.launch {
            val state = _uiState.value
            autoSaveJob?.cancel()
            if (persistedTaskId != null) {
                taskRepository.deleteTaskById(persistedTaskId!!)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }
}
