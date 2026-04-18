package com.theveloper.pixelplay.di

import com.theveloper.pixelplay.data.ai.MetadataProvider
import com.theveloper.pixelplay.data.metadata.provider.LastFmProvider
import com.theveloper.pixelplay.data.metadata.provider.MusicBrainzProvider
import com.theveloper.pixelplay.data.metadata.provider.MusicMetadataSourceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

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
