package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "locale_settings")

class LocalePreferences(private val context: Context) {

    companion object {
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
        const val DEFAULT_LANG = "en"
    }

    val languageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_CODE] ?: DEFAULT_LANG
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { prefs ->
            prefs[LANGUAGE_CODE] = code
        }
    }
}
