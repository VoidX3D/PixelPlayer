package com.theveloper.pixelplay.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theveloper.pixelplay.data.ai.AiMetadataGenerator
import com.theveloper.pixelplay.data.database.MusicDao
import com.theveloper.pixelplay.data.database.toSong
import com.theveloper.pixelplay.data.media.CoverArtUpdate
import com.theveloper.pixelplay.data.media.SongMetadataEditor
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Background worker that uses Gemini AI to auto-repair broken metadata
 * and identify high-resolution album art for local files.
 */
@HiltWorker
class MetadataRepairWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val aiMetadataGenerator: AiMetadataGenerator,
    private val musicDao: MusicDao,
    private val metadataEditor: SongMetadataEditor,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("MetadataRepairWorker: Starting repair cycle...")

        val apiKey = userPreferencesRepository.geminiApiKey.first()
        if (apiKey.isBlank()) {
            Timber.d("MetadataRepairWorker: Gemini API Key not set, skipping.")
            return Result.success()
        }

        return try {
            // Find songs with missing critical metadata
            val songsToRepair = musicDao.getAllSongsList().filter { entity ->
                entity.title.contains("Unknown", true) ||
                entity.artistName.contains("Unknown", true) ||
                entity.albumName.contains("Unknown", true) ||
                entity.genre.isNullOrBlank()
            }.take(10) // Limit per run to avoid rate limits/cost

            if (songsToRepair.isEmpty()) {
                Timber.d("MetadataRepairWorker: No songs need repair.")
                return Result.success()
            }

            var repairedCount = 0
            songsToRepair.forEach { entity ->
                val song = entity.toSong()
                val fieldsToRepair = mutableListOf<String>()
                if (song.title.contains("Unknown", true)) fieldsToRepair.add("title")
                if (song.artist.contains("Unknown", true)) fieldsToRepair.add("artist")
                if (song.album.contains("Unknown", true)) fieldsToRepair.add("album")
                if (song.genre.isNullOrBlank()) fieldsToRepair.add("genre")
                if (song.albumArtUriString == null) fieldsToRepair.add("highResAlbumArtUrl")

                val result = aiMetadataGenerator.generate(song, fieldsToRepair)
                result.onSuccess { suggested ->
                    Timber.d("MetadataRepairWorker: Suggested for ${song.title}: $suggested")

                    var coverArtUpdate: CoverArtUpdate? = null
                    if (!suggested.highResAlbumArtUrl.isNullOrBlank()) {
                        coverArtUpdate = downloadAlbumArt(suggested.highResAlbumArtUrl)
                    }

                    // Apply repairs
                    metadataEditor.editSongMetadata(
                        songId = song.id.toLong(),
                        newTitle = suggested.title ?: song.title,
                        newArtist = suggested.artist ?: song.artist,
                        newAlbum = suggested.album ?: song.album,
                        newAlbumArtist = suggested.artist ?: song.artist,
                        newYear = song.year,
                        newGenre = suggested.genre ?: song.genre ?: "",
                        newLyrics = song.lyrics ?: "",
                        newTrackNumber = song.trackNumber,
                        coverArtUpdate = coverArtUpdate
                    )
                    repairedCount++
                }
            }

            Timber.d("MetadataRepairWorker: Repaired $repairedCount songs.")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "MetadataRepairWorker: Unexpected error")
            Result.failure()
        }
    }

    private suspend fun downloadAlbumArt(url: String): CoverArtUpdate? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                val contentType = response.body?.contentType()?.toString() ?: "image/jpeg"
                if (bytes != null) {
                    return@withContext CoverArtUpdate(bytes, contentType)
                }
            }
            null
        } catch (e: Exception) {
            Timber.e(e, "MetadataRepairWorker: Failed to download album art from $url")
            null
        }
    }
}
