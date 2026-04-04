package com.example.projekat.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.AiScheduleRepository
import com.example.projekat.data.repository.ScheduleResult
import com.example.projekat.data.repository.TaskRepository
import com.example.projekat.location.GeofenceManager
import com.example.projekat.notification.DeadlineScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val inProgressCount: Int = 0,
    val completedCount: Int = 0,
    // AI scheduling state
    val isSelectionMode: Boolean = false,
    val selectedTaskIds: Set<String> = emptySet(),
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val scheduleResults: List<ScheduleResult>? = null,
    val showScheduleDialog: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val aiScheduleRepository: AiScheduleRepository,
    private val deadlineScheduler: DeadlineScheduler,
    private val geofenceManager: GeofenceManager
) : ViewModel() {

    private val _aiState = MutableStateFlow(AiState())

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.getAllTasks(),
        _aiState
    ) { tasks, aiState ->
        TasksUiState(
            tasks = tasks,
            inProgressCount = tasks.count { it.status == TaskStatus.IN_PROGRESS },
            completedCount = tasks.count { it.status == TaskStatus.COMPLETED },
            isSelectionMode = aiState.isSelectionMode,
            selectedTaskIds = aiState.selectedTaskIds,
            isAiLoading = aiState.isLoading,
            aiError = aiState.error,
            scheduleResults = aiState.results,
            showScheduleDialog = aiState.showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskStatus(task)
            val newStatus = when (task.status) {
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
            }
            if (newStatus == TaskStatus.COMPLETED) {
                // Cancel deadline notification
                deadlineScheduler.cancelDeadlineNotification(task.id)
                // Remove geofence when task is completed
                geofenceManager.removeGeofenceForTask(task.id)
            } else {
                // Re-schedule deadline notification
                if (task.deadline != null) {
                    deadlineScheduler.scheduleDeadlineNotification(
                        task.id,
                        task.title,
                        task.deadline
                    )
                }
                // Re-add geofence when task is moved back to in-progress
                if (task.locationLat != null && task.locationLng != null) {
                    geofenceManager.addGeofenceForTask(
                        task.id,
                        task.title,
                        task.locationLat,
                        task.locationLng,
                        task.locationRadius
                    )
                }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deadlineScheduler.cancelDeadlineNotification(task.id)
            // Remove geofence when task is deleted
            geofenceManager.removeGeofenceForTask(task.id)
            taskRepository.deleteTask(task)
        }
    }

    // --- AI Scheduling ---

    /**
     * Returns the start of tomorrow (midnight) in milliseconds.
     * Tasks must have a deadline >= this value to be eligible for AI scheduling.
     */
    private fun startOfTomorrow(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Check if a task is eligible for AI scheduling:
     * - must be IN_PROGRESS
     * - must have a deadline
     * - deadline must be tomorrow or later
     */
    fun isTaskEligibleForAi(task: Task): Boolean {
        return task.status == TaskStatus.IN_PROGRESS &&
                task.deadline != null &&
                task.deadline >= startOfTomorrow()
    }

    /**
     * Enter selection mode - user picks which tasks to schedule.
     */
    fun enterSelectionMode() {
        val eligibleIds = uiState.value.tasks
            .filter { isTaskEligibleForAi(it) }
            .map { it.id }
            .toSet()
        _aiState.update {
            it.copy(
                isSelectionMode = true,
                selectedTaskIds = eligibleIds,
                error = null,
                results = null,
                showDialog = false
            )
        }
    }

    fun exitSelectionMode() {
        _aiState.update { AiState() }
    }

    fun toggleTaskSelection(taskId: String) {
        _aiState.update { state ->
            val newSelection = if (taskId in state.selectedTaskIds) {
                state.selectedTaskIds - taskId
            } else {
                state.selectedTaskIds + taskId
            }
            state.copy(selectedTaskIds = newSelection)
        }
    }

    /**
     * Send selected tasks to AI backend for scheduling.
     */
    fun requestAiSchedule() {
        val selectedIds = _aiState.value.selectedTaskIds
        if (selectedIds.isEmpty()) return

        val tasks = uiState.value.tasks.filter { it.id in selectedIds }
        // Only tasks with deadlines can be scheduled
        val schedulableTasks = tasks.filter { it.deadline != null }
        if (schedulableTasks.isEmpty()) {
            _aiState.update { it.copy(error = "Izabrani taskovi nemaju rokove.") }
            return
        }

        viewModelScope.launch {
            _aiState.update { it.copy(isLoading = true, error = null) }

            val result = aiScheduleRepository.requestSchedule(schedulableTasks)

            result.onSuccess { scheduleResults ->
                _aiState.update {
                    it.copy(
                        isLoading = false,
                        results = scheduleResults,
                        showDialog = true
                    )
                }
            }.onFailure { error ->
                val message = when {
                    error.message?.contains("connect", ignoreCase = true) == true ->
                        "Server nije dostupan. Proverite da li je pokrenut."
                    error.message?.contains("timeout", ignoreCase = true) == true ->
                        "Zahtev je istekao. Pokusajte ponovo."
                    else -> "Greska: ${error.message ?: "Nepoznata greska"}"
                }
                _aiState.update {
                    it.copy(isLoading = false, error = message)
                }
            }
        }
    }

    /**
     * Apply AI-suggested scheduled dates to tasks (overwrite deadlines with scheduled dates).
     */
    fun applySchedule() {
        val results = _aiState.value.results ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        viewModelScope.launch {
            for (scheduleResult in results) {
                val task = uiState.value.tasks.find { it.id == scheduleResult.taskId } ?: continue
                try {
                    val scheduledDate = dateFormat.parse(scheduleResult.scheduledDate) ?: continue
                    val updatedTask = task.copy(
                        deadline = scheduledDate.time,
                        updatedAt = System.currentTimeMillis()
                    )
                    taskRepository.updateTask(updatedTask)
                    // Re-schedule notification for the new deadline
                    deadlineScheduler.scheduleDeadlineNotification(
                        updatedTask.id,
                        updatedTask.title,
                        updatedTask.deadline!!
                    )
                } catch (_: Exception) {
                    // Skip tasks with unparseable dates
                }
            }
            // Exit selection mode after applying
            _aiState.update { AiState() }
        }
    }

    fun dismissScheduleDialog() {
        _aiState.update { it.copy(showDialog = false, results = null) }
    }

    fun clearAiError() {
        _aiState.update { it.copy(error = null) }
    }

    private data class AiState(
        val isSelectionMode: Boolean = false,
        val selectedTaskIds: Set<String> = emptySet(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val results: List<ScheduleResult>? = null,
        val showDialog: Boolean = false
    )
}
