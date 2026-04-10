package com.theveloper.pixelplay.data.ai


import com.theveloper.pixelplay.data.database.LocalPlaylistDao
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.stats.PlaybackStatsRepository
import com.theveloper.pixelplay.data.stats.StatsTimeRange
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileDigestGenerator @Inject constructor(
    private val statsRepository: PlaybackStatsRepository,
    private val playlistDao: LocalPlaylistDao
) {
    private val SAFE_TARGET_CHAR_LIMIT = 15000 // Approx 3.5k-4k tokens
    private val MAX_TARGET_CHAR_LIMIT = 150000 // Large context for discovery
    /**
     * Computes a highly condensed representation of the user's listening profile.
     * Uses a compact key-value format to minimize token consumption while maximizing signal.
     */
    suspend fun generateDigest(allSongs: List<Song>, isSafeLimit: Boolean = true): String {
        val targetLimit = if (isSafeLimit) SAFE_TARGET_CHAR_LIMIT else MAX_TARGET_CHAR_LIMIT
        val summary = statsRepository.loadSummary(StatsTimeRange.ALL, allSongs)
        val recentSummary = statsRepository.loadSummary(StatsTimeRange.WEEK, allSongs)
        val playlists = playlistDao.observePlaylistsWithSongs().first()
        
        val sb = StringBuilder()
        sb.append("USER_PROFILE\n")
        
        // --- 1. Behavioral & Pattern Metrics ---
        sb.append("STATS: total_plays=${summary.totalPlayCount}, unique_songs=${summary.uniqueSongs}\n")
        sb.append("TOP_GENRES: ${summary.topGenres.take(5).joinToString(",") { it.genre }}\n")
        sb.append("TOP_ARTISTS: ${summary.topArtists.take(5).joinToString(",") { it.artist }}\n")
        
        summary.dayListeningDistribution?.let { dist ->
            val phases = dist.buckets.groupBy { bucket ->
                val hour = bucket.startMinute / 60
                when (hour) {
                    in 5..10 -> "Morning"
                    in 11..16 -> "Afternoon"
                    in 17..22 -> "Evening"
                    else -> "Night"
                }
            }.mapValues { it.value.sumOf { b -> b.totalDurationMs } }
            sb.append("PHASE: ${phases.maxByOrNull { it.value }?.key ?: "Unknown"}\n")
        }
        
        sb.append("DYNAMICS: variety=${"%.2f".format(if (summary.totalPlayCount > 0) summary.uniqueSongs.toDouble() / summary.totalPlayCount else 0.0)}\n")
        
        val playlistLimit = if (isSafeLimit) 10 else 50
        sb.append("PLAYLISTS: ${playlists.take(playlistLimit).joinToString(",") { it.playlist.name }}\n")
        
        // --- 2. Listened Tracks (Deep Stats) ---
        // Format: ID | PLAYS | TIME(m) | FAV(0/1) | TITLE - ARTIST
        // We use a compact format to pack as much as possible
        sb.append("\nLISTENED_TRACKS_KEY: id|p(plays)|d(min)|f(fav)|meta\n")
        
        val songMap = allSongs.associateBy { it.id }
        val playedSongs = summary.songs.takeWhile { sb.length < targetLimit * 0.7 }
        
        playedSongs.forEach { s ->
            val song = songMap[s.songId]
            val fav = if (song?.isFavorite == true) "1" else "0"
            val mins = s.totalDurationMs / 60000
            sb.append("${s.songId}|${s.playCount}|$mins|$fav|${s.title}-${s.artist}\n")
        }
        
        // --- 3. Discovery Pool (Unplayed Gems) ---
        // AI must know what is available but never played
        sb.append("\nDISCOVERY_POOL (Unplayed):\n")
        val playedIds = summary.songs.map { it.songId }.toSet()
        val unplayed = allSongs.filter { it.id !in playedIds }
            .shuffled()
            .takeWhile { sb.length < targetLimit }
        
        unplayed.forEach { s ->
            val fav = if (s.isFavorite) "1" else "0"
            sb.append("${s.id}|0|0|$fav|${s.title}-${s.displayArtist}\n")
        }
        
        return sb.toString()
    }
}
