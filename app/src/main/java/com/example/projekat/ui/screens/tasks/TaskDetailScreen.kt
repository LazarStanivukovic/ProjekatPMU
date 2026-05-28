package com.example.projekat.ui.screens.tasks

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.RepeatInterval
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.ui.components.ChecklistEditor
import com.example.projekat.ui.components.LocationData
import com.example.projekat.ui.components.LocationPicker
import com.example.projekat.ui.components.SwipeBackContainer
import com.example.projekat.ui.components.UndoDialog
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
import com.example.projekat.ui.theme.StatusCanceled
import com.example.projekat.ui.theme.StatusCompleted
import com.example.projekat.ui.theme.StatusInProgress
import com.example.projekat.ui.theme.StatusPaused
import com.example.projekat.util.ShakeDetector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val noteColorsLight = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)
private val noteColorsDark = listOf(NoteYellowDark, NoteGreenDark, NoteBlueDark, NotePinkDark, NoteOrangeDark, NotePurpleDark)

// Small swatches in the color picker always show the light pastel so they're recognizable
private val noteColorSwatches = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)

private enum class DatePickerTarget {
    START,
    END,
    REPEAT_END
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String?,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit = {},
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    var activeDatePicker by remember { mutableStateOf<DatePickerTarget?>(null) }
    var showNoteSelector by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd. MMMM yyyy.", Locale.getDefault()) }
    val dateTimeFormat = remember { SimpleDateFormat("dd. MMMM yyyy. HH:mm", Locale.getDefault()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<DatePickerTarget?>(null) }
    var showRepeatDropdown by remember { mutableStateOf(false) }
    var timeHours by remember { mutableStateOf(9) }
    var timeMinutes by remember { mutableStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareEmail by remember { mutableStateOf("") }

    // Color from the task's colorIndex
    val bgColors = if (isDark) noteColorsDark else noteColorsLight
    val screenBg = bgColors.getOrElse(uiState.colorIndex) { bgColors[0] }

    // Text colors that work on both light pastel and dark muted backgrounds
    val titleColor = if (isDark) Color(0xFFE4E4EC) else NoteCardText
    val contentColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.85f) else NoteCardText.copy(alpha = 0.8f)
    val hintColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.4f) else NoteCardText.copy(alpha = 0.35f)
    val iconTint = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.7f) else NoteCardText.copy(alpha = 0.6f)
    val chipBg = if (isDark) Color(0xFF2A2A3A).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)

    // ---- Shake to Undo setup ----
    val shakeDetector = remember {
        ShakeDetector(
            context = context,
            onShake = { viewModel.showUndoDialog() }
        )
    }

    // Refresh attached note when the screen becomes visible again
    // (e.g. after editing/deleting images in the note detail screen)
    // Also start/stop shake detection based on lifecycle
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshAttachedNote()
                    shakeDetector.start()
                }
                Lifecycle.Event.ON_PAUSE -> shakeDetector.stop()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            shakeDetector.stop()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Navigate back after save/delete
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    // Save on exit
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveOnExit()
        }
    }

    // Date Picker Dialog
    if (activeDatePicker != null) {
        val tomorrowMillis = remember {
            Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = System.currentTimeMillis()
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val initialDate = when (activeDatePicker) {
            DatePickerTarget.START -> uiState.startDate
            DatePickerTarget.END -> uiState.endDate
            DatePickerTarget.REPEAT_END -> uiState.repeatEndDate
            else -> null
        }?.takeIf { it >= tomorrowMillis } ?: tomorrowMillis

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= tomorrowMillis
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                    return year >= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { activeDatePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        val target = activeDatePicker
                        when (target) {
                            DatePickerTarget.START -> {
                                val adjusted = applyTimeIfNeeded(selectedMillis, uiState.hasTime, timeHours, timeMinutes)
                                viewModel.updateStartDate(adjusted)
                                if (uiState.hasTime) {
                                    val localCal = Calendar.getInstance().apply { timeInMillis = adjusted }
                                    timeHours = localCal.get(Calendar.HOUR_OF_DAY)
                                    timeMinutes = localCal.get(Calendar.MINUTE)
                                    timePickerTarget = DatePickerTarget.START
                                    showTimePicker = true
                                }
                            }
                            DatePickerTarget.END -> {
                                val adjusted = applyTimeIfNeeded(selectedMillis, uiState.hasTime, timeHours, timeMinutes)
                                viewModel.updateEndDate(adjusted)
                                if (uiState.hasTime) {
                                    val localCal = Calendar.getInstance().apply { timeInMillis = adjusted }
                                    timeHours = localCal.get(Calendar.HOUR_OF_DAY)
                                    timeMinutes = localCal.get(Calendar.MINUTE)
                                    timePickerTarget = DatePickerTarget.END
                                    showTimePicker = true
                                }
                            }
                            DatePickerTarget.REPEAT_END -> {
                                viewModel.updateRepeatEndDate(applyTimeIfNeeded(selectedMillis, false, 0, 0))
                            }
                            null -> Unit
                        }
                    }
                    activeDatePicker = null
                }) {
                    Text("Potvrdi")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePicker = null }) {
                    Text("Otkazi")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            hours = timeHours,
            minutes = timeMinutes,
            onHoursChange = { timeHours = it },
            onMinutesChange = { timeMinutes = it },
            onConfirm = {
                when (timePickerTarget) {
                    DatePickerTarget.START -> {
                        val startDate = uiState.startDate
                        if (startDate != null) {
                            viewModel.updateStartDate(applyTimeIfNeeded(startDate, true, timeHours, timeMinutes))
                        }
                    }
                    DatePickerTarget.END -> {
                        val endDate = uiState.endDate
                        if (endDate != null) {
                            viewModel.updateEndDate(applyTimeIfNeeded(endDate, true, timeHours, timeMinutes))
                        }
                    }
                    else -> Unit
                }
                showTimePicker = false
                timePickerTarget = null
            },
            onDismiss = { 
                showTimePicker = false
                timePickerTarget = null
            }
        )
    }

    if (showShareDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showShareDialog = false
                shareEmail = ""
            },
            title = { Text("Podeli task") },
            text = {
                Column {
                    Text("Unesite email korisnika sa kojim želite da podelite ovaj task.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = shareEmail,
                        onValueChange = { shareEmail = it },
                        label = { Text("Email adresa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    
                    if (uiState.sharedWith.isNotEmpty() || uiState.pendingInvites.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Trenutno podeljeno sa:", style = MaterialTheme.typography.labelMedium)
                        uiState.sharedWith.forEach { email ->
                            Text(email, style = MaterialTheme.typography.bodySmall)
                        }
                        uiState.pendingInvites.forEach { email ->
                            Text("$email (Na čekanju)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.shareTask(shareEmail)
                        showShareDialog = false
                        shareEmail = ""
                    }
                ) { Text("Pošalji") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        shareEmail = ""
                    }
                ) { Text("Otkaži") }
            }
        )
    }

    SwipeBackContainer(onBack = onBack) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ---- Top bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Nazad",
                        tint = iconTint
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!uiState.isNew) {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Deli",
                            tint = iconTint
                        )
                    }
                    IconButton(onClick = { viewModel.deleteTask() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Obrisi",
                            tint = iconTint
                        )
                    }
                }
            }

            // ---- Scrollable content ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                // Title — borderless, Google Keep style
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = {
                        Text(
                            "Naslov taska",
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Normal),
                            color = hintColor
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = titleColor
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = titleColor
                    )
                )

                // Description — borderless
                TextField(
                    value = uiState.description,
                    onValueChange = { viewModel.updateDescription(it) },
                    placeholder = {
                        Text(
                            "Opis taska...",
                            style = TextStyle(fontSize = 16.sp),
                            color = hintColor
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = contentColor,
                        lineHeight = 24.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = contentColor
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Status chips ----
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(
                        label = "U toku",
                        selected = uiState.status == TaskStatus.IN_PROGRESS,
                        color = StatusInProgress,
                        chipBg = chipBg,
                        onClick = { viewModel.updateStatus(TaskStatus.IN_PROGRESS) }
                    )
                    StatusChip(
                        label = "Pauzirano",
                        selected = uiState.status == TaskStatus.PAUSED,
                        color = StatusPaused,
                        chipBg = chipBg,
                        onClick = { viewModel.updateStatus(TaskStatus.PAUSED) }
                    )
                    StatusChip(
                        label = "Zavrseno",
                        selected = uiState.status == TaskStatus.COMPLETED,
                        color = StatusCompleted,
                        chipBg = chipBg,
                        onClick = { viewModel.updateStatus(TaskStatus.COMPLETED) }
                    )
                    StatusChip(
                        label = "Otkazano",
                        selected = uiState.status == TaskStatus.CANCELED,
                        color = StatusCanceled,
                        chipBg = chipBg,
                        onClick = { viewModel.updateStatus(TaskStatus.CANCELED) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Priority Slider ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prioritet: ${uiState.priorityScore}",
                        style = MaterialTheme.typography.labelMedium,
                        color = iconTint
                    )
                }
                Slider(
                    value = uiState.priorityScore.toFloat(),
                    onValueChange = { viewModel.updatePriority(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8, // 10 - 1 - 1 = 8 steps (values 2 through 9)
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Date section ----
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = chipBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (uiState.startDate != null) MaterialTheme.colorScheme.primary else iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Datum",
                                style = MaterialTheme.typography.labelMedium,
                                color = iconTint,
                                modifier = Modifier.weight(1f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Vreme",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = iconTint
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = uiState.hasTime,
                                    onCheckedChange = { checked ->
                                        viewModel.updateHasTime(checked)
                                        if (checked && uiState.startDate != null) {
                                            val baseDate = uiState.startDate ?: System.currentTimeMillis()
                                            val localCal = Calendar.getInstance().apply { timeInMillis = baseDate }
                                            timeHours = localCal.get(Calendar.HOUR_OF_DAY)
                                            timeMinutes = localCal.get(Calendar.MINUTE)
                                            timePickerTarget = DatePickerTarget.START
                                            showTimePicker = true
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        DateRow(
                            label = "Od",
                            value = formatDateTime(uiState.startDate, uiState.hasTime, dateFormat, dateTimeFormat),
                            hint = "Izaberi datum",
                            iconTint = iconTint,
                            titleColor = titleColor,
                            hintColor = hintColor,
                            onClick = { activeDatePicker = DatePickerTarget.START },
                            onClear = if (uiState.startDate != null) {
                                {
                                    viewModel.updateStartDate(null)
                                    viewModel.updateEndDate(null)
                                }
                            } else null
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        DateRow(
                            label = "Do",
                            value = formatDateTime(uiState.endDate, uiState.hasTime, dateFormat, dateTimeFormat),
                            hint = "Opcioni kraj",
                            iconTint = iconTint,
                            titleColor = titleColor,
                            hintColor = hintColor,
                            enabled = uiState.startDate != null,
                            onClick = {
                                if (uiState.startDate == null) {
                                    activeDatePicker = DatePickerTarget.START
                                } else {
                                    activeDatePicker = DatePickerTarget.END
                                }
                            },
                            onClear = if (uiState.endDate != null) { { viewModel.updateEndDate(null) } } else null
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Repeat setting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (uiState.repeatInterval != RepeatInterval.NONE) MaterialTheme.colorScheme.primary else iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Ponavljanje",
                                style = MaterialTheme.typography.labelMedium,
                                color = iconTint,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showRepeatDropdown = true }
                                ) {
                                    Text(
                                        text = when (uiState.repeatInterval) {
                                            RepeatInterval.NONE -> "Nikad"
                                            RepeatInterval.DAILY -> "Dnevno"
                                            RepeatInterval.WEEKLY -> "Nedeljno"
                                            RepeatInterval.MONTHLY -> "Mesečno"
                                            RepeatInterval.YEARLY -> "Godišnje"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = titleColor
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = iconTint
                                    )
                                }
                                DropdownMenu(
                                    expanded = showRepeatDropdown,
                                    onDismissRequest = { showRepeatDropdown = false }
                                ) {
                                    DropdownMenuItem(text = { Text("Nikad") }, onClick = { viewModel.updateRepeatInterval(RepeatInterval.NONE); showRepeatDropdown = false })
                                    DropdownMenuItem(text = { Text("Dnevno") }, onClick = { viewModel.updateRepeatInterval(RepeatInterval.DAILY); showRepeatDropdown = false })
                                    DropdownMenuItem(text = { Text("Nedeljno") }, onClick = { viewModel.updateRepeatInterval(RepeatInterval.WEEKLY); showRepeatDropdown = false })
                                    DropdownMenuItem(text = { Text("Mesečno") }, onClick = { viewModel.updateRepeatInterval(RepeatInterval.MONTHLY); showRepeatDropdown = false })
                                    DropdownMenuItem(text = { Text("Godišnje") }, onClick = { viewModel.updateRepeatInterval(RepeatInterval.YEARLY); showRepeatDropdown = false })
                                }
                            }
                        }

                        if (uiState.repeatInterval != RepeatInterval.NONE) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DateRow(
                                label = "Do",
                                value = formatDateTime(uiState.repeatEndDate, uiState.hasTime, dateFormat, dateTimeFormat),
                                hint = "Kraj ponavljanja (opciono)",
                                iconTint = iconTint,
                                titleColor = titleColor,
                                hintColor = hintColor,
                                enabled = true,
                                onClick = { activeDatePicker = DatePickerTarget.REPEAT_END },
                                onClear = if (uiState.repeatEndDate != null) { { viewModel.updateRepeatEndDate(null) } } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ---- Attached note: inline preview + clickable, or add button ----
                if (uiState.attachedNote != null) {
                    AttachedNotePreview(
                        note = uiState.attachedNote!!,
                        isDark = isDark,
                        titleColor = titleColor,
                        iconTint = iconTint,
                        onClick = { onNoteClick(uiState.attachedNote!!.id) },
                        onRemove = { viewModel.attachNote(null) }
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showNoteSelector = !showNoteSelector },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = chipBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Default.NoteAdd,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Dodaj belesku...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = hintColor
                            )
                        }
                    }
                }

                // Note selector dropdown
                AnimatedVisibility(visible = showNoteSelector && uiState.availableNotes.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF2A2A3A) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            uiState.availableNotes.forEach { note ->
                                Text(
                                    text = note.title.ifBlank { "(Bez naslova)" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = titleColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.attachNote(note)
                                            showNoteSelector = false
                                        }
                                        .padding(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Location section ----
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Lokacija za obavestenje",
                    style = MaterialTheme.typography.labelMedium,
                    color = iconTint,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LocationPicker(
                    currentLocation = if (uiState.locationLat != null && uiState.locationLng != null) {
                        LocationData(
                            lat = uiState.locationLat!!,
                            lng = uiState.locationLng!!,
                            name = uiState.locationName ?: "",
                            radius = uiState.locationRadius
                        )
                    } else null,
                    onLocationSelected = { viewModel.updateLocation(it) },
                    titleColor = titleColor,
                    hintColor = hintColor,
                    iconTint = iconTint,
                    chipBg = chipBg
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Checklist section
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                )
                ChecklistEditor(
                    items = uiState.checklistItems,
                    onItemToggle = { viewModel.toggleChecklistItem(it) },
                    onItemTextChange = { id, text -> viewModel.updateChecklistItemText(id, text) },
                    onItemDelete = { viewModel.deleteChecklistItem(it) },
                    onItemAdd = { viewModel.addChecklistItem() },
                    textColor = contentColor,
                    hintColor = hintColor,
                    iconTint = iconTint,
                    checkedColor = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ---- Bottom bar with color picker ----
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Color picker panel (animated slide up)
                AnimatedVisibility(
                    visible = uiState.showColorPicker,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) Color(0xFF2A2A3A) else Color(0xFFF5F5F5)
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "Boja",
                            style = MaterialTheme.typography.labelMedium,
                            color = iconTint,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            noteColorSwatches.forEachIndexed { index, color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .then(
                                            if (uiState.colorIndex == index)
                                                Modifier.border(
                                                    2.5.dp,
                                                    if (isDark) Color.White else Color(0xFF3F51B5),
                                                    CircleShape
                                                )
                                            else Modifier.border(
                                                1.dp,
                                                Color.Black.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                        )
                                        .clickable { viewModel.updateColorIndex(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.colorIndex == index) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = NoteCardText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(screenBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.toggleColorPicker() }) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Izaberi boju",
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = if (uiState.status == TaskStatus.COMPLETED) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (uiState.status == TaskStatus.COMPLETED) StatusCompleted else StatusInProgress,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.status == TaskStatus.COMPLETED) "Zavrseno" else "U toku",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.status == TaskStatus.COMPLETED) StatusCompleted else StatusInProgress
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Izmene se cuvaju automatski",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }

    // Undo confirmation dialog (triggered by shake)
    if (uiState.showUndoDialog) {
        UndoDialog(
            onConfirm = { viewModel.revertToLastSaved() },
            onDismiss = { viewModel.dismissUndoDialog() }
        )
    }
    } // SwipeBackContainer
}

@Composable
private fun DateRow(
    label: String,
    value: String?,
    hint: String,
    iconTint: Color,
    titleColor: Color,
    hintColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    val rowAlpha = if (enabled) 1f else 0.5f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = iconTint.copy(alpha = rowAlpha),
            modifier = Modifier.width(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value ?: hint,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null) titleColor.copy(alpha = rowAlpha) else hintColor.copy(alpha = rowAlpha),
            modifier = Modifier.weight(1f)
        )
        if (onClear != null && enabled) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Ukloni datum",
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TimePickerDialog(
    hours: Int,
    minutes: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Potvrdi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Otkazi") }
        },
        title = { Text("Vreme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TimePickerRow(
                    label = "Sati",
                    value = hours,
                    onValueChange = onHoursChange,
                    range = 0..23
                )
                TimePickerRow(
                    label = "Min",
                    value = minutes,
                    onValueChange = onMinutesChange,
                    range = 0..59
                )
            }
        }
    )
}

@Composable
private fun TimePickerRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        TextField(
            value = value.toString().padStart(2, '0'),
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() }.take(2)
                val parsed = filtered.toIntOrNull()
                if (parsed != null && parsed in range) {
                    onValueChange(parsed)
                }
            },
            modifier = Modifier.width(90.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        TimeAdjuster(
            onMinus = {
                val newValue = if (value - 1 < range.first) range.last else value - 1
                onValueChange(newValue)
            },
            onPlus = {
                val newValue = if (value + 1 > range.last) range.first else value + 1
                onValueChange(newValue)
            }
        )
    }
}

@Composable
private fun TimeAdjuster(
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "Umanji")
        }
        IconButton(onClick = onPlus, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Add, contentDescription = "Uvecaj")
        }
    }
}

