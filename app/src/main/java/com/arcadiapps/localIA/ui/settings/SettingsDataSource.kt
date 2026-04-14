package com.arcadiapps.localIA.ui.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkMode = DarkModeOption.valueOf(
                prefs[SettingsViewModel.KEY_DARK_MODE] ?: DarkModeOption.SYSTEM.name
            ),
            systemPrompt = prefs[SettingsViewModel.KEY_SYSTEM_PROMPT] ?: "",
            temperature = prefs[SettingsViewModel.KEY_TEMPERATURE] ?: 0.8f,
            maxTokens = prefs[SettingsViewModel.KEY_MAX_TOKENS] ?: 1024,
            topK = prefs[SettingsViewModel.KEY_TOP_K] ?: 40,
            streamingEnabled = prefs[SettingsViewModel.KEY_STREAMING] ?: true,
            keepModelLoaded = prefs[SettingsViewModel.KEY_KEEP_LOADED] ?: true
        )
    }
}
