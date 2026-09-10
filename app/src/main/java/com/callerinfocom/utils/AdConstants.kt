package com.callerinfocom.utils

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.BuildConfig

object AdConstants {

    // ── Keep any other ad-related constants you already had here ──────────
    // e.g. const val SOME_KEY = "some_value"

    fun trackAdRevenue(context: Context, revenue: Double, currency: String) {
        val adRevenueParams = Bundle().apply {
            putString(FirebaseAnalytics.Param.AD_PLATFORM, "Google Ad Manager")
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
            putDouble(FirebaseAnalytics.Param.VALUE, revenue)
        }

        Log.d("AdsLog", "▶️ trackAdRevenue CALLED: revenue=$revenue currency=$currency")

        try {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, adRevenueParams)
            Log.d("AdsLog", "✅ logEvent(AD_IMPRESSION) SENT to Firebase: $adRevenueParams")
        } catch (e: Exception) {
            Log.e("AdsLog", "❌ logEvent FAILED: ${e.message}")
        }
    }
}