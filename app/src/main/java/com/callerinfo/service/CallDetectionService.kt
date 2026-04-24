package com.callerinfo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.callerinfo.R
import com.callerinfo.data.model.CallType
import com.callerinfo.data.repository.CallLogRepository
import com.callerinfo.ui.overlay.*
import kotlinx.coroutines.*
import android.content.Context


class CallDetectionService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var phoneStateListener: PhoneStateListener
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var callInProgress = false
    private var incomingNumber = ""

    override fun onCreate() {
        super.onCreate()
//        startForeground(NOTIF_ID, buildNotification())
//        setupPhoneListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
        setupPhoneListener()
    }

//    private fun setupPhoneListener() {
//        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
//
//        phoneStateListener = object : PhoneStateListener() {
//            @Deprecated("Deprecated in Java")
//            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
//                when (state) {
//                    TelephonyManager.CALL_STATE_RINGING -> {
//                        incomingNumber = phoneNumber ?: ""
//                    }
//                    TelephonyManager.CALL_STATE_OFFHOOK -> {
//                        callInProgress = true
//                    }
//                    TelephonyManager.CALL_STATE_IDLE -> {
//                        if (callInProgress) {
//                            callInProgress = false
//                            // Small delay so the call log is written first
//                            scope.launch {
//                                delay(1_500)
//                                fetchLastCallAndShowOverlay()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        @Suppress("DEPRECATION")
//        telephonyManager.listen(
//            phoneStateListener,
//            PhoneStateListener.LISTEN_CALL_STATE
//        )
//    }

    private fun setupPhoneListener() {
        if (checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        phoneStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        incomingNumber = phoneNumber ?: ""
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        callInProgress = true
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (callInProgress) {
                            callInProgress = false
                            scope.launch {
                                delay(1_500)
                                fetchLastCallAndShowOverlay()
                            }
                        }
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        telephonyManager.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

private suspend fun fetchLastCallAndShowOverlay() {

    val prefs = getSharedPreferences("theme_settings", MODE_PRIVATE)
    if (!prefs.getBoolean("callback_screen_enabled", true)) return  // ← add this

    val repo = CallLogRepository(applicationContext)
    val latest = repo.fetchCallLogs(limit = 1).firstOrNull() ?: return

    // Convert your CallType enum to the string expected by PostCallOverlayActivity
    val callTypeString = when (latest.callType) {
        CallType.INCOMING  -> "incoming"
        CallType.OUTGOING  -> "outgoing"
        CallType.MISSED    -> "missed"
        else               -> "incoming"   // fallback for REJECTED, VOICEMAIL, UNKNOWN
    }

    val intent = Intent(applicationContext, PostCallOverlayActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

        putExtra(EXTRA_NUMBER,       latest.number)
        putExtra(EXTRA_NAME,         latest.contactName)
        putExtra(EXTRA_PHOTO_URI,    latest.photoUri)
        putExtra(EXTRA_DURATION_SEC, latest.duration)
        putExtra(EXTRA_CALL_TYPE,    callTypeString)
    }

    startActivity(intent)
}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

//    override fun onDestroy() {
//        @Suppress("DEPRECATION")
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
//        scope.cancel()
//        super.onDestroy()
//    }
override fun onDestroy() {
    if (::telephonyManager.isInitialized && ::phoneStateListener.isInitialized) {
        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }
    scope.cancel()
    super.onDestroy()
}

    // ── Foreground notification (required on Android 8+) ─────────────────

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Call monitoring",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call monitoring active")
            .setSmallIcon(R.drawable.ic_call)   // use any icon you have
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID   = 1001
        private const val CHANNEL_ID = "call_detection_channel"
    }


    // Inside CallDetectionService.kt

    private fun showCallerIdNotification(number: String, callerName: String?) {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val callerIdEnabled = prefs.getBoolean("caller_id_enabled", false)
        if (!callerIdEnabled) return

        val channelId = "caller_id_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Caller ID",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows caller information"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val displayName = callerName ?: number
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_call)           // use your call icon
            .setContentTitle("📞 Incoming Call")
            .setContentText(displayName)
            .setSubText(number)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(101, notification)
    }

}