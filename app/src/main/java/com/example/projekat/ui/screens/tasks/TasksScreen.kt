package com.example.projekat.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.data.repository.ScheduleResult
import com.example.projekat.ui.components.ChecklistPreview
import com.example.projekat.ui.theme.NoteBlue
import com.example.projekat.ui.theme.NoteBlueDark
import com.example.projekat.ui.theme.NoteCardText
import com.example.projekat.ui.theme.NoteGreen
import com.example.projekat.ui.theme.NoteGreenDark
import com.example.projekat.ui.theme.NoteOrange
import com.example.projekat.ui.theme.NoteOrangeDark
import com.example.projekat.ui.theme.NotePink
import com.example.projekat.ui.theme.NotePinkDark
import com.example.projekat.ui.theme.NotePurple
import com.example.projekat.ui.theme.NotePurpleDark
import com.example.projekat.ui.theme.NoteYellow
import com.example.projekat.ui.theme.NoteYellowDark
import com.example.projekat.ui.theme.PriorityHigh
import com.example.projekat.ui.theme.PriorityLow
import com.example.projekat.ui.theme.PriorityMedium
import com.example.projekat.ui.theme.StatusCanceled
import com.example.projekat.ui.theme.StatusCompleted
import com.example.projekat.ui.theme.StatusInProgress
import com.example.projekat.ui.theme.StatusOverdue
import com.example.projekat.ui.theme.StatusPaused
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
private val taskColorsLight = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)
private val taskColorsDark = listOf(NoteYellowDark, NoteGreenDark, NoteBlueDark, NotePinkDark, NoteOrangeDark, NotePurpleDark)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Moji Taskovi", "Inbox")

    // Show error in snackbar
    LaunchedEffect(uiState.aiError) {
        uiState.aiError?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearAiError()
        }
    }

    // Schedule preview dialog
    if (uiState.showScheduleDialog && uiState.scheduleResults != null) {
        SchedulePreviewDialog(
            results = uiState.scheduleResults!!,
            onConfirm = { selectedIds -> viewModel.applySchedule(selectedIds) },
            onDismiss = { viewModel.dismissScheduleDialog() }
        )
    }

    if (uiState.showPromptDialog) {
        AiPromptDialog(
            onConfirm = { prompt -> viewModel.requestAiSchedule(prompt) },
            onDismiss = { viewModel.dismissAiPromptDialog() }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isSyncing,
        onRefresh = { viewModel.swipeToRefresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with AI button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isSelectionMode) "Izaberi taskove" else "Taskovi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                if (uiState.isSelectionMode) {
                    // Cancel selection mode
                    IconButton(onClick = { viewModel.exitSelectionMode() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Otkazi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // AI Schedule button
                    val hasEligibleTasks = uiState.tasks.any {
                        viewModel.isTaskEligibleForAi(it)
                    }
                    if (hasEligibleTasks) {
                        Button(
                            onClick = { viewModel.enterSelectionMode() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI Raspored",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            if (index == 1 && uiState.pendingInvites.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(uiState.pendingInvites.size.toString())
                                        }
                                    }
                                ) {
                                    Text(title, modifier = Modifier.padding(end = 12.dp))
                                }
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // Stats row
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "U toku",
                        count = uiState.inProgressCount,
                        color = StatusInProgress,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Pauzirano",
                        count = uiState.pausedCount,
                        color = StatusPaused,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Zavrseno",
                        count = uiState.completedCount,
                        color = StatusCompleted,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Otkazano",
                        count = uiState.canceledCount,
                        color = StatusCanceled,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selection mode info bar
                AnimatedVisibility(
                    visible = uiState.isSelectionMode,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Izabrano: ${uiState.selectedTaskIds.size} taskova",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.showAiPromptDialog() },
                                    enabled = uiState.selectedTaskIds.isNotEmpty() && !uiState.isAiLoading,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    if (uiState.isAiLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generisanje...")
                                    } else {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Rasporedi")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                val displayTasks = if (uiState.isSelectionMode) {
                    uiState.tasks.filter { viewModel.isTaskEligibleForAi(it) }
                } else {
                    uiState.tasks
                }

                if (displayTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CheckBox,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.isSelectionMode) "Nema taskova za AI raspored" else "Nema taskova",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayTasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        if (viewModel.isTaskEligibleForAi(task)) {
                                            viewModel.toggleTaskSelection(task.id)
                                        }
                                    } else {
                                        onTaskClick(task.id)
                                    }
                                },
                                onToggleStatus = {
                                    if (!uiState.isSelectionMode) {
                                        viewModel.toggleTaskStatus(task)
                                    }
                                },
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = task.id in uiState.selectedTaskIds,
                                isEligibleForSelection = viewModel.isTaskEligibleForAi(task)
                            )
                        }
                    }
                }
            } else {
                // Inbox / Invites Tab
                if (uiState.pendingInvites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nema novih pozivnica",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.pendingInvites, key = { it.id }) { task ->
                            InviteCard(
                                task = task,
                                onAccept = { viewModel.acceptInvite(task) },
                                onDecline = { viewModel.declineInvite(task) }
                            )
                        }
                    }
                }
            }
        }

        // FAB - hidden during selection mode and loading
        if (!uiState.isSelectionMode) {
            FloatingActionButton(
                onClick = onCreateTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Novi task")
            }
        }

        // Snackbar for errors
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isEligibleForSelection: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isPaused = task.status == TaskStatus.PAUSED
    val isCanceled = task.status == TaskStatus.CANCELED
    val primaryDate = task.endDate ?: task.startDate
    val isOverdue = primaryDate != null &&
            primaryDate < System.currentTimeMillis() &&
            task.status != TaskStatus.COMPLETED &&
            task.status != TaskStatus.CANCELED

    val statusColor = when {
        isCompleted -> StatusCompleted
        isPaused -> StatusPaused
        isCanceled -> StatusCanceled
        isOverdue -> StatusOverdue
        else -> StatusInProgress
    }

    val bgColors = if (isDark) taskColorsDark else taskColorsLight
    val cardBg = bgColors.getOrElse(task.colorIndex) { bgColors[0] }
    val textColor = if (isDark) Color(0xFFE4E4EC) else NoteCardText
    val subtextColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.7f) else NoteCardText.copy(alpha = 0.7f)

    // Selection mode: dim ineligible tasks
    val alpha = if (isSelectionMode && !isEligibleForSelection) 0.4f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize()
            .then(
                if (isSelectionMode && isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg.copy(alpha = alpha)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection checkbox or status indicator
            if (isSelectionMode) {
                Box(
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Outlined.CheckBox
                        else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = if (isSelected) "Izabran" else "Nije izabran",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                        else if (isEligibleForSelection) subtextColor
                        else subtextColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    IconButton(
                        onClick = onToggleStatus,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle
                            else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Status",
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isCompleted) subtextColor else textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Priority + date row
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val priorityColor = when (task.priorityScore) {
                        in 8..10 -> PriorityHigh
                        in 4..7 -> PriorityMedium
                        else -> PriorityLow
                    }
                    val priorityLabel = "Prioritet: ${task.priorityScore}"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = priorityLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (task.startDate != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = statusColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatTaskDateRange(task, dateFormat, dateTimeFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                        if (isOverdue) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Istekao",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusOverdue
                            )
                        }
                    }
                }

                // Checklist preview if task has checklist items
                if (task.checklistItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ChecklistPreview(
                        items = task.checklistItems,
                        textColor = textColor,
                        checkedColor = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50),
                        maxItems = 2
                    )
                }
            }

            // Status badge (hidden in selection mode)
            if (!isSelectionMode) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCompleted) "Zavrseno" else "U toku",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InviteCard(
    task: Task,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColors = if (isDark) taskColorsDark else taskColorsLight
    val cardBg = bgColors.getOrElse(task.colorIndex) { bgColors[0] }
    val textColor = if (isDark) Color(0xFFE4E4EC) else NoteCardText
    val subtextColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.7f) else NoteCardText.copy(alpha = 0.7f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = subtextColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Od: ${task.ownerEmail ?: task.ownerId}",
                style = MaterialTheme.typography.labelSmall,
                color = subtextColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDecline) {
                    Text("Odbij", color = textColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Prihvati")
                }
            }
        }
    }
}

