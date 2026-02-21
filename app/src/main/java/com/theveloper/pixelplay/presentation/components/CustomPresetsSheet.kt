package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theveloper.pixelplay.data.equalizer.EqualizerPreset
import com.theveloper.pixelplay.ui.theme.GoogleSansRounded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPresetsSheet(
    presets: List<EqualizerPreset>,
    pinnedPresetsNames: List<String>,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onPinToggled: (EqualizerPreset) -> Unit,
    onRename: (EqualizerPreset) -> Unit,
    onDelete: (EqualizerPreset) -> Unit,
    onExport: (EqualizerPreset) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val filteredPresets = remember(presets, searchQuery) {
        if (searchQuery.isBlank()) presets
        else presets.filter { it.displayName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Presets",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onImport) {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = "Import Preset (.pxp)",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (presets.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    placeholder = { Text("Search presets...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            if (filteredPresets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (presets.isEmpty()) "No custom presets saved yet." else "No matches found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    items(filteredPresets, key = { it.name }) { preset ->
                        CustomPresetItem(
                            preset = preset,
                            isPinned = pinnedPresetsNames.contains(preset.name),
                            onClick = {
                                onPresetSelected(preset)
                                onDismiss()
                            },
                            onPinClick = { onPinToggled(preset) },
                            onRenameClick = { onRename(preset) },
                            onDeleteClick = { onDelete(preset) },
                            onExportClick = { onExport(preset) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CustomPresetItem(
    preset: EqualizerPreset,
    isPinned: Boolean,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.displayName.firstOrNull()?.toString()?.uppercase() ?: "C",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontFamily = GoogleSansRounded
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Row {
             IconButton(onClick = onPinClick) {
                Icon(
                    imageVector = if (isPinned) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isPinned) "Unpin" else "Pin",
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRenameClick) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Rename",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onExportClick) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Export",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
