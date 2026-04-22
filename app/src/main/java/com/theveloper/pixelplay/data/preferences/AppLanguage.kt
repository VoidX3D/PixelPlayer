package com.theveloper.pixelplay.data.preferences

object AppLanguage {
    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val SPANISH = "es"

    val supportedLanguageTags: Set<String> = setOf(SYSTEM, ENGLISH, SPANISH)

    fun normalize(languageTag: String): String {
        val normalized = languageTag.trim().lowercase()
        return if (normalized in supportedLanguageTags) normalized else SYSTEM
    }
}
