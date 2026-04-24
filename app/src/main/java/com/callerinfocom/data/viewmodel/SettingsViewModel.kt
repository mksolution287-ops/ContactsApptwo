package com.callerinfocom.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.callerinfocom.ui.theme.AppAccentColor
import com.callerinfocom.ui.theme.AppThemeMode
import com.callerinfocom.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("theme_settings", Application.MODE_PRIVATE)

    private val _themeSettings = MutableStateFlow(loadThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    // Other settings
    private val _callerIdEnabled = MutableStateFlow(false)
    val callerIdEnabled: StateFlow<Boolean> = _callerIdEnabled.asStateFlow()

    private val _callerIdOverlayEnabled = MutableStateFlow(false)
    val callerIdOverlayEnabled: StateFlow<Boolean> = _callerIdOverlayEnabled.asStateFlow()

    private val _keypadSoundEnabled = MutableStateFlow(false)
    val keypadSoundEnabled: StateFlow<Boolean> = _keypadSoundEnabled.asStateFlow()

    private val _callbackScreenEnabled = MutableStateFlow(true)
    val callbackScreenEnabled: StateFlow<Boolean> = _callbackScreenEnabled.asStateFlow()

    init {
        // Load non-theme settings
        _callerIdEnabled.value = prefs.getBoolean("caller_id_enabled", false)
        _callerIdOverlayEnabled.value = prefs.getBoolean("caller_id_overlay_enabled", false)
        _keypadSoundEnabled.value = prefs.getBoolean("keypad_sound_enabled", false)
        _callbackScreenEnabled.value = prefs.getBoolean("callback_screen_enabled", true)
    }

    // ── Load Theme Settings ─────────────────────────────────────────────────────
    private fun loadThemeSettings(): ThemeSettings {
        return ThemeSettings(
            themeMode = try {
                AppThemeMode.valueOf(
                    prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
                )
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            },

            accentColor = try {
                AppAccentColor.valueOf(
                    prefs.getString("accent_color", AppAccentColor.PARROTGREEN.name) ?: AppAccentColor.PARROTGREEN.name
                )
            } catch (e: Exception) {
                AppAccentColor.PARROTGREEN
            },

            useDynamicColor = prefs.getBoolean("use_dynamic_color", false),
            useAmoledBlack = prefs.getBoolean("use_amoled_black", false),
            languageCode = prefs.getString("language_code", "en") ?: "en"
        )
    }

    // ── Save Theme Settings ─────────────────────────────────────────────────────
    private fun saveThemeSettings() {
        prefs.edit().apply {
            putString("theme_mode", _themeSettings.value.themeMode.name)
            putString("accent_color", _themeSettings.value.accentColor.name)
            putBoolean("use_dynamic_color", _themeSettings.value.useDynamicColor)
            putBoolean("use_amoled_black", _themeSettings.value.useAmoledBlack)
            putString("language_code", _themeSettings.value.languageCode)
            apply()
        }
    }

    // ── Theme Setters (with persistence) ────────────────────────────────────────
    fun setThemeMode(mode: AppThemeMode) {
        _themeSettings.update { it.copy(themeMode = mode) }
        saveThemeSettings()
    }

    fun setAccentColor(color: AppAccentColor) {
        _themeSettings.update { it.copy(accentColor = color, useDynamicColor = false) }
        saveThemeSettings()
    }

    fun setDynamicColor(enabled: Boolean) {
        _themeSettings.update { it.copy(useDynamicColor = enabled) }
        saveThemeSettings()
    }

    fun setAmoledBlack(enabled: Boolean) {
        _themeSettings.update { it.copy(useAmoledBlack = enabled) }
        saveThemeSettings()
    }

    // ── Other Setters ───────────────────────────────────────────────────────────
    fun setCallerIdEnabled(enabled: Boolean) {
        _callerIdEnabled.value = enabled
        prefs.edit().putBoolean("caller_id_enabled", enabled).apply()
    }

    fun setCallerIdOverlay(enabled: Boolean) {
        _callerIdOverlayEnabled.value = enabled
        prefs.edit().putBoolean("caller_id_overlay_enabled", enabled).apply()
    }

    fun setKeypadSound(enabled: Boolean) {
        _keypadSoundEnabled.value = enabled
        prefs.edit().putBoolean("keypad_sound_enabled", enabled).apply()
    }

    fun setCallbackScreen(enabled: Boolean) {
        _callbackScreenEnabled.value = enabled
        prefs.edit().putBoolean("callback_screen_enabled", enabled).apply()
    }
}