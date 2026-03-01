package com.example.projekat.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.projekat.data.model.Task
import com.example.projekat.data.model.TaskStatus
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
import com.example.projekat.ui.theme.StatusCompleted
import com.example.projekat.ui.theme.StatusInProgress
import com.example.projekat.ui.theme.StatusOverdue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val calTaskColorsLight = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)
private val calTaskColorsDark = listOf(NoteYellowDark, NoteGreenDark, NoteBlueDark, NotePinkDark, NoteOrangeDark, NotePurpleDark)

@Composable
fun CalendarScreen(
    onTaskClick: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val today = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }
    var selectedDay by remember { mutableIntStateOf(today.get(Calendar.DAY_OF_MONTH)) }

    val allTasks by viewModel.allTasksWithDeadline.collectAsState()

    // Filter tasks for selected day
    val tasksForDay = allTasks.filter { task ->
        task.deadline?.let { deadline ->
            val taskCal = Calendar.getInstance().apply { timeInMillis = deadline }
            taskCal.get(Calendar.YEAR) == currentYear &&
                    taskCal.get(Calendar.MONTH) == currentMonth &&
                    taskCal.get(Calendar.DAY_OF_MONTH) == selectedDay
        } ?: false
    }

    // Days that have tasks
    val daysWithTasks = allTasks.mapNotNull { task ->
        task.deadline?.let { deadline ->
            val taskCal = Calendar.getInstance().apply { timeInMillis = deadline }
            if (taskCal.get(Calendar.YEAR) == currentYear && taskCal.get(Calendar.MONTH) == currentMonth) {
                taskCal.get(Calendar.DAY_OF_MONTH)
            } else null
        }
    }.toSet()

    val calendarDays = getCalendarDays(currentYear, currentMonth)

    val dateLabel = remember(selectedDay, currentMonth, currentYear) {
        val fmt = SimpleDateFormat("dd. MMMM yyyy.", Locale.getDefault())
        fmt.format(
            Calendar.getInstance().apply { set(currentYear, currentMonth, selectedDay) }.time
        )
    }

    // Track swipe direction for AnimatedContent transition
    var swipeDirection by remember { mutableIntStateOf(0) } // -1 = prev, +1 = next

    // Helper lambdas for changing month
    val goToPreviousMonth = {
        swipeDirection = -1
        if (currentMonth == 0) {
            currentMonth = 11
            currentYear--
        } else {
            currentMonth--
        }
        selectedDay = 1
    }
    val goToNextMonth = {
        swipeDirection = 1
        if (currentMonth == 11) {
            currentMonth = 0
            currentYear++
        } else {
            currentMonth++
        }
        selectedDay = 1
    }

    // Single scrollable LazyColumn: calendar at top, task list below
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ---- Header ----
        item {
            Text(
                text = "Kalendar",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        // ---- Calendar card with swipe gesture ----
        item {
            // Accumulate drag distance for swipe detection
            var dragAccumulator by remember { mutableFloatStateOf(0f) }
            val swipeThreshold = 100f // pixels

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onDragEnd = {
                                if (dragAccumulator > swipeThreshold) {
                                    goToPreviousMonth()
                                } else if (dragAccumulator < -swipeThreshold) {
                                    goToNextMonth()
                                }
                                dragAccumulator = 0f
                            },
                            onDragCancel = { dragAccumulator = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount
                            }
                        )
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month navigation
                    val monthFormat = remember { SimpleDateFormat("LLLL yyyy", Locale.getDefault()) }
                    val displayDate = Calendar.getInstance().apply { set(currentYear, currentMonth, 1) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { goToPreviousMonth() }) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Prethodni mesec",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = monthFormat.format(displayDate.time)
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(onClick = { goToNextMonth() }) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Sledeci mesec",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Day of week headers
                    val dayNames = listOf("Pon", "Uto", "Sre", "Cet", "Pet", "Sub", "Ned")
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayNames.forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Animated calendar grid with slide transition on month change
                    AnimatedContent(
                        targetState = Triple(currentYear, currentMonth, calendarDays),
                        transitionSpec = {
                            if (swipeDirection >= 0) {
                                // Next month: slide in from right, slide out to left
                                slideInHorizontally { fullWidth -> fullWidth } togetherWith
                                        slideOutHorizontally { fullWidth -> -fullWidth }
                            } else {
                                // Previous month: slide in from left, slide out to right
                                slideInHorizontally { fullWidth -> -fullWidth } togetherWith
                                        slideOutHorizontally { fullWidth -> fullWidth }
                            }
                        },
                        label = "calendarTransition"
                    ) { (_, _, days) ->
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.height(((days.size / 7) * 44).dp),
                            userScrollEnabled = false
                        ) {
                            items(days) { day ->
                                val isToday = day == today.get(Calendar.DAY_OF_MONTH) &&
                                        currentMonth == today.get(Calendar.MONTH) &&
                                        currentYear == today.get(Calendar.YEAR)
                                val isSelected = day == selectedDay
                                val hasTask = day in daysWithTasks

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .then(
                                            when {
                                                isSelected -> Modifier.background(
                                                    MaterialTheme.colorScheme.primary
                                                )
                                                isToday -> Modifier.background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                )
                                                else -> Modifier
                                            }
                                        )
                                        .clickable(enabled = day > 0) {
                                            if (day > 0) selectedDay = day
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (day > 0) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    isToday -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            if (hasTask && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Divider / section label ----
        item {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Taskovi za $dateLabel",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // ---- Task list (or empty state) ----
        if (tasksForDay.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nema taskova za ovaj dan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(tasksForDay, key = { it.id }) { task ->
                CalendarTaskCard(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarTaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var isExpanded by remember { mutableStateOf(false) }
    val isCompleted = task.status == TaskStatus.COMPLETED
    val isOverdue = task.deadline != null &&
            task.deadline < System.currentTimeMillis() &&
            !isCompleted
    val statusColor = when {
        isCompleted -> StatusCompleted
        isOverdue -> StatusOverdue
        else -> StatusInProgress
    }
    // Card background color based on task's colorIndex
    val bgColors = if (isDark) calTaskColorsDark else calTaskColorsLight
    val cardBg = bgColors.getOrElse(task.colorIndex) { bgColors[0] }
    val textColor = if (isDark) Color(0xFFE4E4EC) else NoteCardText
    val subtextColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.7f) else NoteCardText.copy(alpha = 0.7f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Smanji" else "Prosiri",
                    tint = subtextColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtextColor,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Row(
                        modifier = Modifier.padding(start = 30.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "Zavrseno" else "U toku",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Otvori task",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onClick() }
                        )
                    }
                }
            }
        }
    }
}

private fun getCalendarDays(year: Int, month: Int): List<Int> {
    val cal = Calendar.getInstance().apply {
        set(year, month, 1)
    }
    // Monday = 1 ... Sunday = 7 in our grid
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Convert to Monday-first: Mon=0, Tue=1, ..., Sun=6
    val offset = when (firstDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val days = mutableListOf<Int>()
    // Empty cells before first day
    repeat(offset) { days.add(0) }
    // Actual days
    for (d in 1..daysInMonth) {
        days.add(d)
    }
    // Pad to complete last row
    while (days.size % 7 != 0) {
        days.add(0)
    }
    return days
}
