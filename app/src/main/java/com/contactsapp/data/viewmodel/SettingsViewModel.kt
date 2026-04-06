package com.contactsapp.data.viewmodel

import androidx.lifecycle.ViewModel
import com.contactsapp.ui.theme.AppAccentColor
import com.contactsapp.ui.theme.AppThemeMode
import com.contactsapp.ui.theme.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel : ViewModel() {

    private val _themeSettings = MutableStateFlow(ThemeSettings())
    val themeSettings: StateFlow<ThemeSettings> = _themeSettings.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) =
        _themeSettings.update { it.copy(themeMode = mode) }

    fun setAccentColor(color: AppAccentColor) =
        _themeSettings.update { it.copy(accentColor = color) }

    fun setDynamicColor(enabled: Boolean) =
        _themeSettings.update { it.copy(useDynamicColor = enabled) }

    fun setAmoledBlack(enabled: Boolean) =
        _themeSettings.update { it.copy(useAmoledBlack = enabled) }

    fun setLanguage(code: String) {
        _themeSettings.update {
            it.copy(languageCode = code)
        }
    }

//    fun setLanguageSelected(done: Boolean) {
//        _themeSettings.update {
//            it.copy(languageSelected = done)
//        }
//    }
}