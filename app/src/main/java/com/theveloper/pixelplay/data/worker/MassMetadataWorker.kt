package com.theveloper.pixelplay.data.worker

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.theveloper.pixelplay.data.ai.MetadataEditor
import com.theveloper.pixelplay.data.database.MusicDao
import com.theveloper.pixelplay.data.database.toSong
import com.theveloper.pixelplay.data.media.CoverArtUpdate
import com.theveloper.pixelplay.data.media.SongMetadataEditor
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Background worker that iterates through the entire song library and enriches
 * metadata using the user-selected [MetadataEditor] provider.
 *
 * Progress is reported via [setProgress] so the UI can observe it through WorkManager.
 */
@HiltWorker
class MassMetadataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val musicDao: MusicDao,
    private val metadataEditor: MetadataEditor,
    private val songMetadataEditor: SongMetadataEditor,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val PROGRESS_CURRENT = "progress_current"
        const val PROGRESS_TOTAL = "progress_total"
    }

    override suspend fun doWork(): Result {
        Timber.d("MassMetadataWorker started")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Timber.w("MassMetadataWorker requires Android 11+ for MediaStore write operations.")
            return Result.failure()
        }

        val providerId = userPreferencesRepository.metadataProviderFlow.first()
        val allSongs = musicDao.getAllSongsList()
        val totalCount = allSongs.size

        if (totalCount == 0) return Result.success()

        var processedCount = 0

        for (songEntity in allSongs) {
            // CoroutineWorker exposes `isStopped` — not `isActive`
            if (isStopped) break

            val song = songEntity.toSong()

            try {
                val result = metadataEditor.getMetadata(song, providerId)

                result.getOrNull()?.let { newMetadata ->
                    // Only write fields that the provider actually returned
                    val hasChanges = !newMetadata.title.isNullOrBlank()
                            || !newMetadata.artist.isNullOrBlank()
                            || !newMetadata.album.isNullOrBlank()
                            || !newMetadata.genre.isNullOrBlank()
                            || !newMetadata.albumArtUrl.isNullOrBlank()

                    if (hasChanges) {
                        songMetadataEditor.editSongMetadata(
                            songId = song.id.toLong(),
                            newTitle = newMetadata.title?.takeIf { it.isNotBlank() } ?: song.title,
                            newArtist = newMetadata.artist?.takeIf { it.isNotBlank() } ?: song.displayArtist,
                            newAlbum = newMetadata.album?.takeIf { it.isNotBlank() } ?: song.album,
                            newGenre = newMetadata.genre?.takeIf { it.isNotBlank() } ?: (song.genre ?: ""),
                            newLyrics = song.lyrics ?: "",
                            newTrackNumber = song.trackNumber,
                            newDiscNumber = song.discNumber,
                            coverArtUpdate = newMetadata.albumArtUrl
                                ?.takeIf { it.isNotBlank() }
                                ?.let { CoverArtUpdate(remoteUrl = it) }
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to process song: %s", song.title)
            }

            processedCount++

            setProgress(
                Data.Builder()
                    .putInt(PROGRESS_CURRENT, processedCount)
                    .putInt(PROGRESS_TOTAL, totalCount)
                    .build()
            )
        }

        Timber.d("MassMetadataWorker finished. Processed %d/%d songs.", processedCount, totalCount)
        return Result.success()
    }
}
