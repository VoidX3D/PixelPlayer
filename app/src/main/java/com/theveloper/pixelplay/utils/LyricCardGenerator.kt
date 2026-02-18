package com.theveloper.pixelplay.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.theveloper.pixelplay.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LyricCardGenerator {

    suspend fun generateAndShare(
        context: Context,
        song: Song,
        lyrics: String,
        imageLoader: ImageLoader
    ) {
        val bitmap = withContext(Dispatchers.IO) {
            createLyricCardBitmap(context, song, lyrics, imageLoader)
        } ?: return

        val file = File(context.cacheDir, "lyric_card.png")
        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Lyric Card"))
    }

    private suspend fun createLyricCardBitmap(
        context: Context,
        song: Song,
        lyrics: String,
        imageLoader: ImageLoader
    ): Bitmap? {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background Color (Dark gradient)
        val paint = Paint()
        val gradient = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#121212"), Color.parseColor("#000000"),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Album Art
        val artUri = song.albumArtUriString
        if (artUri != null) {
            val request = ImageRequest.Builder(context)
                .data(artUri)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                val artBitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (artBitmap != null) {
                    val scaledArt = Bitmap.createScaledBitmap(artBitmap, width, width, true)

                    // Draw blurred background art
                    val blurPaint = Paint().apply {
                        alpha = 100
                    }
                    canvas.drawBitmap(scaledArt, 0f, 0f, blurPaint)

                    // Draw main art in center
                    val padding = 100
                    val artSize = width - (padding * 2)
                    val centeredArt = Bitmap.createScaledBitmap(artBitmap, artSize, artSize, true)
                    canvas.drawBitmap(centeredArt, padding.toFloat(), 300f, Paint())
                }
            }
        }

        // Song Info
        val infoPaint = Paint().apply {
            color = Color.WHITE
            textSize = 60f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(song.title, 100f, 1300f, infoPaint)

        infoPaint.apply {
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            alpha = 180
        }
        canvas.drawText(song.displayArtist, 100f, 1360f, infoPaint)

        // Lyrics
        val lyricsPaint = Paint().apply {
            color = Color.WHITE
            textSize = 70f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val wrappedLyrics = wrapText(lyrics, lyricsPaint, width - 200)
        var y = 1500f
        wrappedLyrics.take(4).forEach { line ->
            canvas.drawText(line, 100f, y, lyricsPaint)
            y += 90f
        }

        return bitmap
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }
}
