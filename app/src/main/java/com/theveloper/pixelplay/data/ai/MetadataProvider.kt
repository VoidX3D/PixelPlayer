package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.model.Song

interface MetadataProvider {
    val providerId: String
    suspend fun getMetadata(song: Song): Result<SongMetadata>
}
