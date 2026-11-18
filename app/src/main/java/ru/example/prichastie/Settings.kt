package ru.example.prichastie

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

const val MIN_FONT_SP = 16
const val MAX_FONT_SP = 32
const val DEFAULT_FONT_SP = 20

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val fontSizeSp: Int = DEFAULT_FONT_SP,
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val FONT = intPreferencesKey("font_size_sp")
        val THEME = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            fontSizeSp = (p[Keys.FONT] ?: DEFAULT_FONT_SP).coerceIn(MIN_FONT_SP, MAX_FONT_SP),
            themeMode = ThemeMode.values().firstOrNull { it.name == p[Keys.THEME] }
                ?: ThemeMode.SYSTEM
        )
    }

    suspend fun setFontSize(value: Int) {
        context.settingsDataStore.edit { it[Keys.FONT] = value.coerceIn(MIN_FONT_SP, MAX_FONT_SP) }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)

    val settings: StateFlow<AppSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    fun setFontSize(value: Int) {
        viewModelScope.launch { store.setFontSize(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.setThemeMode(mode) }
    }
}