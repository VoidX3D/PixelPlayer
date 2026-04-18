package com.theveloper.pixelplay.data.metadata.provider

import android.content.Context
import com.theveloper.pixelplay.data.ai.MetadataProvider
import com.theveloper.pixelplay.data.ai.SongMetadata
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.MetadataPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import com.theveloper.pixelplay.data.network.metadata.LastFmApiService
import com.theveloper.pixelplay.data.network.metadata.MusicBrainzApiService
import mms.lastfm.LastFmTrackResponse
import mms.musicbrainz.MusicBrainzSearchResultRecording
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default provider that delegates to the MusicMetadataSource models but uses the 
 * project's standard Retrofit stack for networking to ensure stability.
 */
@Singleton
class MusicMetadataSourceProvider @Inject constructor(
    private val lastFmApiService: LastFmApiService,
    private val musicBrainzApiService: MusicBrainzApiService,
    private val metadataPreferencesRepository: MetadataPreferencesRepository
) : MetadataProvider {
    override val providerId: String = "musicmetadatasource"

    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        return try {
            // Try LastFM first if user has an API key configured
            val lastFmKey = metadataPreferencesRepository.lastFmApiKey.first()
            if (lastFmKey.isNotBlank()) {
                val lastFmResult = fetchFromLastFm(song, lastFmKey)
                if (lastFmResult != null) return Result.success(lastFmResult)
            }

            // Fallback to MusicBrainz (public, no key needed)
            val mbResult = fetchFromMusicBrainz(song)
            if (mbResult != null) return Result.success(mbResult)

            // Nothing found — return empty metadata (no error, just no enrichment)
            Result.success(SongMetadata())
        } catch (e: Exception) {
            Timber.e(e, "MusicMetadataSourceProvider failed for: %s", song.title)
            Result.failure(e)
        }
    }

    private suspend fun fetchFromLastFm(song: Song, apiKey: String): SongMetadata? {
        return try {
            val response = lastFmApiService.getTrackInfo(song.title, song.displayArtist, apiKey)
            val track = response?.track ?: return null
            
            SongMetadata(
                title = track.name.takeIf { it.isNotBlank() },
                artist = track.artist?.name?.takeIf { it.isNotBlank() },
                album = track.album?.name?.takeIf { it.isNotBlank() },
                genre = track.toptags?.tag?.firstOrNull()?.name,
                albumArtUrl = track.album?.image?.lastOrNull()?.text?.takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Timber.w(e, "LastFM lookup failed, falling back to MusicBrainz")
            null
        }
    }

    private suspend fun fetchFromMusicBrainz(song: Song): SongMetadata? {
        return try {
            val query = "recording:\"${song.title}\" AND artist:\"${song.displayArtist}\""
            val response = musicBrainzApiService.searchRecording(query, 0)
            val recordings = response?.recordings ?: return null
            val firstHit = recordings.firstOrNull() ?: return null
            
            SongMetadata(
                title = firstHit.title.takeIf { it.isNotBlank() },
                artist = firstHit.artistCredit.firstOrNull()?.name?.takeIf { it.isNotBlank() },
                album = firstHit.releases?.firstOrNull()?.title?.takeIf { it.isNotBlank() },
                genre = firstHit.genres.firstOrNull()?.name ?: firstHit.tags?.firstOrNull()?.name,
                albumArtUrl = null
            )
        } catch (e: Exception) {
            Timber.w(e, "MusicBrainz lookup failed")
            null
        }
    }
}
