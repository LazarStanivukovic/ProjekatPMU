package com.example.projekat.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.TaskRepository
import com.example.projekat.notification.DeadlineScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val inProgressCount: Int = 0,
    val completedCount: Int = 0
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val deadlineScheduler: DeadlineScheduler
) : ViewModel() {

    val uiState: StateFlow<TasksUiState> = taskRepository.getAllTasks()
        .map { tasks ->
            TasksUiState(
                tasks = tasks,
                inProgressCount = tasks.count { it.status == TaskStatus.IN_PROGRESS },
                completedCount = tasks.count { it.status == TaskStatus.COMPLETED }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TasksUiState()
        )

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            taskRepository.toggleTaskStatus(task)
            // Handle notification scheduling based on new status
            val newStatus = when (task.status) {
                TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
                TaskStatus.COMPLETED -> TaskStatus.IN_PROGRESS
            }
            if (newStatus == TaskStatus.COMPLETED) {
                deadlineScheduler.cancelDeadlineNotification(task.id)
            } else if (task.deadline != null) {
                // Re-enable notification if task moved back to in-progress
                deadlineScheduler.scheduleDeadlineNotification(
                    task.id,
                    task.title,
                    task.deadline
                )
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            deadlineScheduler.cancelDeadlineNotification(task.id)
            taskRepository.deleteTask(task)
        }
    }
}
