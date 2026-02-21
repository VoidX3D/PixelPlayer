package com.theveloper.pixelplay.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theveloper.pixelplay.data.ai.AiPlaylistGenerator
import com.theveloper.pixelplay.data.repository.MusicRepository
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Background worker that pre-curates a dynamic playlist for the home screen widget
 * based on the user's listening patterns using Gemini AI.
 */
@HiltWorker
class WidgetPlaylistWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val playlistGenerator: AiPlaylistGenerator,
    private val musicRepository: MusicRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("WidgetPlaylistWorker: Starting pre-curation...")

        val apiKey = userPreferencesRepository.geminiApiKey.first()
        if (apiKey.isBlank()) {
            Timber.d("WidgetPlaylistWorker: Gemini API Key not set, skipping.")
            return Result.success()
        }

        return try {
            val allSongs = musicRepository.getAllSongsOnce()
            if (allSongs.isEmpty()) {
                Timber.d("WidgetPlaylistWorker: Library is empty.")
                return Result.success()
            }

            val result = playlistGenerator.generate(
                userPrompt = "Pre-curate a 'Zero-Latency' playlist for my home screen widget. Focus on my favorite genres and high-relevance songs for instant playback.",
                allSongs = allSongs,
                minLength = 10,
                maxLength = 25
            )

            result.fold(
                onSuccess = { songs ->
                    Timber.d("WidgetPlaylistWorker: Pre-curated ${songs.size} songs.")
                    userPreferencesRepository.saveYourMixSongIds(songs.map { it.id })
                    Result.success()
                },
                onFailure = {
                    Timber.e(it, "WidgetPlaylistWorker: Generation failed")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "WidgetPlaylistWorker: Unexpected error")
            Result.failure()
        }
    }
}
