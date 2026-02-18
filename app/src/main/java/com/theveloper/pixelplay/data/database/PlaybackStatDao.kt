package com.theveloper.pixelplay.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackStatDao {
    @Insert
    suspend fun insertStat(stat: PlaybackStatEntity)

    @Query("SELECT * FROM playback_stats WHERE mediaId = :mediaId ORDER BY timestamp DESC")
    fun getStatsForSong(mediaId: String): Flow<List<PlaybackStatEntity>>

    @Query("SELECT AVG(completionRate) FROM playback_stats WHERE mediaId = :mediaId")
    suspend fun getAverageCompletion(mediaId: String): Float

    @Query("SELECT mediaId FROM playback_stats GROUP BY mediaId ORDER BY COUNT(*) DESC LIMIT :limit")
    suspend fun getTopPlayedSongs(limit: Int): List<String>

    @Query("DELETE FROM playback_stats")
    suspend fun clearStats()
}
