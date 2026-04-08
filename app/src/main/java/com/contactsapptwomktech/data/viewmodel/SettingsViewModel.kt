package com.contactsapptwomktech.data.viewmodel

import androidx.lifecycle.ViewModel
import com.contactsapptwomktech.ui.theme.AppAccentColor
import com.contactsapptwomktech.ui.theme.AppThemeMode
import com.contactsapptwomktech.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    // ── New settings ────────────────────────────────────────────────
    private val _callerIdEnabled     = MutableStateFlow(false)
    val callerIdEnabled: StateFlow<Boolean> = _callerIdEnabled.asStateFlow()

    private val _keypadSoundEnabled  = MutableStateFlow(false)
    val keypadSoundEnabled: StateFlow<Boolean> = _keypadSoundEnabled.asStateFlow()

    private val _callbackScreenEnabled = MutableStateFlow(true)
    val callbackScreenEnabled: StateFlow<Boolean> = _callbackScreenEnabled.asStateFlow()

    // ── Setters ─────────────────────────────────────────────────────
    fun setThemeMode(mode: AppThemeMode)       = _themeSettings.update { it.copy(themeMode = mode) }
    fun setAccentColor(color: AppAccentColor)  = _themeSettings.update { it.copy(accentColor = color) }
    fun setDynamicColor(enabled: Boolean)      = _themeSettings.update { it.copy(useDynamicColor = enabled) }
    fun setAmoledBlack(enabled: Boolean)       = _themeSettings.update { it.copy(useAmoledBlack = enabled) }
    fun setLanguage(code: String)              = _themeSettings.update { it.copy(languageCode = code) }

    fun setCallerIdEnabled(enabled: Boolean)   { _callerIdEnabled.value = enabled }
    fun setKeypadSound(enabled: Boolean)       { _keypadSoundEnabled.value = enabled }
    fun setCallbackScreen(enabled: Boolean)    { _callbackScreenEnabled.value = enabled }
}