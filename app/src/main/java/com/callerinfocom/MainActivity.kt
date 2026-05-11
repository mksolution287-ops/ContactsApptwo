package com.callerinfocom

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.callerinfocom.data.viewmodel.ContactsViewModel
import com.callerinfocom.data.viewmodel.SettingsViewModel
import com.callerinfocom.service.CallDetectionService
import com.callerinfocom.ui.screens.MainScreen
import com.callerinfocom.ui.theme.ContactsAppTheme
import com.callerinfocom.utils.AnalyticsHelper
import com.callerinfocom.utils.AppOpenAdManager
import com.callerinfocom.utils.BaseContextWrapper
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val contactsViewModel: ContactsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    // Holds the requested tab so MainScreen can observe it reactively
    private val _initialTab = MutableStateFlow<String?>(null)

    lateinit var firebaseAnalytics: FirebaseAnalytics
        private set

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(BaseContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirebaseApp.initializeApp(this)
        firebaseAnalytics = Firebase.analytics
        AnalyticsHelper.init(firebaseAnalytics)
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)
        setDeviceInfoToCrashlytics(crashlytics)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val callerIdOn = prefs.getBoolean("caller_id_enabled", false)
        settingsViewModel.setCallerIdEnabled(callerIdOn)

        _initialTab.value = resolveTab(intent)

        setContent {
            val themeSettings by settingsViewModel.themeSettings.collectAsState()
            val initialTab    by _initialTab.collectAsState()          // ← observe

            ContactsAppTheme(settings = themeSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        rootViewModel     = contactsViewModel,
                        settingsViewModel = settingsViewModel,
                        initialTab        = initialTab,                // ← pass
                        onTabConsumed     = { _initialTab.value = null } // ← reset after nav
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _initialTab.value = resolveTab(intent)
    }

    private fun resolveTab(intent: Intent?): String? =
        when (intent?.data?.host) {
            "contacts" -> "contacts"
            "dialpad"  -> "dialpad"
            "recents"  -> "recents"
            else       -> null
        }

    private fun setDeviceInfoToCrashlytics(crashlytics: FirebaseCrashlytics) {
        try {
            val manufacturer = Build.MANUFACTURER ?: "Unknown"
            val model = Build.MODEL ?: "Unknown"
            val deviceName = "$manufacturer $model"

            crashlytics.setCustomKey("device_manufacturer", manufacturer)
            crashlytics.setCustomKey("device_model", model)
            crashlytics.setCustomKey("device_name", deviceName)
            crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE)
            crashlytics.setCustomKey("sdk_version", Build.VERSION.SDK_INT.toString())

            // Optional: Also set as User ID (visible in every crash)
            // crashlytics.setUserId(deviceName)

        } catch (e: Exception) {
            // Avoid crashing during initialization
            crashlytics.recordException(e)
        }
    }

    private fun startCallDetectionService() {
        val intent = Intent(this, CallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val hasPhonePermission = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_PHONE_STATE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(this)
        } else true

        if (hasPhonePermission && hasOverlayPermission) {
            startCallDetectionService()
        }
    }
}