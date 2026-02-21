package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
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

class LargeWidget4x3 : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(250.dp, 180.dp),
            androidx.compose.ui.unit.DpSize(300.dp, 220.dp),
            androidx.compose.ui.unit.DpSize(350.dp, 260.dp)
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
                    .cornerRadius(28.dp)
                    .padding(16.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    // Top Row: Album Art and Info
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtImage(
                            bitmapData = info.albumArtBitmapData,
                            size = 80.dp,
                            context = context,
                            cornerRadius = 16.dp,
                            modifier = GlanceModifier.size(80.dp)
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = info.songTitle.ifEmpty { "Not Playing" },
                                style = TextStyle(
                                    color = colors.onSurface,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 2
                            )
                            Text(
                                text = info.artistName.ifEmpty { "PixelPlay" },
                                style = TextStyle(
                                    color = colors.artist,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(16.dp))

                    // Controls Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShuffleButton(
                            modifier = GlanceModifier.size(40.dp),
                            backgroundColor = if (info.isShuffleEnabled) colors.onSurface else colors.prevNextBackground,
                            iconColor = if (info.isShuffleEnabled) colors.surface else colors.prevNextIcon,
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        PreviousButton(
                            modifier = GlanceModifier.size(40.dp),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        PlayPauseButton(
                            modifier = GlanceModifier.size(56.dp),
                            isPlaying = info.isPlaying,
                            backgroundColor = colors.playPauseBackground,
                            iconColor = colors.playPauseIcon,
                            cornerRadius = 28.dp,
                            iconSize = 32.dp
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        NextButton(
                            modifier = GlanceModifier.size(40.dp),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = GlanceModifier.width(16.dp))

                        val repeatButtonColor = if (info.repeatMode != Player.REPEAT_MODE_OFF) colors.onSurface else colors.prevNextBackground
                        val repeatIconRes = if (info.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.rounded_repeat_one_24 else R.drawable.rounded_repeat_24
                        val repeatIconColor = if (info.repeatMode != Player.REPEAT_MODE_OFF) colors.surface else colors.prevNextIcon

                        RepeatButton(
                            modifier = GlanceModifier.size(40.dp),
                            backgroundColor = repeatButtonColor,
                            iconRes = repeatIconRes,
                            iconColor = repeatIconColor,
                            cornerRadius = 12.dp
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(16.dp))

                    // Queue Preview
                    if (info.queue.isNotEmpty()) {
                        Text(
                            text = "Up Next",
                            style = TextStyle(
                                color = colors.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier.padding(start = 0.dp, top = 0.dp, end = 0.dp, bottom = 8.dp)
                        )
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            info.queue.take(4).forEach { item ->
                                Box(modifier = GlanceModifier.padding(start = 0.dp, top = 0.dp, end = 8.dp, bottom = 0.dp)) {
                                    AlbumArtImage(
                                        bitmapData = item.albumArtBitmapData,
                                        size = 48.dp,
                                        context = context,
                                        cornerRadius = 8.dp,
                                        modifier = GlanceModifier.size(48.dp)
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

class LargeWidget4x3Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LargeWidget4x3()
}
