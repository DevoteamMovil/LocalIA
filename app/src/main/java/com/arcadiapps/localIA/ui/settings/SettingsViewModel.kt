package com.arcadiapps.localIA.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val systemPrompt: String = "",
    val temperature: Float = 0.8f,
    val maxTokens: Int = 1024,
    val topK: Int = 40,
    val streamingEnabled: Boolean = true,
    val keepModelLoaded: Boolean = true
)

enum class DarkModeOption { LIGHT, DARK, SYSTEM }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        val KEY_SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val KEY_TEMPERATURE = floatPreferencesKey("temperature")
        val KEY_MAX_TOKENS = intPreferencesKey("max_tokens")
        val KEY_TOP_K = intPreferencesKey("top_k")
        val KEY_STREAMING = booleanPreferencesKey("streaming")
        val KEY_KEEP_LOADED = booleanPreferencesKey("keep_loaded")
    }

    val settings: StateFlow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            darkMode = DarkModeOption.valueOf(prefs[KEY_DARK_MODE] ?: DarkModeOption.SYSTEM.name),
            systemPrompt = prefs[KEY_SYSTEM_PROMPT] ?: "",
            temperature = prefs[KEY_TEMPERATURE] ?: 0.8f,
            maxTokens = prefs[KEY_MAX_TOKENS] ?: 1024,
            topK = prefs[KEY_TOP_K] ?: 40,
            streamingEnabled = prefs[KEY_STREAMING] ?: true,
            keepModelLoaded = prefs[KEY_KEEP_LOADED] ?: true
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setDarkMode(option: DarkModeOption) = save { it[KEY_DARK_MODE] = option.name }
    fun setSystemPrompt(prompt: String) = save { it[KEY_SYSTEM_PROMPT] = prompt }
    fun setTemperature(value: Float) = save { it[KEY_TEMPERATURE] = value }
    fun setMaxTokens(value: Int) = save { it[KEY_MAX_TOKENS] = value }
    fun setTopK(value: Int) = save { it[KEY_TOP_K] = value }
    fun setStreaming(enabled: Boolean) = save { it[KEY_STREAMING] = enabled }
    fun setKeepLoaded(enabled: Boolean) = save { it[KEY_KEEP_LOADED] = enabled }

    private fun save(block: (MutablePreferences) -> Unit) {
        viewModelScope.launch {
            context.dataStore.edit { block(it) }
        }
    }
}
