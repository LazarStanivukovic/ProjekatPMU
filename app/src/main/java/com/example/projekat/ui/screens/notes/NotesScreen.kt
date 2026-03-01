package com.example.projekat.ui.screens.notes

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.projekat.data.model.Note
import com.example.projekat.ui.theme.BookmarkColor
import com.example.projekat.ui.theme.NoteCardText
import com.example.projekat.ui.theme.NoteBlue
import com.example.projekat.ui.theme.NoteBlueDark
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val noteColors = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)
private val noteColorsDark = listOf(NoteYellowDark, NoteGreenDark, NoteBlueDark, NotePinkDark, NoteOrangeDark, NotePurpleDark)

enum class NoteFilter {
    ALL, BOOKMARKED, DELETED
}

@Composable
fun NotesScreen(
    onNoteClick: (String) -> Unit,
    onCreateNote: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val drawerTitle = when (currentFilter) {
        NoteFilter.ALL -> "Beleske"
        NoteFilter.BOOKMARKED -> "Obelezene"
        NoteFilter.DELETED -> "Obrisane"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NoteApp",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Outlined.StickyNote2, contentDescription = null) },
                    label = { Text("Sve beleske") },
                    selected = currentFilter == NoteFilter.ALL,
                    onClick = {
                        viewModel.setFilter(NoteFilter.ALL)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                    label = { Text("Obelezene") },
                    selected = currentFilter == NoteFilter.BOOKMARKED,
                    onClick = {
                        viewModel.setFilter(NoteFilter.BOOKMARKED)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    label = { Text("Obrisane") },
                    badge = { Text("7 dana") },
                    selected = currentFilter == NoteFilter.DELETED,
                    onClick = {
                        viewModel.setFilter(NoteFilter.DELETED)
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar with menu and search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Meni",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    // Custom search field instead of SearchBar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Pretrazi beleske...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                // Title
                Text(
                    text = drawerTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                if (notes.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.StickyNote2,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Nema beleski",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Staggered grid of notes
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                showRestore = currentFilter == NoteFilter.DELETED,
                                onRestore = { viewModel.restoreNote(note) }
                            )
                        }
                    }
                }
            }

            // FAB
            FloatingActionButton(
                onClick = onCreateNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova beleska")
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    showRestore: Boolean = false,
    onRestore: () -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) noteColorsDark else noteColors
    val bgColor = colors.getOrElse(note.colorIndex) { colors[0] }
    val textColor = if (isDark) androidx.compose.ui.graphics.Color(0xFFE4E4EC) else NoteCardText
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Image thumbnail at top of card (if image is attached)
            if (!note.imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(note.imageUri))
                        .crossfade(true)
                        .build(),
                    contentDescription = "Slika beleske",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isBookmarked) {
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = "Obelezeno",
                            modifier = Modifier.size(18.dp),
                            tint = BookmarkColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(note.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.4f)
                    )

                    if (showRestore) {
                        IconButton(
                            onClick = onRestore,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.RestoreFromTrash,
                                contentDescription = "Vrati belesku",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
