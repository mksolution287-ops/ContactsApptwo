package com.callerinfocom.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * This service is what makes the app appear in Android's
 * "Caller ID & spam" default app list under Settings → Apps → Default apps.
 *
 * The system discovers this service via the intent-filter
 * "android.telecom.CallScreeningService" declared in AndroidManifest.xml.
 *
 * When the user sets this app as the default Caller ID app, Android calls
 * [onScreenCall] for every incoming/outgoing call. We use it as a hook to
 * launch the CallerIdOverlayService with the caller's number.
 *
 * We never block or reject calls here — we always respond with
 * CallResponse.Builder().build() (allow everything).
 */
@RequiresApi(Build.VERSION_CODES.N)
class AppCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // ── 1. Always allow the call — never block ─────────────────────────
        respondToCall(
            callDetails,
            CallResponse.Builder()
                .setDisallowCall(false)
                .setRejectCall(false)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
        )

        // ── 2. Extract phone number ────────────────────────────────────────
        val phoneNumber: String = try {
            callDetails.handle?.schemeSpecificPart ?: ""
        } catch (_: Exception) { "" }

        // ── 3. Determine call direction ────────────────────────────────────
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            // Pre-Q: CallScreeningService is only invoked for incoming calls
            true
        }

        // Add this check before the overlayIntent block
        val prefs = getSharedPreferences("theme_settings", MODE_PRIVATE)
        val overlayEnabled = prefs.getBoolean("caller_id_overlay_enabled", false)

        if (!overlayEnabled) return  //

        // ── 4. Launch overlay service with the number ──────────────────────
        val overlayIntent = Intent(this, CallerIdOverlayService::class.java).apply {
            putExtra(CallerIdOverlayService.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(CallerIdOverlayService.EXTRA_IS_INCOMING, isIncoming)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(overlayIntent)
            } else {
                startService(overlayIntent)
            }
        } catch (_: Exception) {
            // Overlay permission not granted yet — silently skip
        }
    }
}