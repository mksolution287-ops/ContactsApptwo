package com.callerinfo.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.callerinfo.data.model.AppLanguage
import com.callerinfo.data.preferences.OnboardingPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = OnboardingPreferences(application)

    /** The currently persisted language. Null = user hasn't chosen yet. */
    val selectedLanguage: StateFlow<AppLanguage?> = prefs.selectedLanguageCode
        .map { code -> code?.let { AppLanguage.fromCode(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True once the full onboarding has been completed at least once. */
    val onboardingDone: StateFlow<Boolean> = prefs.onboardingDone
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun saveLanguage(language: AppLanguage) {
        viewModelScope.launch { prefs.saveLanguage(language) }
    }

    fun markOnboardingDone() {
        viewModelScope.launch { prefs.markOnboardingDone() }
    }

    fun markPermissionsAsked() {
        viewModelScope.launch { prefs.markPermissionsAsked() }
    }
}