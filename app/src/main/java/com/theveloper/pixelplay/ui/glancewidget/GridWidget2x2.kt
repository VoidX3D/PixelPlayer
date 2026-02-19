package com.theveloper.pixelplay.ui.glancewidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.theveloper.pixelplay.MainActivity
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.PlayerInfo
import timber.log.Timber

class GridWidget2x2 : GlanceAppWidget() {

    companion object {
        // LruCache for Bitmaps
        private object AlbumArtBitmapCache {
            private const val CACHE_SIZE_BYTES = 2 * 1024 * 1024 // 2 MiB
            private val lruCache = object : LruCache<String, Bitmap>(CACHE_SIZE_BYTES) {
                override fun sizeOf(key: String, value: Bitmap): Int {
                    return value.byteCount
                }
            }

            fun getBitmap(key: String): Bitmap? = lruCache.get(key)

            fun putBitmap(key: String, bitmap: Bitmap) {
                if (getBitmap(key) == null) {
                    lruCache.put(key, bitmap)
                }
            }

            fun getKey(byteArray: ByteArray): String {
                return byteArray.contentHashCode().toString()
            }
        }
    }

    override val sizeMode = SizeMode.Exact
    override val stateDefinition = PlayerInfoStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val playerInfo = currentState<PlayerInfo>()
            GlanceTheme {
                GridWidget2x2Content(playerInfo = playerInfo, context = context)
            }
        }
    }

    @Composable
    private fun GridWidget2x2Content(
        playerInfo: PlayerInfo,
        context: Context
    ) {
        val isPlaying = playerInfo.isPlaying
        val albumArtBitmapData = playerInfo.albumArtBitmapData

        val themeColors = playerInfo.themeColors

        val bgColor = themeColors?.let {
            ColorProvider(
                day = Color(it.lightSurfaceContainer),
                night = Color(it.darkSurfaceContainer)
            )
        } ?: GlanceTheme.colors.surface

        val playPauseBgColor = themeColors?.let {
            ColorProvider(
                day = Color(it.lightPlayPauseBackground),
                night = Color(it.darkPlayPauseBackground)
            )
        } ?: GlanceTheme.colors.primaryContainer

        val playPauseIcColor = themeColors?.let {
            ColorProvider(
                day = Color(it.lightPlayPauseIcon),
                night = Color(it.darkPlayPauseIcon)
            )
        } ?: GlanceTheme.colors.onPrimaryContainer

        val prevNextBgColor = themeColors?.let {
            ColorProvider(
                day = Color(it.lightPrevNextBackground),
                night = Color(it.darkPrevNextBackground)
            )
        } ?: GlanceTheme.colors.secondaryContainer

        val prevNextIcColor = themeColors?.let {
            ColorProvider(
                day = Color(it.lightPrevNextIcon),
                night = Color(it.darkPrevNextIcon)
            )
        } ?: GlanceTheme.colors.onSecondaryContainer

        val widgetCornerRadius = 28.dp
        val itemCornerRadius = 16.dp

        val size = LocalSize.current
        val minSide = min(size.width, size.height)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(minSide)
                    .background(bgColor)
                    .cornerRadius(widgetCornerRadius)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Top
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth()
                    ) {
                        AlbumArtImage(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            bitmapData = albumArtBitmapData,
                            size = 44.dp,
                            context = context,
                            cornerRadius = itemCornerRadius
                        )

                        Spacer(GlanceModifier.width(6.dp))

                        // Play/Pause Button
                        PlayPauseButton(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            isPlaying = isPlaying,
                            backgroundColor = playPauseBgColor,
                            iconColor = playPauseIcColor,
                            cornerRadius = itemCornerRadius
                        )
                    }

                    Spacer(GlanceModifier.height(6.dp))

                    // Bottom
                    Row(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxWidth()
                    ) {
                        // Previous Button
                        PreviousButton(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            backgroundColor = prevNextBgColor,
                            iconColor = prevNextIcColor,
                            cornerRadius = itemCornerRadius
                        )

                        Spacer(GlanceModifier.width(6.dp))

                        // Next Button
                        NextButton(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight(),
                            backgroundColor = prevNextBgColor,
                            iconColor = prevNextIcColor,
                            cornerRadius = itemCornerRadius
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AlbumArtImage(
        modifier: GlanceModifier,
        bitmapData: ByteArray?,
        size: Dp,
        context: Context,
        cornerRadius: Dp
    ) {
        val imageProvider = bitmapData?.let { data ->
            val cacheKey = AlbumArtBitmapCache.getKey(data)
            var bitmap = AlbumArtBitmapCache.getBitmap(cacheKey)

            if (bitmap == null) {
                try {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeByteArray(data, 0, data.size, options)

                    var inSampleSize = 1
                    val targetSizePx = (size.value * context.resources.displayMetrics.density).toInt()

                    if (options.outHeight > targetSizePx || options.outWidth > targetSizePx) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while (halfHeight / inSampleSize >= targetSizePx &&
                            halfWidth / inSampleSize >= targetSizePx) {
                            inSampleSize *= 2
                        }
                    }

                    options.inSampleSize = inSampleSize
                    options.inJustDecodeBounds = false
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)

                    bitmap?.let { AlbumArtBitmapCache.putBitmap(cacheKey, it) }
                } catch (e: Exception) {
                    Timber.tag("BarWidget4x1").e(e, "Error decoding bitmap")
                    bitmap = null
                }
            }
            bitmap?.let { ImageProvider(it) }
        }

        Box(
            modifier = modifier
        ) {
            if (imageProvider != null) {
                Image(
                    provider = imageProvider,
                    contentDescription = "Album Art",
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(cornerRadius),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(cornerRadius)
                        .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_music_placeholder),
                        contentDescription = "Placeholder",
                        modifier = GlanceModifier.size(size * 0.6f),
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
        }
    }

    @Composable
    private fun PreviousButton(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        iconColor: ColorProvider,
        cornerRadius: Dp
    ) {
        val params = actionParametersOf(PlayerActions.key to PlayerActions.PREVIOUS)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.rounded_skip_previous_24),
                contentDescription = "Previous",
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    @Composable
    private fun PlayPauseButton(
        modifier: GlanceModifier,
        isPlaying: Boolean,
        backgroundColor: ColorProvider,
        iconColor: ColorProvider,
        cornerRadius: Dp
    ) {
        val params = actionParametersOf(PlayerActions.key to PlayerActions.PLAY_PAUSE)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(
                    if (isPlaying) R.drawable.rounded_pause_24
                    else R.drawable.rounded_play_arrow_24
                ),
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = GlanceModifier.size(22.dp),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }

    @Composable
    private fun NextButton(
        modifier: GlanceModifier,
        backgroundColor: ColorProvider,
        iconColor: ColorProvider,
        cornerRadius: Dp
    ) {
        val params = actionParametersOf(PlayerActions.key to PlayerActions.NEXT)
        Box(
            modifier = modifier
                .background(backgroundColor)
                .cornerRadius(cornerRadius)
                .clickable(actionRunCallback<PlayerControlActionCallback>(params)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(R.drawable.rounded_skip_next_24),
                contentDescription = "Next",
                modifier = GlanceModifier.size(20.dp),
                colorFilter = ColorFilter.tint(iconColor)
            )
        }
    }
}