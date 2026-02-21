package com.theveloper.pixelplay.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.theveloper.pixelplay.data.equalizer.EqualizerPreset

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey val id: String, // name
    val displayName: String,
    val bandLevels: String, // Comma-separated levels
    val isCustom: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

fun EqualizerPresetEntity.toDomain(): EqualizerPreset {
    return EqualizerPreset(
        name = id,
        displayName = displayName,
        bandLevels = try {
            bandLevels.split(",").map { it.toInt() }
        } catch (e: Exception) {
            List(10) { 0 }
        },
        isCustom = isCustom
    )
}

fun EqualizerPreset.toEntity(): EqualizerPresetEntity {
    return EqualizerPresetEntity(
        id = name,
        displayName = displayName,
        bandLevels = bandLevels.joinToString(","),
        isCustom = isCustom
    )
}
