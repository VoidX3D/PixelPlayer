package com.theveloper.pixelplay.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_stats")
data class PlaybackStatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: String,
    val timestamp: Long,
    val completionRate: Float,
    val skipVelocity: Float = 0f,
    val volumePreference: Float = 1.0f,
    val timeOfDay: Long = 0L
)
