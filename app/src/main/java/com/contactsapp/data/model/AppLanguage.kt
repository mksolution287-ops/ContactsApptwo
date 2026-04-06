package com.contactsapp.data.model

import androidx.annotation.StringRes
import com.contactsapp.R

enum class AppLanguage(
    val code        : String,   // BCP-47 tag used with LocaleList
    val displayName : String,   // always in the target language
    val nativeName  : String,   // native script
    val flag        : String    // emoji flag
) {
    ENGLISH  ("en",    "English",  "English",   "🇬🇧"),
    HINDI    ("hi",    "Hindi",    "हिन्दी",      "🇮🇳"),
    ARABIC   ("ar",    "Arabic",   "العربية",    "🇸🇦"),
    SPANISH  ("es",    "Spanish",  "Español",   "🇪🇸"),
    FRENCH   ("fr",    "French",   "Français",  "🇫🇷"),
    GERMAN   ("de",    "German",   "Deutsch",   "🇩🇪"),
    KOREAN   ("ko",    "Korean",   "한국어",       "🇰🇷"),
    JAPANESE ("ja",    "Japanese", "日本語",       "🇯🇵"),
    MANDARIN ("zh-CN", "Mandarin", "普通话",       "🇨🇳");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}