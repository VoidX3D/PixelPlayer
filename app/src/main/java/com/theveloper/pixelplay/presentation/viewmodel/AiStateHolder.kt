package com.theveloper.pixelplay.presentation.viewmodel

import android.content.Context
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.DailyMixManager
import com.theveloper.pixelplay.data.ai.AiErrorHandler
import com.theveloper.pixelplay.data.ai.AiNotificationManager
import com.theveloper.pixelplay.data.ai.AiPlaylistGenerator
import com.theveloper.pixelplay.data.ai.AiSystemPromptType
import com.theveloper.pixelplay.data.preferences.PlaylistPreferencesRepository
import com.theveloper.pixelplay.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiStateHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiPlaylistGenerator: AiPlaylistGenerator,
    private val dailyMixManager: DailyMixManager,
    private val playlistPreferencesRepository: PlaylistPreferencesRepository,
    private val dailyMixStateHolder: DailyMixStateHolder,
    private val notificationManager: AiNotificationManager,
    private val errorHandler: AiErrorHandler,
    private val handler: com.theveloper.pixelplay.data.ai.AiHandler
) {
    private val _showAiPlaylistSheet = MutableStateFlow(false)
    val showAiPlaylistSheet = _showAiPlaylistSheet.asStateFlow()

    private val _isGeneratingAiPlaylist = MutableStateFlow(false)
    val isGeneratingAiPlaylist = _isGeneratingAiPlaylist.asStateFlow()

    private val _aiSuccess = MutableStateFlow(false)
    val aiSuccess = _aiSuccess.asStateFlow()

    private val _aiStatus = MutableStateFlow<String?>(null)
    val aiStatus = _aiStatus.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError = _aiError.asStateFlow()

    private var _lastPlaylistPrompt: String? = null
    private var _lastMinLength: Int = 5
    private var _lastMaxLength: Int = 15

    private var scope: CoroutineScope? = null
    private var allSongsProvider: (suspend () -> List<Song>)? = null
    private var favoriteSongIdsProvider: (() -> Set<String>)? = null

    private var toastEmitter: ((String) -> Unit)? = null
    private var playSongsCallback: ((List<Song>, Song, String) -> Unit)? = null
    private var openPlayerSheetCallback: (() -> Unit)? = null

    private val titleStopWords = setOf(
        "a", "an", "the", "and", "or", "for", "to", "of", "in", "on", "with", "by", "from",
        "de", "la", "el", "los", "las", "y", "o", "para", "con", "por", "del", "al", "un", "una",
        "core", "request", "mood", "target", "activity", "context", "era", "focus", "prioritize",
        "genres", "avoid", "preferred", "language", "energy", "level", "discovery", "where",
        "familiar", "deep", "cuts", "keep", "transitions", "smooth", "repetitive", "artist",
        "clustering", "songs", "listener", "favorites", "explicit", "lyrics", "alternatives",
        "whenever", "possible"
    )

    fun initialize(
        scope: CoroutineScope,
        allSongsProvider: suspend () -> List<Song>,
        favoriteSongIdsProvider: () -> Set<String>,
        toastEmitter: (String) -> Unit,
        playSongsCallback: (List<Song>, Song, String) -> Unit,
        openPlayerSheetCallback: () -> Unit
    ) {
        this.scope = scope
        this.allSongsProvider = allSongsProvider
        this.favoriteSongIdsProvider = favoriteSongIdsProvider
        this.toastEmitter = toastEmitter
        this.playSongsCallback = playSongsCallback
        this.openPlayerSheetCallback = openPlayerSheetCallback
    }

    fun showAiPlaylistSheet() {
        _showAiPlaylistSheet.value = true
    }

    fun dismissAiPlaylistSheet() {
        _showAiPlaylistSheet.value = false
        _aiError.value = null
        _aiSuccess.value = false
        _isGeneratingAiPlaylist.value = false
        _aiStatus.value = null
    }

    fun retryLastPlaylistGeneration() {
        val prompt = _lastPlaylistPrompt ?: return
        generateAiPlaylist(prompt, _lastMinLength, _lastMaxLength)
    }

    fun clearAiPlaylistError() {
        _aiError.value = null
    }

    fun generateAiPlaylist(
        prompt: String,
        minLength: Int,
        maxLength: Int,
        saveAsPlaylist: Boolean = false,
        playlistName: String? = null
    ) {
        _lastPlaylistPrompt = prompt
        _lastMinLength = minLength
        _lastMaxLength = maxLength

        val scope = this.scope ?: return

        scope.launch {
            val allSongs = allSongsProvider?.invoke() ?: emptyList()
            val favoriteIds = favoriteSongIdsProvider?.invoke() ?: emptySet()
            _isGeneratingAiPlaylist.value = true
            _aiError.value = null
            _aiSuccess.value = false

            try {
                _aiStatus.value = "Analyzing your library stats..."
                val existingPlaylistNames = playlistPreferencesRepository.userPlaylistsFlow.first()
                    .map { it.name.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()

                _aiStatus.value = "Selecting best candidates..."
                val candidatePool = dailyMixManager.generateDailyMix(
                    allSongs = allSongs,
                    favoriteSongIds = favoriteIds,
                    limit = 120
                )

                _aiStatus.value = "Consulting the Daily Mix guide..."
                notificationManager.showProgress("AI Curation", "Synthesizing your Daily Mix...", 50)

                val result = aiPlaylistGenerator.generate(
                    userPrompt = prompt,
                    allSongs = allSongs,
                    minLength = minLength,
                    maxLength = maxLength,
                    candidateSongs = candidatePool
                )

                result.onSuccess { generatedSongs ->
                    if (generatedSongs.isNotEmpty()) {
                        if (saveAsPlaylist) {
                            val resolvedPlaylistName = resolveAiPlaylistName(
                                requestedName = playlistName,
                                prompt = prompt,
                                existingNames = existingPlaylistNames
                            )
                            val songIds = generatedSongs.map { it.id }
                            playlistPreferencesRepository.createPlaylist(
                                name = resolvedPlaylistName,
                                songIds = songIds,
                                isAiGenerated = true
                            )
                            _aiStatus.value = "Success! Your mix is ready."
                            _aiSuccess.value = true
                            notificationManager.showCompletion("Generation Complete", "Your AI Mix is ready to play.")
                            toastEmitter?.invoke("AI Playlist created!")
                            kotlinx.coroutines.delay(1200)
                            dismissAiPlaylistSheet()
                        } else {
                            _aiStatus.value = "Starting playback..."
                            _aiSuccess.value = true
                            notificationManager.showCompletion("Generation Complete", "Starting your personalized session.")
                            dailyMixStateHolder.setDailyMixSongs(generatedSongs)
                            playSongsCallback?.invoke(generatedSongs, generatedSongs.first(), "AI: $prompt")
                            openPlayerSheetCallback?.invoke()
                            kotlinx.coroutines.delay(800)
                            dismissAiPlaylistSheet()
                        }
                    } else {
                        _aiStatus.value = null
                        _aiError.value = context.getString(R.string.ai_no_songs_found)
                        notificationManager.hideProgress()
                    }
                }.onFailure { error ->
                    Timber.tag("AiPlaylist").e(error, "AI playlist generation failed")
                    _aiStatus.value = null
                    _aiError.value = errorHandler.resolveErrorMessage(error)
                    notificationManager.showCompletion("Generation Failed", errorHandler.extractDetail(error).take(140))
                }
            } catch (e: Exception) {
                Timber.tag("AiPlaylist").e(e, "AI playlist generation threw unhandled exception")
                _aiStatus.value = null
                _aiError.value = errorHandler.resolveErrorMessage(e)
                notificationManager.showCompletion("Generation Failed", errorHandler.extractDetail(e).take(140))
            } finally {
                _isGeneratingAiPlaylist.value = false
                _aiStatus.value = null
            }
        }
    }

    fun regenerateDailyMixWithPrompt(prompt: String) {
        val scope = this.scope ?: return
        val currentDailyMixSongs = dailyMixStateHolder.dailyMixSongs.value

        scope.launch {
            val allSongs = allSongsProvider?.invoke() ?: emptyList()
            val favoriteIds = favoriteSongIdsProvider?.invoke() ?: emptySet()
            if (prompt.isBlank()) {
                toastEmitter?.invoke(context.getString(R.string.ai_prompt_empty))
                return@launch
            }

            _isGeneratingAiPlaylist.value = true
            _aiError.value = null

            try {
                _aiStatus.value = "Refining your Daily Mix..."
                val desiredSize = currentDailyMixSongs.size.takeIf { it > 0 } ?: 25
                val minLength = (desiredSize * 0.6).toInt().coerceAtLeast(10)
                val maxLength = desiredSize.coerceAtLeast(20)

                _aiStatus.value = "Scanning for vibes..."
                val candidatePool = dailyMixManager.generateDailyMix(
                    allSongs = allSongs,
                    favoriteSongIds = favoriteIds,
                    limit = 100
                )

                _aiStatus.value = "Applying AI filters..."
                val result = aiPlaylistGenerator.generate(
                    userPrompt = prompt,
                    allSongs = allSongs,
                    minLength = minLength,
                    maxLength = maxLength,
                    candidateSongs = candidatePool, type = AiSystemPromptType.DAILY_MIX
                )

                result.onSuccess { generatedSongs ->
                    if (generatedSongs.isNotEmpty()) {
                        dailyMixStateHolder.setDailyMixSongs(generatedSongs)
                        toastEmitter?.invoke(context.getString(R.string.ai_daily_mix_updated))
                    } else {
                        toastEmitter?.invoke(context.getString(R.string.ai_no_songs_for_mix))
                    }
                }.onFailure { error ->
                    Timber.tag("AiPlaylist").e(error, "Daily Mix refinement failed")
                    _aiError.value = errorHandler.resolveErrorMessage(error)
                    toastEmitter?.invoke(context.getString(R.string.could_not_update, errorHandler.extractDetail(error)))
                }
            } catch (e: Exception) {
                Timber.tag("AiPlaylist").e(e, "Daily Mix refinement threw unhandled exception")
                _aiError.value = errorHandler.resolveErrorMessage(e)
                toastEmitter?.invoke(context.getString(R.string.could_not_update, errorHandler.extractDetail(e)))
            } finally {
                _isGeneratingAiPlaylist.value = false
                _aiStatus.value = null
            }
        }
    }

    suspend fun translateLyrics(lyricsText: String): Result<String> {
        return try {
            val targetLanguage = context.resources.configuration.locales[0].displayLanguage
            val prompt = """
Translate the provided song lyrics into $targetLanguage.

Keep every timestamp exactly unchanged.

If the lyrics are ALREADY mostly in $targetLanguage, output ONLY the exact phrase "ALREADY_IN_TARGET_LANGUAGE" without any other text.

For each original line, output the original line first, then on the next line output the $targetLanguage translation with the same timestamp.

Do not add any extra text, explanations, numbering, labels, or formatting.
Do not remove, merge, split, or reorder lines.

Output only:
[timestamp] original text
[timestamp] translated text

Lyrics to translate:
$lyricsText
            """.trimIndent()

            val response = handler.generateContent(
                prompt = prompt,
                type = AiSystemPromptType.GENERAL,
                temperature = 0.1f
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun onCleared() {
        scope = null
        allSongsProvider = null
        favoriteSongIdsProvider = null
        toastEmitter = null
        playSongsCallback = null
        openPlayerSheetCallback = null
    }

    private fun resolveAiPlaylistName(
        requestedName: String?,
        prompt: String,
        existingNames: Set<String>
    ): String {
        val normalizedExisting = existingNames.map { it.lowercase() }.toSet()
        val baseName = requestedName?.trim().takeUnless { it.isNullOrEmpty() }
            ?: generateShortAiTitle(prompt)

        var candidate = baseName.ifBlank { "AI Mix" }
        if (candidate.lowercase() !in normalizedExisting) {
            return candidate
        }

        var counter = 2
        while ("$candidate $counter".lowercase() in normalizedExisting) {
            counter++
        }
        return "$candidate $counter"
    }

    private fun generateShortAiTitle(prompt: String): String {
        val coreRequest = Regex("(?i)core request:\\s*([^.]*)")
            .find(prompt)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            .orEmpty()

        val source = if (coreRequest.isNotBlank()) coreRequest else prompt
        val normalizedText = source
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val tokens = normalizedText
            .split(" ")
            .filter { token ->
                token.length >= 3 && token !in titleStopWords
            }

        val compactTitle = when {
            tokens.size >= 2 -> tokens.take(2).joinToString(" ")
            tokens.size == 1 -> "${tokens.first()} mix"
            else -> fallbackTitleByKeyword(normalizedText)
        }

        return compactTitle
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            .split(" ")
            .joinToString(" ") { part ->
                part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
            }
            .take(26)
            .trim()
            .ifBlank { "AI Mix" }
    }

    private fun fallbackTitleByKeyword(text: String): String {
        return when {
            listOf("workout", "gym", "run", "cardio").any { text.contains(it) } -> "Workout Mix"
            listOf("focus", "study", "work", "productivity").any { text.contains(it) } -> "Focus Flow"
            listOf("chill", "relax", "calm", "lofi").any { text.contains(it) } -> "Chill Vibes"
            listOf("party", "dance", "club").any { text.contains(it) } -> "Party Mix"
            listOf("night", "late", "sleep").any { text.contains(it) } -> "Night Vibes"
            listOf("road", "trip", "drive").any { text.contains(it) } -> "Road Trip"
            listOf("romantic", "love").any { text.contains(it) } -> "Love Notes"
            listOf("sad", "melancholic").any { text.contains(it) } -> "Blue Hour"
            else -> "Fresh Mix"
        }
    }
}
