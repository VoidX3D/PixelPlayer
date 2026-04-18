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
class MetadataPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val LASTFM_API_KEY = stringPreferencesKey("lastfm_api_key")
        val MUSICBRAINZ_API_KEY = stringPreferencesKey("musicbrainz_api_key")
    }

    val lastFmApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.LASTFM_API_KEY] ?: ""
    }

    val musicBrainzApiKey: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.MUSICBRAINZ_API_KEY] ?: ""
    }

    suspend fun setLastFmApiKey(apiKey: String) {
        dataStore.edit { preferences -> preferences[Keys.LASTFM_API_KEY] = apiKey }
    }

    suspend fun setMusicBrainzApiKey(apiKey: String) {
        dataStore.edit { preferences -> preferences[Keys.MUSICBRAINZ_API_KEY] = apiKey }
    }
}
