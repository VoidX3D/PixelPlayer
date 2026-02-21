package com.theveloper.pixelplay.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageManager @Inject constructor() {

    /**
     * Applies the specified language to the application.
     * @param languageCode The ISO 639-1 language code (e.g., "en", "es", "fr")
     *                     or "default" for system default.
     */
    fun applyLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = if (languageCode == "default") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    /**
     * Gets the currently active language code.
     */
    fun getCurrentLanguageCode(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags().ifEmpty { "default" }
    }

    /**
     * Supported languages mapping.
     */
    val supportedLanguages = mapOf(
        "default" to "System Default",
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "it" to "Italiano",
        "pt" to "Português",
        "ru" to "Русский",
        "zh" to "中文",
        "ja" to "日本語",
        "ko" to "한국어"
    )
}
