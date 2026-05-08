package com.callerinfocom.ui.uninstall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.callerinfocom.MainActivity
import com.callerinfocom.data.viewmodel.SettingsViewModel
import com.callerinfocom.ui.theme.ContactsAppTheme
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * Activity launched by the "Uninstall" long-press shortcut.
 *
 * Hosts a 2-step flow:
 *   1. Retention screen — "We're truly sorry!" with 3 fixable reasons.
 *      - "Try" buttons → open the app (MainActivity).
 *      - "Don't uninstall yet" → finish, keep the app.
 *      - "Still want to uninstall" → step 2.
 *   2. Feedback screen — "Why do you uninstall?" with radio reasons.
 *      - "Cancel" → finish.
 *      - "Uninstall" → log reason to analytics, open system app-details
 *        page where the user can tap the OS-level Uninstall button.
 */
class UninstallTrampolineActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeSettings by settingsViewModel.themeSettings.collectAsState()
            ContactsAppTheme(settings = themeSettings) {

                var step by remember { mutableStateOf(UninstallStep.Retention) }

                when (step) {
                    UninstallStep.Retention -> UninstallRetentionScreen(
                        onBack          = { finish() },
                        onHome          = { finish() },
                        onTryStorage    = { openMainApp("try_storage") },
                        onTryContacts   = { openMainApp("try_contacts") },
                        onTryUi         = { openMainApp("try_ui") },
                        onKeepApp       = {
                            logEvent("uninstall_kept_app")
                            finish()
                        },
                        onContinueUninstall = { step = UninstallStep.Feedback }
                    )

                    UninstallStep.Feedback -> UninstallFeedbackScreen(
                        onBack    = { step = UninstallStep.Retention },
                        onCancel  = { finish() },
                        onConfirm = { reasonKey ->
                            logUninstallReason(reasonKey)
                            openSystemAppDetails()
                            finish()
                        }
                    )
                }
            }
        }
    }

    /**
     * Opens MainActivity (brings the app to the foreground) and finishes the
     * trampoline so the back stack stays clean. Logs which "Try" button was
     * tapped so you can see in Analytics which retention angle works.
     */
    private fun openMainApp(source: String) {
        logEvent("uninstall_try_tapped") {
            param("source", source)
        }
        try {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        } catch (_: Exception) {
            // Fall back to the launcher intent if the direct class reference
            // fails for any reason (build variants, renamed activity, etc.)
            packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
        }
        finish()
    }

    private fun logUninstallReason(reasonKey: String) {
        logEvent("uninstall_reason_selected") {
            param("reason", reasonKey)
        }
    }

    private inline fun logEvent(
        name: String,
        crossinline block: com.google.firebase.analytics.ParametersBuilder.() -> Unit = {}
    ) {
        Firebase.analytics.logEvent(name) { block() }
    }

    /**
     * Opens the system app-details page for this app. The user taps the OS-level
     * "Uninstall" button there — Android requires this; an app cannot uninstall
     * itself silently.
     */
    private fun openSystemAppDetails() {
        try {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        } catch (_: Exception) {
            try {
                @Suppress("DEPRECATION")
                startActivity(
                    Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
            } catch (_: Exception) { /* nothing else we can do */ }
        }
    }
}

private enum class UninstallStep { Retention, Feedback }