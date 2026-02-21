package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.data.model.PlayerInfo

/**
 * Tiny 1x1 Widget for minimal playback control.
 * Optimized for Small Form Factor (SFF) home screens.
 */
class TinyWidget1x1 : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(40.dp, 40.dp),
            androidx.compose.ui.unit.DpSize(70.dp, 70.dp),
            androidx.compose.ui.unit.DpSize(100.dp, 100.dp)
        )
    )
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val info = currentState<PlayerInfo>()
            Content(context, info)
        }
    }

    @Composable
    private fun Content(context: Context, info: PlayerInfo) {
        val colors = info.getWidgetColors()
        GlanceTheme {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.surface)
                    .cornerRadius(20.dp)
                    .padding(4.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                // Show album art as background if playing, otherwise just the play icon
                if (info.albumArtBitmapData != null) {
                    AlbumArtImage(
                        bitmapData = info.albumArtBitmapData,
                        size = 80.dp,
                        context = context,
                        cornerRadius = 16.dp,
                        modifier = GlanceModifier.fillMaxSize()
                    )
                    // Semi-transparent overlay for contrast
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(colors.surface)
                            .cornerRadius(16.dp)
                    ) {}
                }

                PlayPauseButton(
                    modifier = GlanceModifier.size(48.dp),
                    isPlaying = info.isPlaying,
                    backgroundColor = colors.playPauseBackground,
                    iconColor = colors.playPauseIcon,
                    cornerRadius = 12.dp,
                    iconSize = 24.dp
                )
            }
        }
    }
}

class TinyWidget1x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TinyWidget1x1()
}
