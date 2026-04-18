package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.model.Song
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject

@Serializable
data class SongMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val albumArtUrl: String? = null
)

class MetadataEditor @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards MetadataProvider>
) {
    suspend fun getMetadata(song: Song, providerId: String): Result<SongMetadata> {
        val provider = providers.find { it.providerId == providerId }
        if (provider == null) {
            Timber.e("Metadata provider %s not found. Available providers: %s", providerId, providers.map { it.providerId })
            return Result.failure(Exception("Metadata provider $providerId not found"))
        }
        return provider.getMetadata(song)
    }
}
