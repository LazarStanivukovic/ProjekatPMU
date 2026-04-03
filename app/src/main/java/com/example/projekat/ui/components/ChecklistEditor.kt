package com.example.projekat.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.projekat.data.model.ChecklistItem

/**
 * Reusable checklist editor component for notes and tasks.
 * Supports adding, editing, toggling, and deleting checklist items.
 */
@Composable
fun ChecklistEditor(
    items: List<ChecklistItem>,
    onItemToggle: (String) -> Unit,
    onItemTextChange: (String, String) -> Unit,
    onItemDelete: (String) -> Unit,
    onItemAdd: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    hintColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    iconTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    showAddButton: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Checklist header with add button
        if (showAddButton || items.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lista stavki",
                    style = MaterialTheme.typography.labelMedium,
                    color = iconTint
                )
                Spacer(modifier = Modifier.weight(1f))
                if (showAddButton) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onItemAdd)
                            .background(
                                checkedColor.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .padding(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Dodaj stavku",
                            tint = checkedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Checklist items
        items.sortedBy { it.order }.forEach { item ->
            ChecklistItemRow(
                item = item,
                onToggle = { onItemToggle(item.id) },
                onTextChange = { newText -> onItemTextChange(item.id, newText) },
                onDelete = { onItemDelete(item.id) },
                textColor = textColor,
                hintColor = hintColor,
                iconTint = iconTint,
                checkedColor = checkedColor
            )
        }

        // Empty state hint
        if (items.isEmpty() && showAddButton) {
            Text(
                text = "Dodaj stavke klikom na +",
                style = MaterialTheme.typography.bodySmall,
                color = hintColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    textColor: Color,
    hintColor: Color,
    iconTint: Color,
    checkedColor: Color
) {
    var localText by remember(item.id) { mutableStateOf(item.text) }
    val focusRequester = remember { FocusRequester() }

    // Focus new empty items automatically
    LaunchedEffect(item.id) {
        if (item.text.isEmpty()) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus request fails
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (item.isChecked) Icons.Default.CheckBox 
                              else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (item.isChecked) "Odcekiraj" else "Cekiraj",
                tint = if (item.isChecked) checkedColor else iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Text field
        BasicTextField(
            value = localText,
            onValueChange = { newText ->
                localText = newText
                onTextChange(newText)
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = if (item.isChecked) textColor.copy(alpha = 0.5f) else textColor,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { /* Hide keyboard */ }),
            singleLine = true,
            decorationBox = { innerTextField ->
                Box {
                    if (localText.isEmpty()) {
                        Text(
                            text = "Stavka...",
                            style = TextStyle(fontSize = 15.sp),
                            color = hintColor
                        )
                    }
                    innerTextField()
                }
            }
        )

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Obrisi stavku",
                tint = iconTint.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Compact checklist preview for note/task cards.
 * Shows only the first few items with progress indicator.
 */
@Composable
fun ChecklistPreview(
    items: List<ChecklistItem>,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    checkedColor: Color = MaterialTheme.colorScheme.primary,
    maxItems: Int = 3
) {
    if (items.isEmpty()) return

    val checkedCount = items.count { it.isChecked }
    val totalCount = items.size

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Progress text
        Text(
            text = "$checkedCount / $totalCount",
            style = MaterialTheme.typography.labelSmall,
            color = if (checkedCount == totalCount) checkedColor else textColor.copy(alpha = 0.6f)
        )

        // Show first few items
        items.take(maxItems).forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                Icon(
                    imageVector = if (item.isChecked) Icons.Default.CheckBox 
                                  else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (item.isChecked) checkedColor else textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.text.ifEmpty { "..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isChecked) textColor.copy(alpha = 0.5f) else textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }

        // "and X more" hint
        if (items.size > maxItems) {
            Text(
                text = "...i jos ${items.size - maxItems}",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f)
            )
        }
    }
}
