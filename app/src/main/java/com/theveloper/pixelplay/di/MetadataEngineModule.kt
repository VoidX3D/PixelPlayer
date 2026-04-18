package com.theveloper.pixelplay.di

import com.theveloper.pixelplay.data.ai.MetadataProvider
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.ai.SongMetadata
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject
import javax.inject.Singleton

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

// Implementation using io.github.phonographplus:music-metadata-source
@Singleton
class MusicMetadataSourceProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataPreferencesRepository: MetadataPreferencesRepository
) : MetadataProvider {
    override val providerId: String = "musicmetadatasource"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        return try {
            // MusicMetadataSource provides a unified way to fetch metadata
            // from various sources (Last.fm, MusicBrainz)
            val source = io.github.phonographplus.musicmetadatasource.MusicMetadataSource(context)
            
            // Note: In a real implementation, we'd configure the source with keys if it supports them
            // For now we use its default behavior which might use its own internal keys or public endpoints
            
            val tags = source.getMetadata(song.displayArtist, song.album, song.title)
            
            Result.success(SongMetadata(
                title = tags.title ?: song.title,
                artist = tags.artist ?: song.displayArtist,
                album = tags.album ?: song.album,
                genre = tags.genre ?: song.genre,
                albumArtUrl = tags.albumArtUrl
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class LastFmProvider @Inject constructor(
    private val metadataPreferencesRepository: MetadataPreferencesRepository
) : MetadataProvider {
    override val providerId: String = "lastfm"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        // val apiKey = metadataPreferencesRepository.lastFmApiKey.first()
        // TODO: Implement actual lookup using Last.fm API with the key
        return Result.success(SongMetadata())
    }
}

@Singleton
class MusicBrainzProvider @Inject constructor(
    private val metadataPreferencesRepository: MetadataPreferencesRepository
) : MetadataProvider {
    override val providerId: String = "musicbrainz"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        // val apiKey = metadataPreferencesRepository.musicBrainzApiKey.first()
        // TODO: Implement actual lookup using MusicBrainz API
        return Result.success(SongMetadata())
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MetadataEngineModule {
    @Binds
    @IntoSet
    abstract fun bindMusicMetadataSourceProvider(
        provider: MusicMetadataSourceProvider
    ): MetadataProvider

    @Binds
    @IntoSet
    abstract fun bindLastFmProvider(
        provider: LastFmProvider
    ): MetadataProvider

    @Binds
    @IntoSet
    abstract fun bindMusicBrainzProvider(
        provider: MusicBrainzProvider
    ): MetadataProvider
}
