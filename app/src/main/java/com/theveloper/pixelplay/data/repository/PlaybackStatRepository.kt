package com.theveloper.pixelplay.data.repository

import com.theveloper.pixelplay.data.database.PlaybackStatDao
import com.theveloper.pixelplay.data.database.PlaybackStatEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class PlaybackStatRepository @Inject constructor(
    private val playbackStatDao: PlaybackStatDao
) {
    suspend fun insertStat(stat: PlaybackStatEntity) = playbackStatDao.insertStat(stat)
    fun getStatsForSong(mediaId: String): Flow<List<PlaybackStatEntity>> = playbackStatDao.getStatsForSong(mediaId)
    suspend fun getAverageCompletion(mediaId: String): Float = playbackStatDao.getAverageCompletion(mediaId)
}
