package com.callerinfocom.service

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
import com.callerinfocom.R

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
//        startForeground(NOTIF_ID, buildNotification())
//        registerPhoneStateListener()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
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
        val contactName: String? = lookupContactName(phoneNumber)
        val displayName = contactName ?: phoneNumber.ifEmpty { "Unknown Number" }
        val subLabel = if (contactName != null && phoneNumber.isNotEmpty()) phoneNumber else ""

        // ── Root card (green gradient background) ──────────────────────────────
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false
            clipToPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                colors = intArrayOf(
                    Color.parseColor("#22C55E"),
                    Color.parseColor("#16A34A")
                )
                orientation = GradientDrawable.Orientation.TL_BR
            }
            elevation = dp(12).toFloat()
        }

        // ── Top section: avatar + info + close ────────────────────────────────
        val topRow = FrameLayout(this).apply {
            setPadding(dp(20), dp(28), dp(20), dp(22))
        }

        val rowInner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Avatar circle (white circle with person icon)
        val avatarBg = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(dp(70), dp(70)).also {
                it.marginEnd = dp(18)
            }
        }

        // Person icon drawn with two shapes: head + body
        val personIcon = object : android.view.View(this) {
            override fun onDraw(canvas: android.graphics.Canvas) {
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#22C55E")
                    style = android.graphics.Paint.Style.FILL
                }
                val cx = width / 2f
                // head
                canvas.drawCircle(cx, height * 0.35f, width * 0.22f, paint)
                // body (ellipse)
                val oval = android.graphics.RectF(
                    cx - width * 0.38f, height * 0.58f,
                    cx + width * 0.38f, height * 1.1f
                )
                canvas.drawOval(oval, paint)
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(dp(70), dp(70))
        }
        avatarBg.addView(personIcon)

        // Info column
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        infoCol.addView(TextView(this).apply {
            text = displayName
            textSize = 20f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        })

        if (subLabel.isNotEmpty()) {
            infoCol.addView(TextView(this).apply {
                text = subLabel
                textSize = 14f
                setTextColor(Color.parseColor("#E0FFE0"))
                setPadding(0, dp(4), 0, 0)
            })
        }

        // Close button (top-right)
        val closeBtn = TextView(this).apply {
            text = "✕"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.argb(0x2D, 0xFF, 0xFF, 0xFF))
            }
            layoutParams = FrameLayout.LayoutParams(dp(34), dp(34)).also {
                it.gravity = Gravity.TOP or Gravity.END
            }
            setOnClickListener { removeOverlay() }
        }

        rowInner.addView(avatarBg)
        rowInner.addView(infoCol)
        topRow.addView(rowInner, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
        topRow.addView(closeBtn)
        root.addView(topRow)

        // ── Bottom strip: "Mobile" label ──────────────────────────────────────
        val bottomStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(22), dp(12), dp(22), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                // Rounded only on bottom corners
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat(), dp(24).toFloat())
                setColor(android.graphics.Color.argb(0x2D, 0xFF, 0xFF, 0xFF))
            }
        }

        bottomStrip.addView(TextView(this).apply {
            text = when {
                isIncoming -> "Mobile"
                else -> "Mobile"
            }
            textSize = 15f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        root.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
            )
        })
        root.addView(bottomStrip)

        // ── Outer wrapper with margin ──────────────────────────────────────────
        val wrapper = FrameLayout(this).apply {
            val m = dp(10)
            setPadding(m, m, m, 0)
        }
        wrapper.addView(root, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ))
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