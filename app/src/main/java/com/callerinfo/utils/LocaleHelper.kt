package com.callerinfo.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    private const val PREF_NAME = "app_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    // Save selected language
    fun saveLanguage(context: Context, languageCode: String) {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    // Get saved language
    fun getLanguage(context: Context): String {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    // Apply locale
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            context.createConfigurationContext(config)
        } else {
            config.locale = locale
            context.resources.updateConfiguration(
                config,
                context.resources.displayMetrics
            )
            context
        }
    }
}