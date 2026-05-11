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

class UninstallTrampolineActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeSettings by settingsViewModel.themeSettings.collectAsState()
            ContactsAppTheme(settings = themeSettings) {

                var step by remember { mutableStateOf(UninstallStep.Retention) }
                var pendingReasonKey by remember { mutableStateOf("") }


                when (step) {
                    UninstallStep.Retention -> UninstallRetentionScreen(
                        onBack          = { finish() },
                        onHome          = { openMainApp("home") },
                        onTryStorage    = { openMainApp("try_storage") },
                        onTryContacts   = { openMainApp("try_contacts") },
                        onTryUi         = { openMainApp("try_ui") },
                        onKeepApp       = {
                            logEvent("uninstall_kept_app")
                            openMainApp("onKeepApp")
                        },
                        onContinueUninstall = { step = UninstallStep.Feedback }
                    )

                    // In Feedback step:
                    UninstallStep.Feedback -> UninstallFeedbackScreen(
                        onBack    = { step = UninstallStep.Retention },
                        onCancel  = { finish() },
                        onConfirm = { reasonKey ->
                            pendingReasonKey = reasonKey          // store for later
                            logUninstallReason(reasonKey)
                            step = UninstallStep.ThankYou         // show thank-you first
                        }
                    )

// Add ThankYou step:
                    UninstallStep.ThankYou -> UninstallThankYouScreen(
                        onTimeout = {
                            openSystemAppDetails()
                            finish()
                        }
                    )
                }
            }
        }
    }

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

private enum class UninstallStep { Retention, Feedback, ThankYou }