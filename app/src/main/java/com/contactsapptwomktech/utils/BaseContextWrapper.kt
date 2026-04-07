package com.contactsapptwomktech.utils

import android.content.Context
import android.content.ContextWrapper

class BaseContextWrapper(base: Context) : ContextWrapper(base) {

    companion object {
        fun wrap(context: Context): ContextWrapper {
            val language = LocaleHelper.getLanguage(context)
            val newContext = LocaleHelper.setLocale(context, language)
            return BaseContextWrapper(newContext)
        }
    }
}