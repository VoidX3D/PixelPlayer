package com.theveloper.pixelplay.data.metadata.provider

import android.content.Context
import com.theveloper.pixelplay.data.ai.MetadataProvider
import com.theveloper.pixelplay.data.ai.SongMetadata
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.MetadataPreferencesRepository
import com.theveloper.pixelplay.data.network.metadata.LastFmApiService
import mms.lastfm.LastFmTrackResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated LastFM provider — uses user's API key from settings.
 */
@Singleton
class LastFmProvider @Inject constructor(
    private val lastFmApiService: LastFmApiService,
    private val metadataPreferencesRepository: MetadataPreferencesRepository
) : MetadataProvider {
    override val providerId: String = "lastfm"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        return try {
            val apiKey = metadataPreferencesRepository.lastFmApiKey.first()
            val response = lastFmApiService.getTrackInfo(song.title, song.displayArtist, apiKey)
            val track = response?.track
            
            if (track != null) {
                Result.success(SongMetadata(
                    title = track.name.takeIf { it.isNotBlank() },
                    artist = track.artist?.name?.takeIf { it.isNotBlank() },
                    album = track.album?.name?.takeIf { it.isNotBlank() },
                    genre = track.toptags?.tag?.firstOrNull()?.name,
                    albumArtUrl = track.album?.image?.lastOrNull()?.text?.takeIf { it.isNotBlank() }
                ))
            } else {
                Result.success(SongMetadata())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
