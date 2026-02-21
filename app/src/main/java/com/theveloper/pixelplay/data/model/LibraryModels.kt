package com.theveloper.pixelplay.data.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Immutable
@Parcelize
data class Album(
    val id: Long, // MediaStore.Audio.Albums._ID
    val title: String,
    val artist: String,
    val year: Int,
    val albumArtUriString: String?,
    val songCount: Int
) : Parcelable {
    companion object {
        fun empty() = Album(
            id = -1,
            title = "",
            artist = "",
            year = 0,
            albumArtUriString = null,
            songCount = 0
        )
    }
}

@Immutable
@Parcelize
data class Artist(
    val id: Long, // MediaStore.Audio.Artists._ID
    val name: String,
    val songCount: Int,
    val imageUrl: String? = null, // Deezer artist image URL (from API)
    val customImageUri: String? = null // User-defined custom artist image (local file path)
) : Parcelable {
    companion object {
        fun empty() = Artist(
            id = -1,
            name = "",
            songCount = 0,
            imageUrl = null,
            customImageUri = null
        )
    }

    /** Returns the image URL/path to use, preferring the user's custom image. */
    val effectiveImageUrl: String?
        get() = customImageUri?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
}

/**
 * Represents a simplified artist reference for multi-artist support.
 * Used when displaying multiple artists for a song.
 */
@Serializable
@Immutable
@Parcelize
data class ArtistRef(
    val id: Long,
    val name: String,
    val isPrimary: Boolean = false
) : Parcelable

@Serializable
@Immutable
@Parcelize
data class ThemeColors(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val outlineVariant: Int,
    val inverseOnSurface: Int,
    val inverseSurface: Int,
    val inversePrimary: Int,
    val surfaceTint: Int,
    val surfaceContainerLowest: Int,
    val surfaceContainerLow: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int
) : Parcelable {
    fun toColorScheme(): androidx.compose.material3.ColorScheme {
        return androidx.compose.material3.ColorScheme(
            primary = androidx.compose.ui.graphics.Color(primary),
            onPrimary = androidx.compose.ui.graphics.Color(onPrimary),
            primaryContainer = androidx.compose.ui.graphics.Color(primaryContainer),
            onPrimaryContainer = androidx.compose.ui.graphics.Color(onPrimaryContainer),
            secondary = androidx.compose.ui.graphics.Color(secondary),
            onSecondary = androidx.compose.ui.graphics.Color(onSecondary),
            secondaryContainer = androidx.compose.ui.graphics.Color(secondaryContainer),
            onSecondaryContainer = androidx.compose.ui.graphics.Color(onSecondaryContainer),
            tertiary = androidx.compose.ui.graphics.Color(tertiary),
            onTertiary = androidx.compose.ui.graphics.Color(onTertiary),
            tertiaryContainer = androidx.compose.ui.graphics.Color(tertiaryContainer),
            onTertiaryContainer = androidx.compose.ui.graphics.Color(onTertiaryContainer),
            error = androidx.compose.ui.graphics.Color(error),
            onError = androidx.compose.ui.graphics.Color(onError),
            errorContainer = androidx.compose.ui.graphics.Color(errorContainer),
            onErrorContainer = androidx.compose.ui.graphics.Color(onErrorContainer),
            background = androidx.compose.ui.graphics.Color(background),
            onBackground = androidx.compose.ui.graphics.Color(onBackground),
            surface = androidx.compose.ui.graphics.Color(surface),
            onSurface = androidx.compose.ui.graphics.Color(onSurface),
            surfaceVariant = androidx.compose.ui.graphics.Color(surfaceVariant),
            onSurfaceVariant = androidx.compose.ui.graphics.Color(onSurfaceVariant),
            outline = androidx.compose.ui.graphics.Color(outline),
            outlineVariant = androidx.compose.ui.graphics.Color(outlineVariant),
            inverseOnSurface = androidx.compose.ui.graphics.Color(inverseOnSurface),
            inverseSurface = androidx.compose.ui.graphics.Color(inverseSurface),
            inversePrimary = androidx.compose.ui.graphics.Color(inversePrimary),
            surfaceTint = androidx.compose.ui.graphics.Color(surfaceTint),
            scrim = androidx.compose.ui.graphics.Color.Black, // Default
            surfaceBright = androidx.compose.ui.graphics.Color(surface), // Default
            surfaceDim = androidx.compose.ui.graphics.Color(surface), // Default
            surfaceContainerLowest = androidx.compose.ui.graphics.Color(surfaceContainerLowest),
            surfaceContainerLow = androidx.compose.ui.graphics.Color(surfaceContainerLow),
            surfaceContainer = androidx.compose.ui.graphics.Color(surfaceContainer),
            surfaceContainerHigh = androidx.compose.ui.graphics.Color(surfaceContainerHigh),
            surfaceContainerHighest = androidx.compose.ui.graphics.Color(surfaceContainerHighest)
        )
    }
}

@Serializable
@Immutable
@Parcelize
data class CustomTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val colors: ThemeColors,
    val isAmoled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable