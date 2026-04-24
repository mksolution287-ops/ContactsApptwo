//package com.contactsapp
//
//import android.content.Context
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.viewModels
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Modifier
//import androidx.core.view.WindowCompat
//import com.contactsapp.data.viewmodel.SettingsViewModel
//import com.contactsapp.data.viewmodel.ContactsViewModel
//import com.contactsapp.ui.screens.MainScreen
//import com.contactsapp.ui.theme.ContactsAppTheme
//import com.contactsapp.utils.BaseContextWrapper
//
//class MainActivity : ComponentActivity() {
//
//    private val contactsViewModel: ContactsViewModel by viewModels()
//    private val settingsViewModel: SettingsViewModel by viewModels()
//
//    override fun attachBaseContext(newBase: Context) {
//        super.attachBaseContext(
//            BaseContextWrapper.wrap(newBase)
//        )
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//
//        setContent {
//            val themeSettings by settingsViewModel.themeSettings.collectAsState()
//
//            ContactsAppTheme(settings = themeSettings) {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    MainScreen(
//                        rootViewModel    = contactsViewModel,
//                        settingsViewModel = settingsViewModel
//                    )
//                }
//            }
//        }
//    }
//}

package com.callerinfo

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
import com.callerinfo.data.viewmodel.ContactsViewModel
import com.callerinfo.data.viewmodel.SettingsViewModel
import com.callerinfo.service.CallDetectionService
import com.callerinfo.ui.screens.MainScreen
import com.callerinfo.ui.theme.ContactsAppTheme
import com.callerinfo.utils.AnalyticsHelper
import com.callerinfo.utils.AppOpenAdManager
import com.callerinfo.utils.BaseContextWrapper
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : ComponentActivity() {

    private val contactsViewModel: ContactsViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    lateinit var firebaseAnalytics: FirebaseAnalytics
        private set

    // ── App Open Ad Manager ───────────────────────────────────────────────
    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(BaseContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        firebaseAnalytics = Firebase.analytics
        AnalyticsHelper.init(firebaseAnalytics)
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)


        // ==================== Set Device & Model Identifier ====================
        setDeviceInfoToCrashlytics(crashlytics)

        // Optional: Log app open
//        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {}

        // ── Restore persisted settings ──────────────────────────────
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val callerIdOn = prefs.getBoolean("caller_id_enabled", false)
        settingsViewModel.setCallerIdEnabled(callerIdOn)


        setContent {
            val themeSettings by settingsViewModel.themeSettings.collectAsState()

            ContactsAppTheme(settings = themeSettings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        rootViewModel = contactsViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    private fun setDeviceInfoToCrashlytics(crashlytics: FirebaseCrashlytics) {
        try {
            val manufacturer = Build.MANUFACTURER ?: "Unknown"
            val model = Build.MODEL ?: "Unknown"
            val deviceName = "$manufacturer $model"

            crashlytics.setCustomKey("device_manufacturer", manufacturer)
            crashlytics.setCustomKey("device_model", model)
            crashlytics.setCustomKey("device_name", deviceName)        // Most useful
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