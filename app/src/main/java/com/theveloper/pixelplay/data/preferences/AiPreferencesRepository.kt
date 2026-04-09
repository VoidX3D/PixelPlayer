package com.theveloper.pixelplay.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """
            You are 'Vibe-Engine v2', a professional music curator in a premium Android music player.
            Your role: Convert fuzzy user requests into strict JSON playlist structures.
            
            RULES:
            1. Output MUST be ONLY a JSON array of song IDs. NO markdown, NO text, NO conversational filler.
            2. Match user vibes/genres while staying within their 'Recently Played' affinity.
            3. If a request is unclear, favor 'Vibe-Engine' best-best matches.
            4. Example: ["id1", "id2", "id3"]
        """.trimIndent()

        const val DEFAULT_PLAYLIST_SYSTEM_PROMPT = """
            You are a hyper-intelligent music curator. You analyze a user's Listening Profile Digest and their current request to create a matching playlist.
            
            INPUT:
            - User Listening Profile (JSON)
            - Candidate Song List (JSON)
            - Request Prompt (String)
            
            GOAL:
            - Select the most relevant song IDs from the 'Candidate Song List'.
            - Maximize enjoyment by favoring Artists/Genres listed in the 'User Listening Profile'.
            - Response MUST be raw JSON: ["id1", "id2", "id3"]
        """.trimIndent()
        
        const val DEFAULT_METADATA_SYSTEM_PROMPT = """
            You are a music metadata specialist. Complete missing song fields with 99.9% accuracy.
            Response MUST be exactly this JSON schema: {"title": "", "artist": "", "album": "", "genre": ""}
            Only one genre per song. No conversational text.
        """.trimIndent()
    }

    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val GEMINI_SYSTEM_PROMPT = stringPreferencesKey("gemini_system_prompt")
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val DEEPSEEK_API_KEY = stringPreferencesKey("deepseek_api_key")
        val DEEPSEEK_MODEL = stringPreferencesKey("deepseek_model")
        val DEEPSEEK_SYSTEM_PROMPT = stringPreferencesKey("deepseek_system_prompt")
        
        val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
        val GROQ_MODEL = stringPreferencesKey("groq_model")
        val GROQ_SYSTEM_PROMPT = stringPreferencesKey("groq_system_prompt")
        
        val MISTRAL_API_KEY = stringPreferencesKey("mistral_api_key")
        val MISTRAL_MODEL = stringPreferencesKey("mistral_model")
        val MISTRAL_SYSTEM_PROMPT = stringPreferencesKey("mistral_system_prompt")
    }

    val geminiApiKey: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.GEMINI_API_KEY] ?: "" }

    val geminiModel: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.GEMINI_MODEL] ?: "" }

    val geminiSystemPrompt: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[Keys.GEMINI_SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT
        }

    val aiProvider: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.AI_PROVIDER] ?: "GEMINI" }

    val deepseekApiKey: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.DEEPSEEK_API_KEY] ?: "" }

    val deepseekModel: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.DEEPSEEK_MODEL] ?: "" }

    val deepseekSystemPrompt: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[Keys.DEEPSEEK_SYSTEM_PROMPT] ?: DEFAULT_DEEPSEEK_SYSTEM_PROMPT
        }

    val groqApiKey: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.GROQ_API_KEY] ?: "" }

    val groqModel: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.GROQ_MODEL] ?: "" }

    val groqSystemPrompt: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[Keys.GROQ_SYSTEM_PROMPT] ?: DEFAULT_GROQ_SYSTEM_PROMPT
        }

    val mistralApiKey: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.MISTRAL_API_KEY] ?: "" }

    val mistralModel: Flow<String> =
        dataStore.data.map { preferences -> preferences[Keys.MISTRAL_MODEL] ?: "" }

    val mistralSystemPrompt: Flow<String> =
        dataStore.data.map { preferences ->
            preferences[Keys.MISTRAL_SYSTEM_PROMPT] ?: DEFAULT_MISTRAL_SYSTEM_PROMPT
        }

    suspend fun setGeminiApiKey(apiKey: String) {
        dataStore.edit { preferences -> preferences[Keys.GEMINI_API_KEY] = apiKey }
    }

    suspend fun setGeminiModel(model: String) {
        dataStore.edit { preferences -> preferences[Keys.GEMINI_MODEL] = model }
    }

    suspend fun setGeminiSystemPrompt(prompt: String) {
        dataStore.edit { preferences -> preferences[Keys.GEMINI_SYSTEM_PROMPT] = prompt }
    }

    suspend fun resetGeminiSystemPrompt() {
        dataStore.edit { preferences ->
            preferences[Keys.GEMINI_SYSTEM_PROMPT] = DEFAULT_SYSTEM_PROMPT
        }
    }

    suspend fun setAiProvider(provider: String) {
        dataStore.edit { preferences -> preferences[Keys.AI_PROVIDER] = provider }
    }

    suspend fun setDeepseekApiKey(apiKey: String) {
        dataStore.edit { preferences -> preferences[Keys.DEEPSEEK_API_KEY] = apiKey }
    }

    suspend fun setDeepseekModel(model: String) {
        dataStore.edit { preferences -> preferences[Keys.DEEPSEEK_MODEL] = model }
    }

    suspend fun setDeepseekSystemPrompt(prompt: String) {
        dataStore.edit { preferences -> preferences[Keys.DEEPSEEK_SYSTEM_PROMPT] = prompt }
    }

    suspend fun resetDeepseekSystemPrompt() {
        dataStore.edit { preferences ->
            preferences[Keys.DEEPSEEK_SYSTEM_PROMPT] = DEFAULT_DEEPSEEK_SYSTEM_PROMPT
        }
    }

    suspend fun setGroqApiKey(apiKey: String) {
        dataStore.edit { preferences -> preferences[Keys.GROQ_API_KEY] = apiKey }
    }

    suspend fun setGroqModel(model: String) {
        dataStore.edit { preferences -> preferences[Keys.GROQ_MODEL] = model }
    }

    suspend fun setGroqSystemPrompt(prompt: String) {
        dataStore.edit { preferences -> preferences[Keys.GROQ_SYSTEM_PROMPT] = prompt }
    }

    suspend fun resetGroqSystemPrompt() {
        dataStore.edit { preferences ->
            preferences[Keys.GROQ_SYSTEM_PROMPT] = DEFAULT_GROQ_SYSTEM_PROMPT
        }
    }

    suspend fun setMistralApiKey(apiKey: String) {
        dataStore.edit { preferences -> preferences[Keys.MISTRAL_API_KEY] = apiKey }
    }

    suspend fun setMistralModel(model: String) {
        dataStore.edit { preferences -> preferences[Keys.MISTRAL_MODEL] = model }
    }

    suspend fun setMistralSystemPrompt(prompt: String) {
        dataStore.edit { preferences -> preferences[Keys.MISTRAL_SYSTEM_PROMPT] = prompt }
    }

    suspend fun resetMistralSystemPrompt() {
        dataStore.edit { preferences ->
            preferences[Keys.MISTRAL_SYSTEM_PROMPT] = DEFAULT_MISTRAL_SYSTEM_PROMPT
        }
    }
}
