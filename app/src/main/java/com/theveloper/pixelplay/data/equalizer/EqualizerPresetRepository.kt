package com.theveloper.pixelplay.data.equalizer

import android.content.Context
import android.net.Uri
import com.theveloper.pixelplay.data.database.EqualizerDao
import com.theveloper.pixelplay.data.database.toDomain
import com.theveloper.pixelplay.data.database.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerPresetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val equalizerDao: EqualizerDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    val customPresets: Flow<List<EqualizerPreset>> = equalizerDao.getAllPresets().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun savePreset(preset: EqualizerPreset) = withContext(Dispatchers.IO) {
        equalizerDao.insertPreset(preset.toEntity())
    }

    suspend fun deletePreset(preset: EqualizerPreset) = withContext(Dispatchers.IO) {
        equalizerDao.deletePresetByName(preset.name)
    }

    suspend fun exportPreset(preset: EqualizerPreset, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(EqualizerPreset.serializer(), preset)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importPreset(uri: Uri): EqualizerPreset? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString(EqualizerPreset.serializer(), jsonString)
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getPresetByName(name: String): EqualizerPreset? = withContext(Dispatchers.IO) {
        equalizerDao.getPresetByName(name)?.toDomain()
    }
}
