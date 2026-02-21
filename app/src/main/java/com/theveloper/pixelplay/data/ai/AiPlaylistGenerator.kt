package com.theveloper.pixelplay.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.theveloper.pixelplay.data.DailyMixManager
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.max

class AiPlaylistGenerator @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailyMixManager: DailyMixManager,
    private val json: Json
) {
    private val promptCache: MutableMap<String, List<String>> = object : LinkedHashMap<String, List<String>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean = size > 20
    }

    suspend fun generate(
        userPrompt: String,
        allSongs: List<Song>,
        minLength: Int,
        maxLength: Int,
        candidateSongs: List<Song>? = null
    ): Result<List<Song>> {
        return try {
            val apiKey = userPreferencesRepository.geminiApiKey.first()
            if (apiKey.isBlank()) {
                return Result.failure(Exception("API Key not configured."))
            }

            val normalizedPrompt = userPrompt.trim().lowercase()
            promptCache[normalizedPrompt]?.let { cachedIds ->
                val songMap = allSongs.associateBy { it.id }
                val cachedSongs = cachedIds.mapNotNull { songMap[it] }
                if (cachedSongs.isNotEmpty()) {
                    return Result.success(cachedSongs)
                }
            }

            val selectedModel = userPreferencesRepository.geminiModel.first()
            val modelName = selectedModel.ifEmpty { "" }

            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )

            val samplingPool = when {
                candidateSongs.isNullOrEmpty().not() -> candidateSongs ?: allSongs
                else -> {
                    // Prefer a cost-aware ranked list before falling back to the whole library
                    val rankedForPrompt = dailyMixManager.generateDailyMix(
                        allSongs = allSongs,
                        favoriteSongIds = emptySet(),
                        limit = 200
                    )
                    if (rankedForPrompt.isNotEmpty()) rankedForPrompt else allSongs
                }
            }

            // To optimize cost, cap the context size and shuffle it a bit for diversity
            // Reduced max sample size for faster processing
            val sampleSize = max(minLength, 60).coerceAtMost(120)
            val songSample = samplingPool.shuffled().take(sampleSize)

            val availableSongsJson = songSample.joinToString(separator = ",\n") { song ->
                // Compact JSON to reduce token usage and improve speed
                """{"id":"${song.id}","t":"${song.title.take(30).replace("\"", "'")}","a":"${song.displayArtist.take(20).replace("\"", "'")}","g":"${song.genre?.take(15)?.replace("\"", "'") ?: "u"}"}"""
            }

            // Get the custom system prompt from user preferences
            val customSystemPrompt = userPreferencesRepository.geminiSystemPrompt.first()

            // Build the task-specific instructions
            val taskInstructions = """
            Your task is to create a playlist for a user based on their prompt.
            You will be given a user's request, a desired playlist length range, and a list of available songs with their metadata.

            Instructions:
            1. Analyze prompt mood/genre/theme.
            2. Select matching songs from list.
            3. Min length: $minLength, Max length: $maxLength.
            4. Response MUST be ONLY a valid JSON array of song IDs. No markdown, no explanations.

            Example response for a playlist of 3 songs:
            ["song_id_1", "song_id_2", "song_id_3"]
            """.trimIndent()

            val fullPrompt = """
            
            $taskInstructions
            
            $customSystemPrompt
            
            User's request: "$userPrompt"
            Minimum playlist length: $minLength
            Maximum playlist length: $maxLength
            Available songs:
            [
            $availableSongsJson
            ]
            """.trimIndent()

            val response = generativeModel.generateContent(fullPrompt)
            val responseText = response.text ?: return Result.failure(Exception("AI returned an empty response."))

            val songIds = extractPlaylistSongIds(responseText)

            // Map the returned IDs to the actual Song objects
            val songMap = allSongs.associateBy { it.id }
            val generatedPlaylist = songIds.mapNotNull { songMap[it] }

            if (generatedPlaylist.isNotEmpty()) {
                promptCache[normalizedPrompt] = generatedPlaylist.map { it.id }
            }

            Result.success(generatedPlaylist)

        } catch (e: IllegalArgumentException) {
            Result.failure(Exception(e.message ?: "AI response did not contain a valid playlist."))
        } catch (e: Exception) {
            Result.failure(Exception("AI Error: ${e.message}"))
        }
    }

    private fun extractPlaylistSongIds(rawResponse: String): List<String> {
        val sanitized = rawResponse
            .replace("```json", "")
            .replace("```", "")
            .trim()

        for (startIndex in sanitized.indices) {
            if (sanitized[startIndex] != '[') continue

            var depth = 0
            var inString = false
            var isEscaped = false

            for (index in startIndex until sanitized.length) {
                val character = sanitized[index]

                if (inString) {
                    if (isEscaped) {
                        isEscaped = false
                        continue
                    }

                    when (character) {
                        '\\' -> isEscaped = true
                        '"' -> inString = false
                    }
                    continue
                }

                when (character) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) {
                            val candidate = sanitized.substring(startIndex, index + 1)
                            val decoded = runCatching { json.decodeFromString<List<String>>(candidate) }
                            if (decoded.isSuccess) {
                                return decoded.getOrThrow()
                            }
                            break
                        }
                    }
                }
            }
        }

        throw IllegalArgumentException("AI response did not contain a valid playlist.")
    }
}
