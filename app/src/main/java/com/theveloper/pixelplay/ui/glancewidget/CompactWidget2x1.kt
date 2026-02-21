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
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.PlayerInfo

class CompactWidget2x1 : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(110.dp, 40.dp),
            androidx.compose.ui.unit.DpSize(150.dp, 60.dp),
            androidx.compose.ui.unit.DpSize(200.dp, 80.dp)
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
                    .padding(8.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Art
                    AlbumArtImage(
                        bitmapData = info.albumArtBitmapData,
                        size = 48.dp,
                        context = context,
                        cornerRadius = 12.dp,
                        modifier = GlanceModifier.size(48.dp)
                    )

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Title and Artist
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = info.songTitle.ifEmpty { "Not Playing" },
                            style = TextStyle(
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = info.artistName.ifEmpty { "PixelPlay" },
                            style = TextStyle(
                                color = colors.artist,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }

                    // Controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayPauseButton(
                            modifier = GlanceModifier.size(32.dp),
                            isPlaying = info.isPlaying,
                            backgroundColor = colors.playPauseBackground,
                            iconColor = colors.playPauseIcon,
                            cornerRadius = 8.dp,
                            iconSize = 18.dp
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        NextButton(
                            modifier = GlanceModifier.size(32.dp),
                            backgroundColor = colors.prevNextBackground,
                            iconColor = colors.prevNextIcon,
                            cornerRadius = 8.dp,
                            iconSize = 18.dp
                        )
                    }
                }
            }
        }
    }
}

class CompactWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget2x1()
}
