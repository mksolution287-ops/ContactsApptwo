package com.contactsapptwomktech.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.contactsapptwomktech.data.model.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPreferences(private val context: Context) {

    companion object {
        private val KEY_LANGUAGE          = stringPreferencesKey("selected_language")
        private val KEY_ONBOARDING_DONE   = booleanPreferencesKey("onboarding_done")
        private val KEY_PERMISSIONS_ASKED = booleanPreferencesKey("permissions_asked")
    }

    /** The user's chosen language code (BCP-47). Null = not yet chosen. */
    val selectedLanguageCode: Flow<String?> = context.dataStore.data
        .map { it[KEY_LANGUAGE] }

    /** True once the user has completed the full onboarding flow. */
    val onboardingDone: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    /** True once we have already asked for permissions (even if denied). */
    val permissionsAsked: Flow<Boolean> = context.dataStore.data
        .map { it[KEY_PERMISSIONS_ASKED] ?: false }

    suspend fun saveLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.code }
    }

    suspend fun markOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    suspend fun markPermissionsAsked() {
        context.dataStore.edit { it[KEY_PERMISSIONS_ASKED] = true }
    }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }
}