package com.theveloper.pixelplay.data.metadata.provider

import android.content.Context
import com.theveloper.pixelplay.data.ai.MetadataProvider
import com.theveloper.pixelplay.data.ai.SongMetadata
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.MetadataPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import mms.lastfm.LastFMRestClient
import mms.lastfm.LastFmTrackResponse
import mms.util.emit
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default provider that delegates to the MusicMetadataSource library (LastFM + MusicBrainz).
 *
 * Queries LastFM first (requires an API key configured in settings); if no key is set or
 * the lookup fails, it falls back to MusicBrainz which has a public API.
 */
@Singleton
class MusicMetadataSourceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
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
            // The library client handles the network call orchestration via emit()
            val client = LastFMRestClient(context, "PixelPlayer")
            val response = client.apiService.getTrackInfo(song.title, song.displayArtist, null).emit()
            val track = (response.dataOrNull() as? LastFmTrackResponse)?.track ?: return null
            
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
            val client = mms.musicbrainz.MusicBrainzRestClient(context, "PixelPlayer")
            val query = "recording:\"${song.title}\" AND artist:\"${song.displayArtist}\""
            val response = client.apiService.searchRecording(query, 0).emit()
            val recordings = (response.dataOrNull() as? mms.musicbrainz.MusicBrainzSearchResultRecording)?.recordings ?: return null
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
