package com.theveloper.pixelplay.presentation.components.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theveloper.pixelplay.presentation.components.WavySliderExpressive
import com.theveloper.pixelplay.presentation.viewmodel.PlayerViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLabBottomSheet(
    onDismiss: () -> Unit,
    playerViewModel: PlayerViewModel
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val preAmp by playerViewModel.preAmpFactor.collectAsState()
    val speed by playerViewModel.playbackSpeed.collectAsState()
    val pitch by playerViewModel.playbackPitch.collectAsState()
    val nightMode by playerViewModel.nightModeEnabled.collectAsState()
    val bitPerfect by playerViewModel.bitPerfectEnabled.collectAsState()
    val aiTracking by playerViewModel.aiTrackingEnabled.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Audio Lab",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Digital Gain (Pre-Amp)
            AudioLabSlider(
                label = "Digital Gain (Pre-Amp)",
                value = preAmp,
                onValueChange = playerViewModel::setPreAmpFactor,
                valueRange = 0.5f..2.0f,
                icon = Icons.Rounded.Add,
                displayValue = "${(preAmp * 100).roundToInt()}%"
            )

            // Playback Speed
            AudioLabSlider(
                label = "Playback Speed",
                value = speed,
                onValueChange = playerViewModel::setPlaybackSpeed,
                valueRange = 0.5f..2.0f,
                icon = Icons.Rounded.Speed,
                displayValue = "${String.format("%.2f", speed)}x"
            )

            // Playback Pitch
            AudioLabSlider(
                label = "Playback Pitch",
                value = pitch,
                onValueChange = playerViewModel::setPlaybackPitch,
                valueRange = 0.5f..2.0f,
                icon = Icons.Rounded.MusicNote,
                displayValue = "${String.format("%.2f", pitch)}x"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Toggles Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AudioLabToggle(
                    title = "Night Mode",
                    subtitle = "Normalize volume for comfortable late-night listening.",
                    checked = nightMode,
                    onCheckedChange = playerViewModel::setNightModeEnabled,
                    icon = Icons.Rounded.NightsStay
                )

                AudioLabToggle(
                    title = "Bit-Perfect (USB)",
                    subtitle = "Bypass Android mixer for external DACs.",
                    checked = bitPerfect,
                    onCheckedChange = playerViewModel::setBitPerfectEnabled,
                    icon = Icons.Rounded.Usb
                )

                AudioLabToggle(
                    title = "AI Learning",
                    subtitle = "Learn your listening habits to optimize Discovery Mix.",
                    checked = aiTracking,
                    onCheckedChange = playerViewModel::setAiTrackingEnabled,
                    icon = Icons.Rounded.Psychology
                )
            }
        }
    }
}

@Composable
private fun AudioLabSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    icon: ImageVector,
    displayValue: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                displayValue,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        WavySliderExpressive(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            thumbColor = MaterialTheme.colorScheme.primary,
            isPlaying = true
        )
    }
}

@Composable
private fun AudioLabToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
