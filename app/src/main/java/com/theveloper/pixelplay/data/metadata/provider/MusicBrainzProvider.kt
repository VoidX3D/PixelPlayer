package com.theveloper.pixelplay.data.metadata.provider

import android.content.Context
import com.theveloper.pixelplay.data.ai.MetadataProvider
import com.theveloper.pixelplay.data.ai.SongMetadata
import com.theveloper.pixelplay.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import mms.musicbrainz.MusicBrainzRestClient
import mms.musicbrainz.MusicBrainzSearchResultRecording
import mms.util.emit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dedicated MusicBrainz provider — uses public API (no key required).
 */
@Singleton
class MusicBrainzProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : MetadataProvider {
    override val providerId: String = "musicbrainz"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        return try {
            val client = MusicBrainzRestClient(context, "PixelPlayer")
            val query = "recording:\"${song.title}\" AND artist:\"${song.displayArtist}\""
            val response = client.apiService.searchRecording(query, 0).emit()
            val recordings = (response.dataOrNull() as? MusicBrainzSearchResultRecording)?.recordings
            val firstHit = recordings?.firstOrNull()
            
            if (firstHit != null) {
                Result.success(SongMetadata(
                    title = firstHit.title.takeIf { it.isNotBlank() },
                    artist = firstHit.artistCredit.firstOrNull()?.name?.takeIf { it.isNotBlank() },
                    album = firstHit.releases?.firstOrNull()?.title?.takeIf { it.isNotBlank() },
                    genre = firstHit.genres.firstOrNull()?.name ?: firstHit.tags?.firstOrNull()?.name,
                    albumArtUrl = null
                ))
            } else {
                Result.success(SongMetadata())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
