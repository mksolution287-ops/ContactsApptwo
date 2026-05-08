package com.callerinfocom.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import java.util.Date

class AppOpenAdManager(private val application: Application) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppOpenAdManager"
        private const val AD_EXPIRY_MS = 4 * 60 * 60 * 1000L
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime: Long = 0
    private var currentActivity: Activity? = null

    // True once the app has genuinely gone to background (ProcessLifecycleOwner sets this)
    private var wasInBackground = false

    init {
        application.registerActivityLifecycleCallbacks(this)
        // ProcessLifecycleOwner correctly detects app-level background/foreground transitions,
        // including when the user navigates to system Settings (overlay / app details).
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // ── ProcessLifecycleOwner callbacks (app-level foreground/background) ─

    /** Called when the app comes to the foreground (any activity becomes visible). */
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "ProcessLifecycle → onStart  wasInBackground=$wasInBackground")
        if (wasInBackground) {
            currentActivity?.let { showAdIfAvailable(it) }
        }
    }

    /** Called when the app goes to the background (all activities are stopped). */
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "ProcessLifecycle → onStop  → wasInBackground=true")
        wasInBackground = true
    }

    // ── Ad loading ────────────────────────────────────────────────────────

    private fun isAdExpired(): Boolean = Date().time - loadTime > AD_EXPIRY_MS
    private fun isAdAvailable(): Boolean = appOpenAd != null && !isAdExpired()

    fun loadAd(context: Context) {
        if (!AdManager.isAppOpenAdEnabled()) return
        if (isLoadingAd || isAdAvailable()) return
        isLoadingAd = true

        val adUnitId = AdManager.getAppOpenAdUnitId()

        AppOpenAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = Date().time
                    Log.d(TAG, "App open ad loaded ✅")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    Log.e(TAG, "App open ad failed ❌ ${error.message}")
                }
            }
        )
    }

    // ── Ad showing ────────────────────────────────────────────────────────

    fun showAdIfAvailable(activity: Activity) {
        val prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Suppress ad on language-change recreate, then immediately clear the flag
        if (prefs.getBoolean("skip_app_open_ad", false)) {
            prefs.edit().putBoolean("skip_app_open_ad", false).apply()
            return
        }

        if (!AdManager.isAppOpenAdEnabled()) return
        if (isShowingAd) return
        if (!wasInBackground) return

        // Hard block — interstitial is literally on screen right now
        if (AdManager.isInterstitialShowing) {
            Log.d(TAG, "Skipping app open — interstitial is on screen")
            return
        }

        // Soft block — interstitial just dismissed within cooldown window
        if (AdManager.isInterstitialRecentlyActive()) {
            Log.d(TAG, "Skipping app open — interstitial cooldown active")
            wasInBackground = false
            return
        }

        if (!isAdAvailable()) {
            loadAd(activity)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                Log.d(TAG, "App open ad shown ✅")
            }

            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                wasInBackground = false
                Log.d(TAG, "App open ad dismissed")
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                wasInBackground = false
                Log.e(TAG, "App open ad failed to show ❌ ${error.message}")
                loadAd(activity)
            }
        }

        appOpenAd?.show(activity)
    }

    // ── Activity lifecycle (only used to track currentActivity) ──────────

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        // Note: ad display is triggered by ProcessLifecycleOwner.onStart(), not here.
        // This avoids double-firing and correctly handles the Settings → app return case.
    }

    override fun onActivityStopped(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) currentActivity = null
    }
}