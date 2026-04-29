package com.callerinfocom

import android.app.Application
import com.callerinfocom.utils.AdManager
import com.callerinfocom.utils.AppOpenAdManager
import com.google.android.gms.ads.MobileAds

class MyApp : Application() {

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize AdMob
        MobileAds.initialize(this) {
            AdManager.init(this)

            // ✅ Correct place
            appOpenAdManager = AppOpenAdManager(this)
            appOpenAdManager.loadAd(this)
        }
    }
}