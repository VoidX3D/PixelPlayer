package com.theveloper.pixelplay.data.network.metadata

import mms.musicbrainz.MusicBrainzSearchResultRecording
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Headers

interface MusicBrainzApiService {
    @Headers("Accept: application/json")
    @GET("ws/2/recording")
    suspend fun searchRecording(
        @Query("query") query: String,
        @Query("offset") offset: Int,
        @Query("fmt") format: String = "json"
    ): MusicBrainzSearchResultRecording?
}
