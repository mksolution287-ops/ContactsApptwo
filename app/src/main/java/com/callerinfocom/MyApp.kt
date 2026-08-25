package com.callerinfocom

import android.app.Application
import com.callerinfocom.utils.AdManager
import com.callerinfocom.utils.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

class MyApp : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()

        // ── Register this device as a test device ──────────────────
        // Device ID comes from logcat: "Use RequestConfiguration.Builder()..."
        // Safe to leave in during development; remove/gate before release.
        val testDeviceIds = listOf("4F095E69A38FF00B02937165814FE45E")
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
        )

        // Initialize AdMob
        MobileAds.initialize(this) {
            AdManager.init(this)

            // ✅ Correct place
            appOpenAdManager = AppOpenAdManager(this)
            appOpenAdManager.loadAd(this)
        }
    }
}