package com.theveloper.pixelplay.data.ai

import com.theveloper.pixelplay.data.database.EngagementDao
import com.theveloper.pixelplay.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileDigestGenerator @Inject constructor(
    private val engagementDao: EngagementDao
) {
    /**
     * Computes a highly condensed JSON representation of the user's listening profile,
     * perfect for injecting into a system prompt without blowing up the token context window.
     */
    suspend fun generateDigest(allSongs: List<Song>): String {
        val songMap = allSongs.associateBy { it.id }
        
        val topEngagements = engagementDao.getTopPlayedSongs(40)
        val recentEngagements = engagementDao.getRecentlyPlayedSongs(15)
        
        val artistFrequency = mutableMapOf<String, Int>()
        val genreFrequency = mutableMapOf<String, Int>()
        
        topEngagements.forEach { entity ->
            songMap[entity.songId]?.let { song ->
                artistFrequency[song.displayArtist] = artistFrequency.getOrDefault(song.displayArtist, 0) + entity.playCount
                val genre = song.genre
                if (!genre.isNullOrBlank()) {
                    genreFrequency[genre] = genreFrequency.getOrDefault(genre, 0) + entity.playCount
                }
            }
        }
        
        val topArtists = artistFrequency.entries.sortedByDescending { it.value }.take(7).map { it.key }
        val topGenres = genreFrequency.entries.sortedByDescending { it.value }.take(5).map { it.key }
        
        val recentTracks = recentEngagements.mapNotNull { entity ->
            songMap[entity.songId]?.let { "${it.title} by ${it.displayArtist}" }
        }.take(10)
        
        return JSONObject().apply {
            put("top_artists", JSONArray(topArtists))
            put("top_genres", JSONArray(topGenres))
            put("recently_played_vibe", JSONArray(recentTracks))
        }.toString(2)
    }
}
