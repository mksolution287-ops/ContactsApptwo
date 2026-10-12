package com.callerinfocom.utils

import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdManager {

    private const val TAG = "AdManager"

    /**
     * MASTER TEST-ADS SWITCH.
     *  true  → all ads use Google's test ad unit IDs.
     *  false → real IDs from Remote Config (RELEASE).
     */
    private const val USE_TEST_ADS = false

    // ── Test Ad Unit IDs ──────────────────────────────────────────────────
    private const val TEST_BANNER_ID       = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE_ID       = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_APP_OPEN_ID     = "ca-app-pub-3940256099942544/9257395921"

    // ── Remote Config keys ────────────────────────────────────────────────
    private const val KEY_ADS_ENABLED          = "ads_enabled"
    private const val KEY_BANNER_ENABLED       = "banner_ad_enabled"
    private const val KEY_INTERSTITIAL_ENABLED = "interstitial_ad_enabled"
    private const val KEY_NATIVE_ENABLED       = "native_ad_enabled"
    private const val KEY_APP_OPEN_ENABLED     = "app_open_ad_enabled"
    private const val KEY_INTERSTITIAL_TRIGGER = "interstitial_trigger_count"
    private const val KEY_BANNER_AD_UNIT       = "banner_ad_unit_id"
    private const val KEY_INTERSTITIAL_AD_UNIT = "interstitial_ad_unit_id"
    private const val KEY_NATIVE_AD_UNIT       = "native_ad_unit_id"
    private const val KEY_APP_OPEN_AD_UNIT     = "app_open_ad_unit_id"

    // ── Native slots ──────────────────────────────────────────────────────
    // Each slot is an independently preloaded NativeAd.
    //  NATIVE_KEY_DEFAULT     → NativeAdCard
    //  NATIVE_KEY_APP_INSTALL → AppInstallNativeAdCard (landscape media, AdChoices top-right)
    const val NATIVE_KEY_DEFAULT = "default"
    const val NATIVE_KEY_APP_INSTALL = "app_install"
    private val ALL_NATIVE_KEYS = listOf(NATIVE_KEY_DEFAULT, NATIVE_KEY_APP_INSTALL)

    // ── Timing knobs ──────────────────────────────────────────────────────
    private const val INTERSTITIAL_COOLDOWN_MS = 2000L
    private const val INTERSTITIAL_WAIT_TIMEOUT_MS = 1500L
    private const val INTERSTITIAL_POLL_INTERVAL_MS = 100L
    private const val AD_EXPIRY_MS = 55 * 60 * 1000L   // interstitial/native expire after 1h

    // Retry with exponential backoff: 2s, 4s, 8s
    private const val MAX_RETRIES = 3
    private const val RETRY_BASE_MS = 2000L

    // ── Threading helpers ─────────────────────────────────────────────────
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun shouldRetry(errorCode: Int, attempt: Int): Boolean =
        attempt < MAX_RETRIES && errorCode != AdRequest.ERROR_CODE_INVALID_REQUEST

    private fun retryDelayMs(attempt: Int): Long = RETRY_BASE_MS shl attempt

    // ── State ─────────────────────────────────────────────────────────────
    private var initialized = false
    private var appContext: Context? = null
    private var actionCount = 0

    private val _adsEnabled = MutableStateFlow(false)
    val adsEnabled: StateFlow<Boolean> = _adsEnabled

    private val _bannerEnabled = MutableStateFlow(false)
    val bannerEnabled: StateFlow<Boolean> = _bannerEnabled

    private val _nativeEnabled = MutableStateFlow(false)
    val nativeEnabled: StateFlow<Boolean> = _nativeEnabled

    private val _appOpenEnabled = MutableStateFlow(false)
    val appOpenEnabled: StateFlow<Boolean> = _appOpenEnabled

    var isInterstitialShowing = false
        private set

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading

    var isAppOpenAdBlocked = false

    // ── Readiness gate ────────────────────────────────────────────────────
    // Preloading starts when BOTH the Mobile Ads SDK is initialized AND
    // Remote Config values are usable (cached → instant; first-ever launch →
    // waits for the first fetch so we never load with placeholder IDs).
    private var mobileAdsReady = false
    private var configLoaded = false

    private val _adsReady = MutableStateFlow(false)
    val adsReady: StateFlow<Boolean> = _adsReady
    val isAdsReady: Boolean get() = _adsReady.value

    private val readyCallbacks = mutableListOf<() -> Unit>()

    /** Run [block] on the main thread once ads are ready (immediately if already ready). */
    fun runWhenReady(block: () -> Unit) {
        runOnMain {
            if (_adsReady.value) block() else readyCallbacks.add(block)
        }
    }

    // Slot requests that arrived before we were ready
    private var interstitialRequestedEarly = false
    private val pendingNativeKeys = mutableSetOf<String>()

    // ── Interstitial state ────────────────────────────────────────────────
    private var interstitialAd: InterstitialAd? = null
    private var interstitialLoadTime = 0L
    private var interstitialLoading = false
    private var interstitialDismissedAt = 0L

    // ── Native state (keyed) ──────────────────────────────────────────────
    private val nativeAds = mutableMapOf<String, NativeAd>()
    private val nativeLoadTimes = mutableMapOf<String, Long>()
    private val nativeLoadingKeys = mutableSetOf<String>()
    private val shownNativeKeys = mutableSetOf<String>()
    private val nativeReadyFlows = mutableMapOf<String, MutableStateFlow<Boolean>>()

    private fun nativeReadyFlowInternal(key: String): MutableStateFlow<Boolean> =
        nativeReadyFlows.getOrPut(key) { MutableStateFlow(false) }

    fun nativeAdReadyFlow(key: String = NATIVE_KEY_DEFAULT): StateFlow<Boolean> = nativeReadyFlowInternal(key)

    // Kept for existing call sites
    val nativeAdReady: StateFlow<Boolean> get() = nativeReadyFlowInternal(NATIVE_KEY_DEFAULT)

    // ── Banner state (one preloaded, cached AdView) ───────────────────────
    private var bannerView: AdView? = null
    private var bannerWrapper: MutableContextWrapper? = null
    private var bannerUnitId: String? = null
    private var bannerWidthDp = 0
    private var bannerRetry = 0
    private var bannerRefCount = 0

    private val _bannerAdReady = MutableStateFlow(false)
    val bannerAdReady: StateFlow<Boolean> = _bannerAdReady

    // ── Remote Config defaults ────────────────────────────────────────────
    private val remoteConfigDefaults = mapOf(
        KEY_ADS_ENABLED          to true,
        KEY_BANNER_ENABLED       to true,
        KEY_INTERSTITIAL_ENABLED to true,
        KEY_NATIVE_ENABLED       to true,
        KEY_APP_OPEN_ENABLED     to true,
        KEY_INTERSTITIAL_TRIGGER to 3L,
        KEY_BANNER_AD_UNIT       to TEST_BANNER_ID,
        KEY_INTERSTITIAL_AD_UNIT to TEST_INTERSTITIAL_ID,
        KEY_NATIVE_AD_UNIT       to TEST_NATIVE_ID,
        KEY_APP_OPEN_AD_UNIT     to TEST_APP_OPEN_ID
    )

    // ── Revenue tracking helper — used by every ad format ─────────────────
    fun trackPaidAd(context: Context, adValue: AdValue) {
        try {
            val revenue = adValue.valueMicros / 1_000_000.0
            val currency = adValue.currencyCode
            AdConstants.trackAdRevenue(context.applicationContext, revenue, currency)
        } catch (_: Exception) {
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Call FIRST in Application.onCreate() — don't wait for MobileAds.
     * Then call [onMobileAdsInitialized] from the MobileAds.initialize callback.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext

        _adsEnabled.value     = remoteConfigDefaults[KEY_ADS_ENABLED] as Boolean
        _bannerEnabled.value  = _adsEnabled.value && (remoteConfigDefaults[KEY_BANNER_ENABLED] as Boolean)
        _nativeEnabled.value  = _adsEnabled.value && (remoteConfigDefaults[KEY_NATIVE_ENABLED] as Boolean)
        _appOpenEnabled.value = _adsEnabled.value && (remoteConfigDefaults[KEY_APP_OPEN_ENABLED] as Boolean)

        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0
                fetchTimeoutInSeconds = 5   // a slow network must not stall the first ad load
            }
        )

        remoteConfig.setDefaultsAsync(remoteConfigDefaults).addOnCompleteListener {
            // FAST PATH: fetched before → activate the downloaded values locally
            // (no network) and be ready within milliseconds.
            if (remoteConfig.info.fetchTimeMillis > 0) {
                remoteConfig.activate().addOnCompleteListener { onConfigLoaded() }
            }
            // Always refresh in the background; changes are picked up afterwards.
            remoteConfig.fetchAndActivate().addOnCompleteListener { onConfigLoaded() }
        }

        // Real-time listener — fires whenever you publish changes in Firebase
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener { onConfigLoaded() }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.e(TAG, "Real-time config error: ${error.message}")
            }
        })
    }

    /** Call from the MobileAds.initialize completion callback (any thread). */
    fun onMobileAdsInitialized() {
        runOnMain {
            mobileAdsReady = true
            evaluateReady()
        }
    }

    private fun onConfigLoaded() {
        runOnMain {
            val config = Firebase.remoteConfig
            val master = config.getBoolean(KEY_ADS_ENABLED)
            _adsEnabled.value     = master
            _bannerEnabled.value  = master && config.getBoolean(KEY_BANNER_ENABLED)
            _nativeEnabled.value  = master && config.getBoolean(KEY_NATIVE_ENABLED)
            _appOpenEnabled.value = master && config.getBoolean(KEY_APP_OPEN_ENABLED)

            Log.d(TAG, "Config applied: ads=${_adsEnabled.value} " +
                    "banner=${_bannerEnabled.value} native=${_nativeEnabled.value}")

            configLoaded = true
            evaluateReady()
        }
    }

    private fun evaluateReady() {
        if (!mobileAdsReady || !configLoaded) return
        val ctx = appContext ?: return

        val firstTime = !_adsReady.value
        _adsReady.value = true

        if (firstTime) {
            Log.d(TAG, "🚀 Ads ready — starting preload")
            // App-open (and anything else registered via runWhenReady) goes first
            val callbacks = readyCallbacks.toList()
            readyCallbacks.clear()
            callbacks.forEach { it() }
        }

        preloadAll(ctx)
    }

    /**
     * Loads every ad this app uses, all at once. Idempotent: anything already
     * loaded/loading is skipped, anything whose ad unit changed or expired is
     * refreshed. Called when ads become ready and again on each config update.
     */
    private fun preloadAll(ctx: Context) {
        if (!_adsEnabled.value) return
        loadInterstitial(ctx, 0)
        if (_bannerEnabled.value) preloadBanner()
        if (_nativeEnabled.value) {
            ALL_NATIVE_KEYS.forEach { loadNativeAd(ctx, it, false, 0) }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONFIG HELPERS
    // ══════════════════════════════════════════════════════════════════════

    fun isInterstitialRecentlyActive(): Boolean {
        return isInterstitialShowing ||
                (System.currentTimeMillis() - interstitialDismissedAt < INTERSTITIAL_COOLDOWN_MS)
    }

    fun isAppOpenAdEnabled(): Boolean {
        val config = Firebase.remoteConfig
        return config.getBoolean(KEY_ADS_ENABLED) &&
                config.getBoolean(KEY_APP_OPEN_ENABLED)
    }

    private fun isInterstitialEnabled(): Boolean {
        if (!_adsEnabled.value) return false
        return Firebase.remoteConfig.getBoolean(KEY_INTERSTITIAL_ENABLED)
    }

    fun getBannerAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_BANNER_ID
        return Firebase.remoteConfig.getString(KEY_BANNER_AD_UNIT).ifBlank { TEST_BANNER_ID }
    }

    fun getAppInstallNativeAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_NATIVE_ID
        return Firebase.remoteConfig.getString(KEY_NATIVE_AD_UNIT).ifBlank { TEST_NATIVE_ID }
    }

    fun getNativeAdUnitId(): String = getAppInstallNativeAdUnitId()

    fun getAppOpenAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_APP_OPEN_ID
        return Firebase.remoteConfig.getString(KEY_APP_OPEN_AD_UNIT).ifBlank { TEST_APP_OPEN_ID }
    }

    private fun getInterstitialAdUnitId(): String {
        if (USE_TEST_ADS) return TEST_INTERSTITIAL_ID
        return Firebase.remoteConfig.getString(KEY_INTERSTITIAL_AD_UNIT).ifBlank { TEST_INTERSTITIAL_ID }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BANNER
    // ══════════════════════════════════════════════════════════════════════

    private fun currentWidthDp(ctx: Context): Int {
        val dm = ctx.resources.displayMetrics
        return (dm.widthPixels / dm.density).toInt()
    }

    private fun getOrCreateBanner(widthDp: Int): AdView? {
        val app = appContext ?: return null
        if (!_adsReady.value || !_adsEnabled.value || !_bannerEnabled.value) return null

        val unitId = getBannerAdUnitId()
        val existing = bannerView
        if (existing != null) {
            if (bannerUnitId == unitId && bannerWidthDp == widthDp) return existing
            destroyBanner()   // unit id or screen width changed → rebuild
        }

        val wrapper = MutableContextWrapper(app)
        val adView = AdView(wrapper)
        adView.setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(app, widthDp))
        adView.adUnitId = unitId

        adView.setOnPaidEventListener { adValue -> trackPaidAd(app, adValue) }

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerRetry = 0
                _bannerAdReady.value = true
                Log.d(TAG, "✅ Banner LOADED")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "❌ Banner FAILED: ${error.code} - ${error.message}")
                if (shouldRetry(error.code, bannerRetry)) {
                    val delay = retryDelayMs(bannerRetry)
                    bannerRetry++
                    mainHandler.postDelayed({
                        if (bannerView === adView) adView.loadAd(AdRequest.Builder().build())
                    }, delay)
                }
            }

            override fun onAdImpression() {
                Log.d(TAG, "Banner impression recorded")
            }
        }

        bannerView = adView
        bannerWrapper = wrapper
        bannerUnitId = unitId
        bannerWidthDp = widthDp
        _bannerAdReady.value = false
        adView.loadAd(AdRequest.Builder().build())
        Log.d(TAG, "Banner loading (unit=$unitId, width=${widthDp}dp)")
        return adView
    }

    /** Start loading the banner in the background before any screen shows it. */
    fun preloadBanner() {
        runOnMain {
            val app = appContext ?: return@runOnMain
            getOrCreateBanner(currentWidthDp(app))
        }
    }

    /**
     * Used by BannerAd(): returns the (usually already loaded) AdView, detached
     * from any previous parent and bound to [hostContext]. Pair with [releaseBanner].
     */
    fun acquireBanner(hostContext: Context): AdView? {
        val adView = getOrCreateBanner(currentWidthDp(hostContext)) ?: return null
        bannerWrapper?.setBaseContext(hostContext)
        (adView.parent as? ViewGroup)?.removeView(adView)
        bannerRefCount++
        adView.resume()
        return adView
    }

    fun releaseBanner() {
        bannerRefCount = (bannerRefCount - 1).coerceAtLeast(0)
        // Only pause/detach when no screen is using it (nav transitions briefly
        // compose two screens at once).
        if (bannerRefCount == 0) {
            bannerView?.pause()
            appContext?.let { bannerWrapper?.setBaseContext(it) }   // drop the Activity reference
        }
    }

    fun destroyBanner() {
        bannerView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        bannerView = null
        bannerWrapper = null
        bannerUnitId = null
        bannerRetry = 0
        bannerRefCount = 0
        _bannerAdReady.value = false
    }

    // ══════════════════════════════════════════════════════════════════════
    //  INTERSTITIAL
    // ══════════════════════════════════════════════════════════════════════

    private fun isInterstitialAvailable(): Boolean {
        if (interstitialAd == null) return false
        if (System.currentTimeMillis() - interstitialLoadTime > AD_EXPIRY_MS) {
            interstitialAd = null
            return false
        }
        return true
    }

    /** Safe to call repeatedly — skips if already loaded or loading. */
    fun preloadInterstitial(context: Context) {
        val app = context.applicationContext
        runOnMain { loadInterstitial(app, 0) }
    }

    private fun loadInterstitial(ctx: Context, attempt: Int) {
        if (!isInterstitialEnabled()) {
            Log.d(TAG, "Interstitial disabled by config")
            return
        }
        if (!_adsReady.value) {
            interstitialRequestedEarly = true
            return
        }
        if (isInterstitialAvailable() || interstitialLoading) return

        val adUnitId = getInterstitialAdUnitId()
        interstitialLoading = true
        Log.d(TAG, "Loading interstitial...")

        InterstitialAd.load(
            ctx,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialAd = ad
                    interstitialLoadTime = System.currentTimeMillis()
                    Log.d(TAG, "✅ Interstitial LOADED")

                    ad.setOnPaidEventListener { adValue -> trackPaidAd(ctx, adValue) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitialAd = null
                    Log.e(TAG, "❌ Interstitial FAILED: ${error.code} ${error.message}")

                    if (shouldRetry(error.code, attempt)) {
                        mainHandler.postDelayed(
                            { loadInterstitial(ctx, attempt + 1) },
                            retryDelayMs(attempt)
                        )
                    }
                }
            }
        )
    }

    /** Shows the already-loaded interstitial (caller must have checked availability). */
    private fun showLoaded(activity: Activity, onDismiss: () -> Unit) {
        val ad = interstitialAd
        if (ad == null || activity.isFinishing || activity.isDestroyed) {
            _isAdLoading.value = false
            onDismiss()
            return
        }

        val app = activity.applicationContext
        isInterstitialShowing = true

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "🎬 Ad SHOWED")
                _isAdLoading.value = false
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "❎ Ad DISMISSED")
                interstitialAd = null
                isInterstitialShowing = false
                interstitialDismissedAt = System.currentTimeMillis()
                _isAdLoading.value = false
                onDismiss()
                loadInterstitial(app, 0)   // next one starts loading immediately
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "❌ Failed to SHOW ad: ${error.message}")
                interstitialAd = null
                isInterstitialShowing = false
                _isAdLoading.value = false
                onDismiss()
                loadInterstitial(app, 0)
            }
        }
        ad.show(activity)
    }

    // ── Call this on user actions (calls made, contacts opened, etc.) ─────
    fun trackAction(context: Context, activity: Activity?) {
        if (!_adsEnabled.value) return
        val config = Firebase.remoteConfig
        if (!config.getBoolean(KEY_INTERSTITIAL_ENABLED)) return

        actionCount++
        val triggerCount = config.getLong(KEY_INTERSTITIAL_TRIGGER).toInt()

        if (actionCount >= triggerCount) {
            actionCount = 0

            // Block interstitial if app just resumed (App Open takes priority)
            if (isInterstitialRecentlyActive()) {
                Log.d(TAG, "Skipping interstitial due to recent app open")
                return
            }

            if (activity != null && isInterstitialAvailable()) {
                showLoaded(activity) { }
            } else {
                preloadInterstitial(context)
            }
        }
    }

    fun immediateInterstitialAd(
        activity: Activity?,
        onDismiss: () -> Unit
    ) {
        runOnMain { immediateInterstitialInternal(activity, onDismiss) }
    }

    private fun immediateInterstitialInternal(activity: Activity?, onDismiss: () -> Unit) {
        Log.d(TAG, "👉 immediateInterstitialAd called")

        // Interstitial switched off → don't make the user wait for nothing
        if (!isInterstitialEnabled()) {
            Log.d(TAG, "❌ Ads / interstitial disabled")
            onDismiss()
            return
        }

        if (activity == null) {
            Log.e(TAG, "❌ Activity is NULL")
            onDismiss()
            return
        }

        // Fast path — already preloaded
        if (isInterstitialAvailable()) {
            Log.d(TAG, "✅ Ad is READY → showing now")
            showLoaded(activity, onDismiss)
            return
        }

        // Slow path — wait briefly for the in-flight (or freshly started) load
        Log.d(TAG, "⏳ Ad not ready → waiting up to ${INTERSTITIAL_WAIT_TIMEOUT_MS}ms")
        _isAdLoading.value = true
        loadInterstitial(activity.applicationContext, 0)

        val startTime = System.currentTimeMillis()
        val check = object : Runnable {
            override fun run() {
                if (isInterstitialAvailable()) {
                    Log.d(TAG, "✅ Ad loaded during wait → showing now")
                    showLoaded(activity, onDismiss)
                    return
                }

                val elapsed = System.currentTimeMillis() - startTime
                // Give up on timeout, or as soon as the load has definitively failed
                val loadDied = _adsReady.value && !interstitialLoading
                if (elapsed > INTERSTITIAL_WAIT_TIMEOUT_MS || loadDied) {
                    Log.d(TAG, "⌛ Giving up on ad (elapsed=${elapsed}ms, loadDied=$loadDied)")
                    _isAdLoading.value = false
                    onDismiss()
                    return
                }

                mainHandler.postDelayed(this, INTERSTITIAL_POLL_INTERVAL_MS)
            }
        }
        mainHandler.post(check)
    }

    fun showInterstitialOnAppResume(activity: Activity?) {
        if (!_adsEnabled.value) return
        if (activity == null) return
        if (isInterstitialShowing) return

        if (isInterstitialAvailable()) {
            showLoaded(activity) { }
        } else {
            preloadInterstitial(activity)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  NATIVE
    // ══════════════════════════════════════════════════════════════════════

    private fun isNativeExpired(key: String): Boolean {
        val t = nativeLoadTimes[key] ?: return false
        return System.currentTimeMillis() - t > AD_EXPIRY_MS
    }

    private fun nativeOptionsFor(key: String): NativeAdOptions =
        if (key == NATIVE_KEY_APP_INSTALL) {
            NativeAdOptions.Builder()
                .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                .build()
        } else {
            NativeAdOptions.Builder().build()
        }

    /**
     * Preload a native ad into [key]'s slot. Safe to call repeatedly (no-op if
     * already loaded/loading). Queued automatically if ads aren't ready yet.
     */
    fun preloadNativeAd(context: Context, key: String = NATIVE_KEY_DEFAULT, force: Boolean = false) {
        val app = context.applicationContext
        runOnMain { loadNativeAd(app, key, force, 0) }
    }

    private fun loadNativeAd(ctx: Context, key: String, force: Boolean, attempt: Int) {
        if (!_adsEnabled.value || !_nativeEnabled.value) return
        if (!_adsReady.value) {
            pendingNativeKeys.add(key)
            return
        }
        if (nativeLoadingKeys.contains(key)) return

        val ready = nativeReadyFlowInternal(key)
        val adUnitId = getAppInstallNativeAdUnitId()

        if (!force && ready.value) {
            // Replace only if expired AND not currently on screen
            if (shownNativeKeys.contains(key) || !isNativeExpired(key)) return
            destroyNativeAd(key)
        }

        nativeLoadingKeys.add(key)
        Log.d(TAG, "preloadNativeAd[$key]: loading adUnitId=$adUnitId")

        val adLoader = AdLoader.Builder(ctx, adUnitId)
            .forNativeAd { ad ->
                nativeAds[key]?.destroy()
                nativeAds[key] = ad
                nativeLoadTimes[key] = System.currentTimeMillis()
                shownNativeKeys.remove(key)
                nativeLoadingKeys.remove(key)
                ready.value = true
                Log.d(TAG, "Native ad[$key] loaded ✅")

                ad.setOnPaidEventListener { adValue -> trackPaidAd(ctx, adValue) }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    nativeLoadingKeys.remove(key)
                    if (nativeAds[key] == null) ready.value = false
                    Log.e(TAG, "Native ad[$key] failed ❌ code=${error.code} msg=${error.message}")

                    if (shouldRetry(error.code, attempt)) {
                        mainHandler.postDelayed(
                            { loadNativeAd(ctx, key, false, attempt + 1) },
                            retryDelayMs(attempt)
                        )
                    }
                }
            })
            .withNativeAdOptions(nativeOptionsFor(key))
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun getNativeAd(key: String = NATIVE_KEY_DEFAULT): NativeAd? = nativeAds[key]

    /** Called by the native composables when they render this slot's ad. */
    fun markNativeAdShown(key: String = NATIVE_KEY_DEFAULT) {
        shownNativeKeys.add(key)
    }

    /**
     * Called when a native composable leaves the screen. If its ad was shown,
     * discard it and immediately load a FRESH one, so the next visit shows a
     * new ad (= new impression) that's already waiting.
     */
    fun recycleNativeAd(context: Context, key: String = NATIVE_KEY_DEFAULT) {
        val app = context.applicationContext
        runOnMain {
            if (!shownNativeKeys.contains(key)) return@runOnMain
            destroyNativeAd(key)
            loadNativeAd(app, key, false, 0)
        }
    }

    fun destroyNativeAd(key: String = NATIVE_KEY_DEFAULT) {
        nativeAds[key]?.destroy()
        nativeAds.remove(key)
        nativeLoadTimes.remove(key)
        shownNativeKeys.remove(key)
        nativeReadyFlowInternal(key).value = false
        nativeLoadingKeys.remove(key)
    }
}

@Composable
fun AdLoadingOverlay(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Loading Ad...",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}