private fun applyTimeIfNeeded(
    baseUtcMillis: Long,
    hasTime: Boolean,
    hours: Int,
    minutes: Int
): Long {
    if (!hasTime) return baseUtcMillis
    val utcCal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = baseUtcMillis
    }
    val year = utcCal.get(Calendar.YEAR)
    val month = utcCal.get(Calendar.MONTH)
    val day = utcCal.get(Calendar.DAY_OF_MONTH)
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hours)
        set(Calendar.MINUTE, minutes)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

private fun formatDateTime(
    millis: Long?,
    hasTime: Boolean,
    dateFormat: SimpleDateFormat,
    dateTimeFormat: SimpleDateFormat
): String? {
    if (millis == null) return null
    return if (hasTime) dateTimeFormat.format(Date(millis)) else dateFormat.format(Date(millis))
}

/**
 * Inline preview of the attached note — shows the note's color, title, content snippet,
 * and image thumbnail. Tapping opens the full note; the X button detaches it.
 */
@Composable
private fun AttachedNotePreview(
    note: Note,
    isDark: Boolean,
    titleColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val bgColors = if (isDark) noteColorsDark else noteColorsLight
    val noteBg = bgColors.getOrElse(note.colorIndex) { bgColors[0] }
    val noteTextColor = if (isDark) Color(0xFFE4E4EC) else NoteCardText

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = noteBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Image thumbnail if note has images
            if (note.imageUris.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(note.imageUris[0]))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Slika beleske",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Header row: label + remove button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Default.NoteAdd,
                        contentDescription = null,
                        tint = noteTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Povezana beleska",
                        style = MaterialTheme.typography.labelSmall,
                        color = noteTextColor.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Ukloni belesku",
                            tint = noteTextColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Note title
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = noteTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Note content snippet
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = noteTextColor.copy(alpha = 0.7f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // "Open note" hint
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Otvori belešku",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    selected: Boolean,
    color: Color,
    chipBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                color = if (selected) color.copy(alpha = 0.15f) else chipBg,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) color else color.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) color else color.copy(alpha = 0.7f)
            )
        }
    }
}


