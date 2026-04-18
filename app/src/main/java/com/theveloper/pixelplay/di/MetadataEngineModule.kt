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
    @ApplicationContext private val context: Context
) : MetadataProvider {
    override val providerId: String = "musicmetadatasource"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        return try {
            // MusicMetadataSource provides a unified way to fetch metadata
            // from various sources (Last.fm, MusicBrainz)
            // val source = io.github.phonographplus.musicmetadatasource.MusicMetadataSource(context)
            // val tags = source.getMetadata(song.displayArtist, song.album, song.title)
            
            // This is a placeholder until the exact API is confirmed by a build
            // The library is specialized for Phonograph Plus ecosystem
            Result.success(SongMetadata(
                title = song.title,
                artist = song.displayArtist,
                album = song.album,
                genre = song.genre,
                albumArtUrl = null // TODO: Extract from source tags
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class LastFmProvider @Inject constructor() : MetadataProvider {
    override val providerId: String = "lastfm"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        // TODO: Implement actual lookup using Last.fm API
        return Result.success(SongMetadata())
    }
}

@Singleton
class MusicBrainzProvider @Inject constructor() : MetadataProvider {
    override val providerId: String = "musicbrainz"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
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
