package com.theveloper.pixelplay.data.network.metadata

import mms.lastfm.LastFmTrackResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmApiService {
    @GET("2.0/?method=track.getInfo&format=json")
    suspend fun getTrackInfo(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String,
        @Query("mbid") mbid: String? = null
    ): LastFmTrackResponse?
}
