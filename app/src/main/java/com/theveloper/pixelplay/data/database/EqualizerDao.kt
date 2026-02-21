package com.theveloper.pixelplay.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EqualizerDao {
    @Query("SELECT * FROM equalizer_presets ORDER BY timestamp DESC")
    fun getAllPresets(): Flow<List<EqualizerPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity)

    @Delete
    suspend fun deletePreset(preset: EqualizerPresetEntity)

    @Query("DELETE FROM equalizer_presets WHERE id = :name")
    suspend fun deletePresetByName(name: String)

    @Query("SELECT * FROM equalizer_presets WHERE id = :name LIMIT 1")
    suspend fun getPresetByName(name: String): EqualizerPresetEntity?
}
