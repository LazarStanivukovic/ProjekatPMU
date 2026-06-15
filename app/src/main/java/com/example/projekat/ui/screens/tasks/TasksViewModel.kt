package com.example.projekat.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.RepeatInterval
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.AiScheduleRepository
import com.example.projekat.data.repository.ScheduleResult
import com.example.projekat.data.repository.TaskRepository
import com.example.projekat.location.GeofenceManager
import com.example.projekat.notification.DeadlineScheduler
import com.example.projekat.data.sync.SyncManager
import com.example.projekat.data.sync.SyncState
import com.google.firebase.auth.FirebaseAuth
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
    val pausedCount: Int = 0,
    val canceledCount: Int = 0,
    // AI scheduling state
    val isSelectionMode: Boolean = false,
    val selectedTaskIds: Set<String> = emptySet(),
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
    val scheduleResults: List<ScheduleResult>? = null,
    val showScheduleDialog: Boolean = false,
    val showPromptDialog: Boolean = false,
    val pendingInvites: List<Task> = emptyList(),
    val isSyncing: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val aiScheduleRepository: AiScheduleRepository,
    private val deadlineScheduler: DeadlineScheduler,
    private val geofenceManager: GeofenceManager,
    private val auth: FirebaseAuth,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _aiState = MutableStateFlow(AiState())

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.getAllTasks(),
        _aiState,
        syncManager.syncState
    ) { tasks, aiState, syncState ->
        val currentUserEmail = auth.currentUser?.email
        val pendingInvitesList = tasks.filter { it.pendingInvites.contains(currentUserEmail) }
        val normalTasks = tasks.filter { !it.pendingInvites.contains(currentUserEmail) }

        TasksUiState(
            tasks = normalTasks,
            inProgressCount = normalTasks.count { it.status == TaskStatus.IN_PROGRESS },
            completedCount = normalTasks.count { it.status == TaskStatus.COMPLETED },
            pausedCount = normalTasks.count { it.status == TaskStatus.PAUSED },
            canceledCount = normalTasks.count { it.status == TaskStatus.CANCELED },
            isSelectionMode = aiState.isSelectionMode,
            selectedTaskIds = aiState.selectedTaskIds,
            isAiLoading = aiState.isLoading,
            aiError = aiState.error,
            scheduleResults = aiState.results,
            showScheduleDialog = aiState.showDialog,
            showPromptDialog = aiState.showPromptDialog,
            pendingInvites = pendingInvitesList,
            isSyncing = syncState is SyncState.Syncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    fun swipeToRefresh() {
        viewModelScope.launch {
            syncManager.syncAll()
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            val newStatus = when (task.status) {
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
                TaskStatus.PAUSED -> TaskStatus.IN_PROGRESS
                TaskStatus.CANCELED -> TaskStatus.IN_PROGRESS
            }
            
            if (newStatus == TaskStatus.COMPLETED && task.repeatInterval != RepeatInterval.NONE) {
                // Repeating task logic
                val nextStartDate = calculateNextDate(task.startDate, task.repeatInterval)
                val nextEndDate = calculateNextDate(task.endDate, task.repeatInterval)
                
                val shouldCreateNext = if (task.repeatEndDate != null && nextStartDate != null) {
                    nextStartDate <= task.repeatEndDate
                } else {
                    true
                }

                if (shouldCreateNext) {
                    val nextTask = task.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        status = TaskStatus.IN_PROGRESS,
                        startDate = nextStartDate,
                        endDate = nextEndDate,
                        lastCompletedAt = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    taskRepository.insertTask(nextTask)
                    
                    // Schedule notification for new task
                    if (nextTask.startDate != null) {
                        deadlineScheduler.scheduleDeadlineNotification(
                            nextTask.id,
                            nextTask.title,
                            nextTask.startDate,
                            nextTask.hasTime
                        )
                    }
                    if (nextTask.locationLat != null && nextTask.locationLng != null) {
                        geofenceManager.addGeofenceForTask(
                            nextTask.id,
                            nextTask.title,
                            nextTask.locationLat,
                            nextTask.locationLng,
                            nextTask.locationRadius
                        )
                    }
                }
                
                // Update current task to completed
                val completedTask = task.copy(
                    status = TaskStatus.COMPLETED,
                    lastCompletedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                taskRepository.updateTask(completedTask)
                
                deadlineScheduler.cancelDeadlineNotification(completedTask.id)
                geofenceManager.removeGeofenceForTask(completedTask.id)
            } else {
                // Normal toggle logic
                val updatedTask = task.copy(
                    status = newStatus,
                    updatedAt = System.currentTimeMillis()
                )
                taskRepository.updateTask(updatedTask)

                if (newStatus == TaskStatus.COMPLETED) {
                    deadlineScheduler.cancelDeadlineNotification(task.id)
                    geofenceManager.removeGeofenceForTask(task.id)
                } else {
                    if (updatedTask.startDate != null) {
                        deadlineScheduler.scheduleDeadlineNotification(
                            updatedTask.id,
                            updatedTask.title,
                            updatedTask.startDate,
                            updatedTask.hasTime
                        )
                    }
                    if (updatedTask.locationLat != null && updatedTask.locationLng != null) {
                        geofenceManager.addGeofenceForTask(
                            updatedTask.id,
                            updatedTask.title,
                            updatedTask.locationLat,
                            updatedTask.locationLng,
                            updatedTask.locationRadius
                        )
                    }
                }
            }
        }
    }

    private fun calculateNextDate(currentMillis: Long?, interval: RepeatInterval): Long? {
        if (currentMillis == null) return null
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        when (interval) {
            RepeatInterval.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatInterval.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatInterval.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RepeatInterval.YEARLY -> cal.add(Calendar.YEAR, 1)
            RepeatInterval.NONE -> return currentMillis
        }
        return cal.timeInMillis
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deadlineScheduler.cancelDeadlineNotification(task.id)
            // Remove geofence when task is deleted
            geofenceManager.removeGeofenceForTask(task.id)
            taskRepository.deleteTask(task)
        }
    }

    fun acceptInvite(task: Task) {
        val email = auth.currentUser?.email ?: return
        viewModelScope.launch {
            val updatedPending = task.pendingInvites.filter { it != email }
            val updatedShared = task.sharedWith + email
            val updatedTask = task.copy(
                pendingInvites = updatedPending,
                sharedWith = updatedShared
            )
            taskRepository.updateTask(updatedTask)
        }
    }

    fun declineInvite(task: Task) {
        val email = auth.currentUser?.email ?: return
        viewModelScope.launch {
            val updatedPending = task.pendingInvites.filter { it != email }
            val updatedTask = task.copy(
                pendingInvites = updatedPending
            )
            // If the user declines, they just remove themselves from pendingInvites.
            // If they are not owner or sharedWith, they will no longer fetch this task from Cloud.
            taskRepository.updateTask(updatedTask)
        }
    }

    // --- AI Scheduling ---

    /**
     * Returns the start of tomorrow (midnight) in milliseconds.
     * Tasks must have a start date >= this value to be eligible for AI scheduling.
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
     * - must have a start date
     * - start date must be tomorrow or later
     */
    fun isTaskEligibleForAi(task: Task): Boolean {
        return task.status == TaskStatus.IN_PROGRESS &&
                task.startDate != null &&
                task.startDate >= startOfTomorrow()
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

    fun showAiPromptDialog() {
        _aiState.update { it.copy(showPromptDialog = true) }
    }

    fun dismissAiPromptDialog() {
        _aiState.update { it.copy(showPromptDialog = false) }
    }

    /**
     * Send selected tasks to AI backend for scheduling.
     */
    fun requestAiSchedule(customPrompt: String? = null) {
        val selectedIds = _aiState.value.selectedTaskIds
        if (selectedIds.isEmpty()) return

        val tasks = uiState.value.tasks.filter { it.id in selectedIds }
        // Only tasks with start dates can be scheduled
        val schedulableTasks = tasks.filter { it.startDate != null }
        if (schedulableTasks.isEmpty()) {
            _aiState.update { it.copy(error = "Izabrani taskovi nemaju datume.", showPromptDialog = false) }
            return
        }

        viewModelScope.launch {
            _aiState.update { it.copy(isLoading = true, error = null, showPromptDialog = false) }

            val result = aiScheduleRepository.requestSchedule(schedulableTasks, customPrompt)

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
     * Apply AI-suggested scheduled dates to tasks (overwrite dates with scheduled dates).
     */
    fun applySchedule(selectedResultTaskIds: List<String>) {
        val results = _aiState.value.results ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

        viewModelScope.launch {
            for (scheduleResult in results) {
                if (scheduleResult.taskId !in selectedResultTaskIds) continue
                
                val task = uiState.value.tasks.find { it.id == scheduleResult.taskId } ?: continue
                try {
                    var scheduledStart = dateTimeFormat.parse(scheduleResult.scheduledStartDateTime)
                        ?: dateFormat.parse(scheduleResult.scheduledStartDateTime)
                        ?: continue
                    
                    val newStartMillis = scheduledStart.time
                    
                    val newEndMillis = if (scheduleResult.scheduledEndDateTime != null) {
                        dateTimeFormat.parse(scheduleResult.scheduledEndDateTime)
                            ?: dateFormat.parse(scheduleResult.scheduledEndDateTime)
                            ?.time
                    } else null

                    val updatedTask = task.copy(
                        startDate = newStartMillis,
                        endDate = (newEndMillis ?: task.endDate) as Long?,
                        hasTime = scheduleResult.hasTime || scheduleResult.scheduledStartDateTime.contains(":"),
                        updatedAt = System.currentTimeMillis()
                    )
                    taskRepository.updateTask(updatedTask)
                    deadlineScheduler.scheduleDeadlineNotification(
                        updatedTask.id,
                        updatedTask.title,
                        updatedTask.startDate!!,
                        updatedTask.hasTime
                    )
                } catch (_: Exception) {
                    // Skip tasks with unparseable dates
                }
            }
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
        val showDialog: Boolean = false,
        val showPromptDialog: Boolean = false
    )
}