private fun formatTaskDateRange(
    task: Task,
    dateFormat: SimpleDateFormat,
    dateTimeFormat: SimpleDateFormat
): String {
    val start = task.startDate
    val end = task.endDate
    if (start == null) return ""
    val formatter = if (task.hasTime) dateTimeFormat else dateFormat
    return if (end != null && end != start) {
        "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
    } else {
        formatter.format(Date(start))
    }
}

@Composable
private fun AiPromptDialog(
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Dodatne instrukcije za AI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Da li imate neke specificne zahteve za rasporedjivanje ovih taskova? (Opciono)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Npr. Rasporedi vecinu za vikend...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(prompt.takeIf { it.isNotBlank() }) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Generisi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Otkazi")
            }
        }
    )
}

/**
 * Dialog showing AI-generated schedule for preview before applying.
 */
@Composable
private fun SchedulePreviewDialog(
    results: List<ScheduleResult>,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val parseFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val selectedIds = remember { androidx.compose.runtime.mutableStateListOf<String>().apply { addAll(results.map { it.taskId }) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "AI predlog rasporeda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Pregled predlozenog rasporeda. Odznacite taskove koje ne zelite da primenite.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                results.forEach { result ->
                    val isSelected = selectedIds.contains(result.taskId)
                    val originalDate = try {
                        val parser = if (result.hasTime) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) else parseFormat
                        val formatter = if (result.hasTime) SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) else dateFormat
                        
                        val start = parser.parse(result.originalStartDateTime)?.let { formatter.format(it) } ?: result.originalStartDateTime
                        if (result.originalEndDateTime != null) {
                            val end = parser.parse(result.originalEndDateTime)?.let { formatter.format(it) } ?: result.originalEndDateTime
                            "$start - $end"
                        } else start
                    } catch (_: Exception) { result.originalStartDateTime }

                    val scheduledDate = try {
                        val parser = if (result.hasTime) SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) else parseFormat
                        val formatter = if (result.hasTime) SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) else dateFormat
                        
                        val start = parser.parse(result.scheduledStartDateTime)?.let { formatter.format(it) } ?: result.scheduledStartDateTime
                        if (result.scheduledEndDateTime != null) {
                            val end = parser.parse(result.scheduledEndDateTime)?.let { formatter.format(it) } ?: result.scheduledEndDateTime
                            "$start - $end"
                        } else start
                    } catch (_: Exception) { result.scheduledStartDateTime }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSelected) 1f else 0.5f)
                        ),
                        modifier = Modifier.clickable {
                            if (isSelected) {
                                selectedIds.remove(result.taskId)
                            } else {
                                selectedIds.add(result.taskId)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.taskName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Original date
                                    Text(
                                        text = originalDate,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textDecoration = if (isSelected) TextDecoration.LineThrough else null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    // New scheduled date
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = scheduledDate,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds) },
                shape = RoundedCornerShape(10.dp),
                enabled = selectedIds.isNotEmpty()
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Primeni")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Otkazi")
            }
        }
    )
}
