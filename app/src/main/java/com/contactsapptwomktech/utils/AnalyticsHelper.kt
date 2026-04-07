package com.contactsapptwomktech.utils

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object AnalyticsHelper {

    private var analytics: FirebaseAnalytics? = null

    fun init(firebaseAnalytics: FirebaseAnalytics) {
        analytics = firebaseAnalytics
    }

    // Screen View
    fun logScreenView(screenName: String) {
        analytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        })
    }

    // Button Click
    fun logButtonClick(buttonName: String, screen: String) {
        analytics?.logEvent("button_click", Bundle().apply {
            putString("button_name", buttonName)
            putString("screen", screen)
        })
    }

    // Call Event
    fun logCallEvent(callType: String, durationSec: Long?) {
        val bundle = Bundle().apply {
            putString("call_type", callType)
            durationSec?.let { putLong("duration_seconds", it) }
        }

        analytics?.logEvent("call_completed", bundle)
    }
}