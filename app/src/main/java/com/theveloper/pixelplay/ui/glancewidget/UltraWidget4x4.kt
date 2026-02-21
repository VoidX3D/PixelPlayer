package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.Player
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.PlayerInfo

/**
 * Ultra 4x4 Widget for full control, queue overview, and adaptive layouts.
 * Perfect for larger phones and tablets.
 */
class UltraWidget4x4 : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(250.dp, 250.dp),
            androidx.compose.ui.unit.DpSize(300.dp, 300.dp),
            androidx.compose.ui.unit.DpSize(350.dp, 350.dp)
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
                    .cornerRadius(32.dp)
                    .padding(20.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Header: Full Song Detail
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(
                            bitmapData = info.albumArtBitmapData,
                            size = 110.dp,
                            context = context,
                            cornerRadius = 20.dp,
                            modifier = GlanceModifier.size(110.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = info.songTitle.ifEmpty { "Not Playing" },
                                style = TextStyle(
                                    color = colors.onSurface,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 2
                            )
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(
                                text = info.artistName.ifEmpty { "PixelPlay" },
                                style = TextStyle(
                                    color = colors.artist,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            Text(
                                text = info.albumName.ifEmpty { "Alpha 6.0" },
                                style = TextStyle(
                                    color = colors.artist,
                                    fontSize = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(24.dp))

                    // Transport Controls
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShuffleButton(
                            modifier = GlanceModifier.size(48.dp),
                            backgroundColor = if (info.isShuffleEnabled) colors.onSurface else colors.prevNextBackground,
                            iconColor = if (info.isShuffleEnabled) colors.surface else colors.prevNextIcon,
                            cornerRadius = 14.dp
                        )
                        Spacer(modifier = GlanceModifier.width(20.dp))
                        PreviousButton(
                            modifier = GlanceModifier.size(48.dp),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = 14.dp
                        )
                        Spacer(modifier = GlanceModifier.width(20.dp))
                        PlayPauseButton(
                            modifier = GlanceModifier.size(64.dp),
                            isPlaying = info.isPlaying,
                            backgroundColor = colors.playPauseBackground,
                            iconColor = colors.playPauseIcon,
                            cornerRadius = 32.dp,
                            iconSize = 36.dp
                        )
                        Spacer(modifier = GlanceModifier.width(20.dp))
                        NextButton(
                            modifier = GlanceModifier.size(48.dp),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = 14.dp
                        )
                        Spacer(modifier = GlanceModifier.width(20.dp))

                        val repeatButtonColor = if (info.repeatMode != Player.REPEAT_MODE_OFF) colors.onSurface else colors.prevNextBackground
                        val repeatIconRes = if (info.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.rounded_repeat_one_24 else R.drawable.rounded_repeat_24
                        val repeatIconColor = if (info.repeatMode != Player.REPEAT_MODE_OFF) colors.surface else colors.prevNextIcon

                        RepeatButton(
                            modifier = GlanceModifier.size(48.dp),
                            backgroundColor = repeatButtonColor,
                            iconRes = repeatIconRes,
                            iconColor = repeatIconColor,
                            cornerRadius = 14.dp
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(24.dp))

                    // Extended Queue Preview
                    if (info.queue.isNotEmpty()) {
                        Text(
                            text = "Next in Queue",
                            style = TextStyle(
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            info.queue.take(5).forEach { item ->
                                Box(modifier = GlanceModifier.padding(end = 12.dp)) {
                                    AlbumArtImage(
                                        bitmapData = item.albumArtBitmapData,
                                        size = 56.dp,
                                        context = context,
                                        cornerRadius = 12.dp,
                                        modifier = GlanceModifier.size(56.dp)
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

class UltraWidget4x4Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UltraWidget4x4()
}
