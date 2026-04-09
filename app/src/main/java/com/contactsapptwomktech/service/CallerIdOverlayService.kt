package com.contactsapptwomktech.service

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.contactsapptwomktech.R

class CallerIdOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    private val CHANNEL_ID = "caller_id_channel"
    private val NOTIF_ID = 101

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
        registerPhoneStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Outgoing call number passed via Intent from OutgoingCallReceiver
        intent?.getStringExtra(EXTRA_PHONE_NUMBER)?.let { number ->
            val isIncoming = intent.getBooleanExtra(EXTRA_IS_INCOMING, true)
            if (overlayView == null) showOverlay(number, isIncoming)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    // ── Phone state listener ──────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListener() {
        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        showOverlay(phoneNumber.orEmpty(), isIncoming = true)
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        // Outgoing number delivered via Intent; show placeholder if nothing yet
                        if (overlayView == null) showOverlay("", isIncoming = false)
                    }
                    TelephonyManager.CALL_STATE_IDLE -> removeOverlay()
                }
            }
        }
        @Suppress("DEPRECATION")
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    // ── Overlay ───────────────────────────────────────────────────────────────

    private fun showOverlay(phoneNumber: String, isIncoming: Boolean) {
        if (overlayView != null) return
        if (!android.provider.Settings.canDrawOverlays(this)) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }

        val view = buildOverlayView(phoneNumber, isIncoming)
        overlayView = view

        TranslateAnimation(0f, 0f, -300f, 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator(1.5f)
            view.startAnimation(this)
        }

        windowManager.addView(view, params)
        Handler(Looper.getMainLooper()).postDelayed({ removeOverlay() }, 8000)
    }

    private fun buildOverlayView(phoneNumber: String, isIncoming: Boolean): View {
        // ── Contact lookup ─────────────────────────────────────────────────────
        // Uses PhoneLookup.CONTENT_FILTER_URI — same as ContactsRepository —
        // which handles country-code normalisation internally.
        val contactName: String? = lookupContactName(phoneNumber)
        val isSpam = false

        val displayName = contactName ?: phoneNumber.ifEmpty { "Unknown Number" }

        val avatarInitial = when {
            contactName != null && contactName.isNotEmpty() -> contactName[0].uppercaseChar().toString()
            phoneNumber.isNotEmpty() -> "#"
            else -> "?"
        }
        val avatarColor = when {
            isSpam       -> Color.parseColor("#C62828")
            contactName != null -> Color.parseColor("#1B8C3C")
            else         -> Color.parseColor("#455A64")
        }

        // ── Root card ──────────────────────────────────────────────────────────
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            clipChildren = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(16).toFloat()
                setColor(Color.parseColor("#1A1A1A"))
                setStroke(dp(1), Color.parseColor("#333333"))
            }
            elevation = dp(10).toFloat()
        }

        val wrapper = FrameLayout(this).apply {
            val m = dp(10)
            setPadding(m, m, m, 0)
        }

        // ── Avatar ─────────────────────────────────────────────────────────────
        val avatar = TextView(this).apply {
            text = avatarInitial
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(avatarColor)
            }
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).also {
                it.marginEnd = dp(12)
                it.gravity = Gravity.CENTER_VERTICAL
            }
        }

        // ── Info column ────────────────────────────────────────────────────────
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.gravity = Gravity.CENTER_VERTICAL
            }
        }

        infoCol.addView(TextView(this).apply {
            text = if (isIncoming) "▼  Incoming Call" else "▲  Outgoing Call"
            textSize = 10.5f
            setTextColor(Color.parseColor("#AAAAAA"))
            letterSpacing = 0.08f
        })

        infoCol.addView(TextView(this).apply {
            text = displayName
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })

        // Raw number line — only when a contact name was resolved
        infoCol.addView(TextView(this).apply {
            text = phoneNumber
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            visibility = if (contactName != null && phoneNumber.isNotEmpty()) View.VISIBLE else View.GONE
        })

        // Tag chip
        val tagText = when {
            isSpam -> "⚠  Likely Spam"
            contactName != null -> "✓  Saved Contact"
            phoneNumber.isEmpty() -> "•  Dialling…"
            else -> "?  Unknown Number"
        }
        val tagColor = when {
            isSpam -> Color.parseColor("#FF5252")
            contactName != null -> Color.parseColor("#4CAF50")
            phoneNumber.isEmpty() -> Color.parseColor("#90A4AE")
            else -> Color.parseColor("#FFA726")
        }
        val tagBg = when {
            isSpam -> Color.parseColor("#1A0000")
            contactName != null -> Color.parseColor("#0A1F0A")
            phoneNumber.isEmpty() -> Color.parseColor("#0D1214")
            else -> Color.parseColor("#1F1600")
        }

        infoCol.addView(TextView(this).apply {
            text = tagText
            textSize = 11f
            setTextColor(tagColor)
            setPadding(dp(7), dp(2), dp(7), dp(2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(4).toFloat()
                setColor(tagBg)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = dp(4) }
        })

        // ── Close button ───────────────────────────────────────────────────────
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 14f
            setTextColor(Color.parseColor("#777777"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).also {
                it.gravity = Gravity.CENTER_VERTICAL
            }
            setOnClickListener { removeOverlay() }
        }

        root.addView(avatar)
        root.addView(infoCol)
        root.addView(closeBtn)
        wrapper.addView(root)
        return wrapper
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            TranslateAnimation(0f, 0f, 0f, -300f).apply {
                duration = 250
                interpolator = AccelerateInterpolator()
                view.startAnimation(this)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                try { windowManager.removeView(view) } catch (_: Exception) {}
                overlayView = null
            }, 240)
        }
    }

    // ── Contact lookup ────────────────────────────────────────────────────────
    //
    // Mirror of ContactsRepository: uses PhoneLookup.CONTENT_FILTER_URI which
    // is the correct API for number-to-name resolution. It internally normalises
    // country codes, spaces and dashes, so "+91 98765-43210" will match
    // "9876543210" stored in contacts.
    //
    // Root cause of "Unknown" bug: the previous implementation called
    // getColumnIndexOrThrow() which throws when the column is absent on some
    // OEM ROMs. We now use getColumnIndex() with a >= 0 guard, matching exactly
    // how ContactsRepository reads DISPLAY_NAME_PRIMARY.

    private fun lookupContactName(rawNumber: String): String? {
        if (rawNumber.isBlank()) return null
        return try {
            val lookupUri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(rawNumber)
            )
            contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx).takeIf { it.isNotBlank() } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Caller ID Service", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Running Caller ID in background" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Caller ID Active")
            .setContentText("Showing caller info on incoming calls")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_IS_INCOMING  = "extra_is_incoming"
    }
}