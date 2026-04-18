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

// Stub implementations
@Singleton
class MusicMetadataSourceProvider @Inject constructor() : MetadataProvider {
    override val providerId: String = "musicmetadatasource"
    override suspend fun getMetadata(song: Song): Result<SongMetadata> {
        // TODO: Implement actual lookup using MusicMetadataSource library
        return Result.success(SongMetadata())
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
