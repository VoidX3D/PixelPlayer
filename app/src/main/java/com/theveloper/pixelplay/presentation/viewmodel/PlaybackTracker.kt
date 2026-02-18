package com.theveloper.pixelplay.presentation.viewmodel

import com.theveloper.pixelplay.data.database.PlaybackStatEntity
import com.theveloper.pixelplay.data.repository.PlaybackStatRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class PlaybackTracker @Inject constructor(
    private val repository: PlaybackStatRepository,
    private val userPreferencesRepository: com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
) {
    private var currentMediaId: String? = null
    private var startTime: Long = 0
    private var maxPosition: Long = 0
    private var totalDuration: Long = 0

    suspend fun trackStart(mediaId: String, duration: Long) {
        if (currentMediaId != null) trackEnd()
        currentMediaId = mediaId
        startTime = System.currentTimeMillis()
        maxPosition = 0
        totalDuration = duration
    }

    suspend fun trackProgress(position: Long) {
        if (position > maxPosition) maxPosition = position
    }

    suspend fun trackEnd() {
        val mediaId = currentMediaId ?: return
        val volume = try {
            userPreferencesRepository.preAmpFactorFlow.first()
        } catch (e: Exception) {
            1.0f
        }

        val completion = if (totalDuration > 0) {
            maxPosition.toFloat() / totalDuration.toFloat()
        } else 1.0f

        val stat = PlaybackStatEntity(
            mediaId = mediaId,
            timestamp = startTime,
            completionRate = completion.coerceIn(0f, 1f),
            skipVelocity = if (completion < 0.1f) 1.0f else 0f,
            volumePreference = volume
        )
        repository.insertStat(stat)
        currentMediaId = null
    }
}
