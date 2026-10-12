package com.callerinfocom

import android.app.Application
import com.callerinfocom.utils.AdManager
import com.callerinfocom.utils.AppOpenAdManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        // 1. Create the app-open manager right away so its lifecycle observers
        //    are registered before the first activity starts.
        appOpenAdManager = AppOpenAdManager(this)

        // 2. Start Remote Config NOW (cached values activate in ms) instead of
        //    waiting for MobileAds.initialize() to finish.
        AdManager.init(this)

        // 3. Load the app-open ad as soon as SDK + config are both ready.
        AdManager.runWhenReady { appOpenAdManager.loadAd(this) }

        // 4. Initialize the Mobile Ads SDK on a background thread (recommended
        //    by Google). When it completes, AdManager preloads the interstitial,
        //    banner and both native ads at once.
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(this@MyApp) {
                AdManager.onMobileAdsInitialized()
            }
        }
    }
}