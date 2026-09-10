//package com.callerinfocom.data.preferences
//
//import android.content.Context
//import androidx.datastore.preferences.core.booleanPreferencesKey
//import androidx.datastore.preferences.core.edit
//import androidx.datastore.preferences.core.stringPreferencesKey
//import androidx.datastore.preferences.preferencesDataStore
//import com.callerinfocom.data.model.AppLanguage
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.map
//
//private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")
//
//class OnboardingPreferences(private val context: Context) {
//
//    companion object {
//        private val KEY_LANGUAGE          = stringPreferencesKey("selected_language")
//        private val KEY_ONBOARDING_DONE   = booleanPreferencesKey("onboarding_done")
//        private val KEY_PERMISSIONS_ASKED = booleanPreferencesKey("permissions_asked")
//    }
//
//    /** The user's chosen language code (BCP-47). Null = not yet chosen. */
//    val selectedLanguageCode: Flow<String?> = context.dataStore.data
//        .map { it[KEY_LANGUAGE] }
//
//    /** True once the user has completed the full onboarding flow. */
//    val onboardingDone: Flow<Boolean> = context.dataStore.data
//        .map { it[KEY_ONBOARDING_DONE] ?: false }
//
//    /** True once we have already asked for permissions (even if denied). */
//    val permissionsAsked: Flow<Boolean> = context.dataStore.data
//        .map { it[KEY_PERMISSIONS_ASKED] ?: false }
//
//    suspend fun saveLanguage(language: AppLanguage) {
//        context.dataStore.edit { it[KEY_LANGUAGE] = language.code }
//    }
//
//    suspend fun markOnboardingDone() {
//        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
//    }
//
//    suspend fun markPermissionsAsked() {
//        context.dataStore.edit { it[KEY_PERMISSIONS_ASKED] = true }
//    }
//
//    suspend fun reset() {
//        context.dataStore.edit { it.clear() }
//    }
//}


package com.callerinfocom.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.callerinfocom.data.model.AppLanguage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class OnboardingPreferences(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "onboarding_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_PERMISSIONS_ASKED = "permissions_asked"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Emits the current value immediately, then again whenever [key] changes. */
    private fun <T> observe(key: String, getValue: () -> T): Flow<T> = callbackFlow {
        trySend(getValue())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == key) {
                trySend(getValue())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    /** The user's chosen language code (BCP-47). Null = not yet chosen. */
    val selectedLanguageCode: Flow<String?> =
        observe(KEY_LANGUAGE) { prefs.getString(KEY_LANGUAGE, null) }

    /** True once the user has completed the full onboarding flow. */
    val onboardingDone: Flow<Boolean> =
        observe(KEY_ONBOARDING_DONE) { prefs.getBoolean(KEY_ONBOARDING_DONE, false) }

    /** True once we have already asked for permissions (even if denied). */
    val permissionsAsked: Flow<Boolean> =
        observe(KEY_PERMISSIONS_ASKED) { prefs.getBoolean(KEY_PERMISSIONS_ASKED, false) }

    suspend fun saveLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    suspend fun markOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    suspend fun markPermissionsAsked() {
        prefs.edit().putBoolean(KEY_PERMISSIONS_ASKED, true).apply()
    }

    suspend fun reset() {
        prefs.edit().clear().apply()
    }
}