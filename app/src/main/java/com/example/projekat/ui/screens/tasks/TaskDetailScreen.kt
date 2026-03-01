package com.example.projekat.ui.screens.tasks

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projekat.data.model.Note
import com.example.projekat.data.model.TaskStatus
import com.example.projekat.ui.components.SwipeBackContainer
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val noteColorsLight = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)
private val noteColorsDark = listOf(NoteYellowDark, NoteGreenDark, NoteBlueDark, NotePinkDark, NoteOrangeDark, NotePurpleDark)

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
    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteSelector by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd. MMMM yyyy.", Locale.getDefault()) }

    // Theme-aware surface color for the full-screen background
    val screenBg = if (isDark) Color(0xFF1E1E2A) else Color(0xFFF8F9FD)
    val titleColor = if (isDark) Color(0xFFE4E4EC) else Color(0xFF1B1B22)
    val contentColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.85f) else Color(0xFF1B1B22).copy(alpha = 0.8f)
    val hintColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.4f) else Color(0xFF1B1B22).copy(alpha = 0.35f)
    val iconTint = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.7f) else Color(0xFF1B1B22).copy(alpha = 0.6f)
    val chipBg = if (isDark) Color(0xFF2A2A3A) else Color(0xFFEEEFF5)

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
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.deadline ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDeadline(it) }
                    showDatePicker = false
                }) {
                    Text("Potvrdi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Otkazi")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatusChip(
                        label = "U toku",
                        selected = uiState.status == TaskStatus.IN_PROGRESS,
                        color = StatusInProgress,
                        chipBg = chipBg,
                        onClick = { viewModel.updateStatus(TaskStatus.IN_PROGRESS) }
                    )
                    StatusChip(
                        label = "Zavrseno",
                        selected = uiState.status == TaskStatus.COMPLETED,
                        color = StatusCompleted,
                        chipBg = chipBg,
                        onClick = { viewModel.updateStatus(TaskStatus.COMPLETED) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Deadline card ----
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showDatePicker = true },
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
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (uiState.deadline != null) MaterialTheme.colorScheme.primary else iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uiState.deadline != null) dateFormat.format(Date(uiState.deadline!!))
                            else "Dodaj krajnji rok...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (uiState.deadline != null) titleColor else hintColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.deadline != null) {
                            IconButton(
                                onClick = { viewModel.updateDeadline(null) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Ukloni rok",
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
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

                Spacer(modifier = Modifier.height(32.dp))
            }

            // ---- Bottom bar ----
            HorizontalDivider(
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(screenBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
    } // SwipeBackContainer
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
