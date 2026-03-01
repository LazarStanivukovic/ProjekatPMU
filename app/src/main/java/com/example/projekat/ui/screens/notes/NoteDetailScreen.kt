package com.example.projekat.ui.screens.notes

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
import java.io.File

private val noteColorsLight = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)
private val noteColorsDark = listOf(NoteYellowDark, NoteGreenDark, NoteBlueDark, NotePinkDark, NoteOrangeDark, NotePurpleDark)

// Small swatches in the color picker always show the light pastel so they're recognizable
private val noteColorSwatches = listOf(NoteYellow, NoteGreen, NoteBlue, NotePink, NoteOrange, NotePurple)

/**
 * Creates a temporary image file in the app's cache directory and returns its content URI
 * via FileProvider. Used as the destination for the camera capture.
 */
private fun createImageUri(context: Context): Uri {
    val imagesDir = File(context.cacheDir, "images")
    imagesDir.mkdirs()
    val imageFile = File(imagesDir, "note_photo_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

/**
 * Copies the content from a picked gallery URI into the app's private cache directory
 * so the URI remains valid after the picker closes (some gallery providers revoke access).
 * Returns the new stable content URI via FileProvider.
 */
private fun copyImageToAppStorage(context: Context, sourceUri: Uri): Uri? {
    return try {
        val imagesDir = File(context.cacheDir, "images")
        imagesDir.mkdirs()
        val destFile = File(imagesDir, "note_img_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destFile
        )
    } catch (e: Exception) {
        null
    }
}

@Composable
fun NoteDetailScreen(
    noteId: String?,
    onBack: () -> Unit,
    viewModel: NoteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val bgColors = if (isDark) noteColorsDark else noteColorsLight
    val noteBackground = bgColors.getOrElse(uiState.colorIndex) { bgColors[0] }

    // Text colors that work on both light pastel and dark muted backgrounds
    val titleColor = if (isDark) Color(0xFFE4E4EC) else NoteCardText
    val contentColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.85f) else NoteCardText.copy(alpha = 0.8f)
    val hintColor = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.4f) else NoteCardText.copy(alpha = 0.35f)
    val iconTint = if (isDark) Color(0xFFE4E4EC).copy(alpha = 0.7f) else NoteCardText.copy(alpha = 0.6f)

    // ---- Camera setup ----
    // Holds the URI where the camera will write the photo
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            viewModel.addImageUri(cameraImageUri.toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createImageUri(context)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Dozvola za kameru je potrebna", Toast.LENGTH_SHORT).show()
        }
    }

    // ---- Gallery setup ----
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Copy to app storage so the URI stays valid
            val stableUri = copyImageToAppStorage(context, uri)
            if (stableUri != null) {
                viewModel.addImageUri(stableUri.toString())
            } else {
                // Fallback: use the original URI (may lose access later)
                viewModel.addImageUri(uri.toString())
            }
        }
    }

    // Navigate back after delete
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onBack()
        }
    }

    // Save on exit (back press, navigation away)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveOnExit()
        }
    }

    SwipeBackContainer(onBack = onBack) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(noteBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
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
                IconButton(onClick = { viewModel.toggleBookmark() }) {
                    Icon(
                        if (uiState.isBookmarked) Icons.Default.Bookmark
                        else Icons.Default.BookmarkBorder,
                        contentDescription = "Obelezi",
                        tint = if (uiState.isBookmarked) {
                            if (isDark) Color(0xFFFFD54F) else Color(0xFFFF8F00)
                        } else iconTint
                    )
                }
                if (!uiState.isNew) {
                    IconButton(onClick = { viewModel.deleteNote() }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Obrisi",
                            tint = iconTint
                        )
                    }
                }
            }

            // Scrollable content area (title + image + content)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                // Title field — clean, no border, Google Keep style
                TextField(
                    value = uiState.title,
                    onValueChange = { viewModel.updateTitle(it) },
                    placeholder = {
                        Text(
                            "Naslov",
                            style = TextStyle(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal
                            ),
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

                // Image gallery (horizontally scrollable, Google Keep style)
                if (uiState.imageUris.isNotEmpty()) {
                    if (uiState.imageUris.size == 1) {
                        // Single image — show full-width like before
                        val imageUri = uiState.imageUris[0]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(Uri.parse(imageUri))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Slika beleske",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                            )

                            // Remove-image button (top-end corner)
                            IconButton(
                                onClick = { viewModel.removeImageUri(imageUri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .background(
                                        Color.Black.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Ukloni sliku",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        // Multiple images — horizontal scrollable row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            uiState.imageUris.forEach { imageUri ->
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(Uri.parse(imageUri))
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Slika beleske",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                    )

                                    // Remove-image button
                                    IconButton(
                                        onClick = { viewModel.removeImageUri(imageUri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(28.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Ukloni sliku",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Content field
                TextField(
                    value = uiState.content,
                    onValueChange = { viewModel.updateContent(it) },
                    placeholder = {
                        Text(
                            "Beleska...",
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
            }

            // Bottom bar
            NoteBottomBar(
                uiState = uiState,
                isDark = isDark,
                iconTint = iconTint,
                noteBackground = noteBackground,
                onToggleColorPicker = { viewModel.toggleColorPicker() },
                onColorSelected = { viewModel.updateColorIndex(it) },
                onCameraClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onGalleryClick = {
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }
    } // SwipeBackContainer
}

@Composable
private fun NoteBottomBar(
    uiState: NoteDetailUiState,
    isDark: Boolean,
    iconTint: Color,
    noteBackground: Color,
    onToggleColorPicker: () -> Unit,
    onColorSelected: (Int) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
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
                                .clickable { onColorSelected(index) },
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

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(noteBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleColorPicker) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "Izaberi boju",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onCameraClick) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Kamera",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onGalleryClick) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Galerija",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

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
