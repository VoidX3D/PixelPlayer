package com.theveloper.pixelplay.data.repository

import com.theveloper.pixelplay.data.database.PlaybackStatDao
import com.theveloper.pixelplay.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class RecommendationRepository @Inject constructor(
    private val playbackStatDao: PlaybackStatDao,
    private val musicRepository: MusicRepository
) {
    suspend fun generateDiscoveryMixPrompt(): String {
        val topSongs = playbackStatDao.getTopPlayedSongs(10)
        val songs = topSongs.mapNotNull { musicRepository.getSong(it).first() }

        return "Based on these favorite songs: ${songs.joinToString { "${it.title} by ${it.artist}" }}, suggest 5 similar tracks."
    }
}
