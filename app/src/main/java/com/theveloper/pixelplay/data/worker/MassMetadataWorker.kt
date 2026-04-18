package com.theveloper.pixelplay.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import android.os.Build
import com.theveloper.pixelplay.data.media.CoverArtUpdate
import com.theveloper.pixelplay.data.media.SongMetadataEditor
import com.theveloper.pixelplay.data.database.MusicDao
import com.theveloper.pixelplay.data.ai.MetadataEditor
import com.theveloper.pixelplay.data.model.toSong
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import timber.log.Timber

@HiltWorker
class MassMetadataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val musicDao: MusicDao,
    private val metadataEditor: MetadataEditor,
    private val songMetadataEditor: SongMetadataEditor
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val PROGRESS_CURRENT = "progress_current"
        const val PROGRESS_TOTAL = "progress_total"
    }

    override suspend fun doWork(): Result {
        Timber.d("MassMetadataWorker started")
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Timber.w("MassMetadataWorker requires Android 11 (API 30) or higher for MediaStore write operations.")
            return Result.failure()
        }

        val allSongs = musicDao.getAllSongs().first()
        val totalCount = allSongs.size
        
        if (totalCount == 0) return Result.success()

        var processedCount = 0
        
        for (songEntity in allSongs) {
            if (!isActive) break
            
            val song = songEntity.toSong()
            
            try {
                // 1. Fetch metadata from selected provider
                val result = metadataEditor.generateMetadata(song)
                
                result.getOrNull()?.let { newMetadata ->
                    // 2. Apply metadata to the file and database
                    songMetadataEditor.editSongMetadata(
                        songId = song.id,
                        newTitle = newMetadata.title ?: song.title,
                        newArtist = newMetadata.artist ?: song.displayArtist,
                        newAlbum = newMetadata.album ?: song.album,
                        newGenre = newMetadata.genre ?: (song.genre ?: ""),
                        newLyrics = song.lyrics ?: "",
                        newTrackNumber = song.trackNumber,
                        newDiscNumber = song.discNumber,
                        coverArtUpdate = newMetadata.albumArtUrl?.let { CoverArtUpdate(remoteUrl = it) }
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to process song: ${song.title}")
            }
            
            processedCount++
            
            // Update progress
            setProgress(
                Data.Builder()
                    .putInt(PROGRESS_CURRENT, processedCount)
                    .putInt(PROGRESS_TOTAL, totalCount)
                    .build()
            )
        }

        Timber.d("MassMetadataWorker finished. Processed $processedCount/$totalCount songs.")
        return Result.success()
    }
}
