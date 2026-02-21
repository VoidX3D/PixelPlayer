package com.theveloper.pixelplay.data.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.theveloper.pixelplay.data.model.ThemeColors
import com.theveloper.pixelplay.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import android.graphics.Color
import timber.log.Timber

@Singleton
class AiThemeGenerator @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val json: Json
) {
    suspend fun generate(prompt: String, isDark: Boolean): Result<ThemeColors> {
        return try {
            val apiKey = userPreferencesRepository.geminiApiKey.first()
            if (apiKey.isBlank()) return Result.failure(Exception("API Key not set"))

            val generativeModel = GenerativeModel(
                modelName = userPreferencesRepository.geminiModel.first().ifBlank { "gemini-1.5-flash" },
                apiKey = apiKey
            )

            val systemPrompt = """
                You are a theme designer for a music player app.
                Generate a color palette for a ${if (isDark) "dark" else "light"} theme based on this prompt: "$prompt".
                The response MUST be ONLY a JSON object with the following hexadecimal color strings (including #):
                primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, error, onError, errorContainer, onErrorContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, outline, outlineVariant, inverseOnSurface, inverseSurface, inversePrimary, surfaceTint, surfaceContainerLowest, surfaceContainerLow, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest.

                Guidelines for ${if (isDark) "DARK" else "LIGHT"} theme:
                - Use harmonious colors.
                - Ensure high contrast for readability.
                - Follow Material Design 3 color roles.
                ${if (isDark) "- Background and Surface should be very dark (not necessarily pure black unless requested)." else "- Background and Surface should be very light."}
            """.trimIndent()

            val response = generativeModel.generateContent(systemPrompt)
            val text = response.text ?: return Result.failure(Exception("Empty response"))

            val jsonStart = text.indexOf("{")
            val jsonEnd = text.lastIndexOf("}") + 1
            if (jsonStart == -1 || jsonEnd == 0) return Result.failure(Exception("Invalid JSON in response"))

            val jsonString = text.substring(jsonStart, jsonEnd)
            val colorMap = json.decodeFromString<Map<String, String>>(jsonString)

            Result.success(parseColorMap(colorMap))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseColorMap(map: Map<String, String>): ThemeColors {
        fun p(key: String): Int = Color.parseColor(map[key] ?: "#000000")

        return ThemeColors(
            primary = p("primary"),
            onPrimary = p("onPrimary"),
            primaryContainer = p("primaryContainer"),
            onPrimaryContainer = p("onPrimaryContainer"),
            secondary = p("secondary"),
            onSecondary = p("onSecondary"),
            secondaryContainer = p("secondaryContainer"),
            onSecondaryContainer = p("onSecondaryContainer"),
            tertiary = p("tertiary"),
            onTertiary = p("onTertiary"),
            tertiaryContainer = p("tertiaryContainer"),
            onTertiaryContainer = p("onTertiaryContainer"),
            error = p("error"),
            onError = p("onError"),
            errorContainer = p("errorContainer"),
            onErrorContainer = p("onErrorContainer"),
            background = p("background"),
            onBackground = p("onBackground"),
            surface = p("surface"),
            onSurface = p("onSurface"),
            surfaceVariant = p("surfaceVariant"),
            onSurfaceVariant = p("onSurfaceVariant"),
            outline = p("outline"),
            outlineVariant = p("outlineVariant"),
            inverseOnSurface = p("inverseOnSurface"),
            inverseSurface = p("inverseSurface"),
            inversePrimary = p("inversePrimary"),
            surfaceTint = p("surfaceTint"),
            surfaceContainerLowest = p("surfaceContainerLowest"),
            surfaceContainerLow = p("surfaceContainerLow"),
            surfaceContainer = p("surfaceContainer"),
            surfaceContainerHigh = p("surfaceContainerHigh"),
            surfaceContainerHighest = p("surfaceContainerHighest")
        )
    }
}
