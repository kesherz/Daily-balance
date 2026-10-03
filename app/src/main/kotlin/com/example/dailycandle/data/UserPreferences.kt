package com.example.dailycandle.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsStore by preferencesDataStore("settings")
enum class ThemePreference { SYSTEM, LIGHT, DARK }

class UserPreferences(context: Context) {
    private val store = context.settingsStore
    private val themeKey = stringPreferencesKey("theme")
    val theme = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { data ->
        ThemePreference.entries.firstOrNull { it.name == data[themeKey] } ?: ThemePreference.SYSTEM
    }

    suspend fun setTheme(theme: ThemePreference) { store.edit { it[themeKey] = theme.name } }
}
