package com.theveloper.pixelplay.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.theveloper.pixelplay.data.model.Lyrics
import com.theveloper.pixelplay.data.model.SyncedLine
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import com.theveloper.pixelplay.utils.LyricsUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject

class AiLyricsTranslator @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val json: Json
) {
    suspend fun translate(
        lyrics: Lyrics,
        targetLanguage: String
    ): Result<Lyrics> {
        return try {
            val apiKey = userPreferencesRepository.geminiApiKey.first()
            if (apiKey.isBlank()) {
                return Result.failure(Exception("API Key not configured."))
            }

            val selectedModel = userPreferencesRepository.geminiModel.first()
            val modelName = selectedModel.ifEmpty { "gemini-1.5-flash" }

            val generativeModel = GenerativeModel(
                modelName = modelName,
                apiKey = apiKey
            )

            val plainText = lyrics.plain?.joinToString("\n") ?: ""
            val syncedLines = lyrics.synced ?: emptyList()

            if (plainText.isBlank() && syncedLines.isEmpty()) {
                return Result.failure(Exception("No lyrics to translate."))
            }

            val contentToTranslate = if (syncedLines.isNotEmpty()) {
                syncedLines.joinToString("\n") { "${it.time}|${it.line}" }
            } else {
                plainText
            }

            val systemPrompt = """
            You are a professional song lyrics translator. Your task is to translate the following lyrics into $targetLanguage.
            Maintain the emotional tone and poetic structure of the original lyrics.
            If the input contains timestamps in the format 'time|line', preserve the EXACT timestamps and only translate the text after the '|'.
            Output ONLY the translated lyrics, no explanations or additional text.
            """.trimIndent()

            val response = generativeModel.generateContent("$systemPrompt\n\nLyrics to translate:\n$contentToTranslate")
            val translatedText = response.text

            if (translatedText.isNullOrBlank()) {
                return Result.failure(Exception("AI returned an empty translation."))
            }

            val translatedLyrics = if (syncedLines.isNotEmpty()) {
                val newSyncedLines = translatedText.lines().mapNotNull { line ->
                    val parts = line.split("|", limit = 2)
                    if (parts.size == 2) {
                        val time = parts[0].trim().toIntOrNull()
                        if (time != null) {
                            SyncedLine(time = time, line = parts[1].trim())
                        } else null
                    } else null
                }
                Lyrics(
                    plain = newSyncedLines.map { it.line },
                    synced = newSyncedLines,
                    areFromRemote = true
                )
            } else {
                val newPlainLines = translatedText.lines().filter { it.isNotBlank() }
                Lyrics(
                    plain = newPlainLines,
                    synced = null,
                    areFromRemote = true
                )
            }

            Result.success(translatedLyrics)
        } catch (e: Exception) {
            Timber.e(e, "Error in AiLyricsTranslator")
            Result.failure(e)
        }
    }
}
