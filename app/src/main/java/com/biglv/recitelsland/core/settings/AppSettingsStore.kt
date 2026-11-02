package com.biglv.recitelsland.core.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

enum class ThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: System
    }
}

class AppSettingsStore(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { preferences ->
        ThemeMode.fromRaw(preferences[KEY_THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.name
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
