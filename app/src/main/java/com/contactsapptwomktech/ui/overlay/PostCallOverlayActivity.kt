package com.contactsapptwomktech.ui.overlay

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.contactsapptwomktech.ui.theme.ContactsAppTheme

// ── Intent extras ────────────────────────────────────────────────────────
const val EXTRA_NUMBER       = "extra_number"
const val EXTRA_NAME         = "extra_name"
const val EXTRA_PHOTO_URI    = "extra_photo_uri"
const val EXTRA_DURATION_SEC = "extra_duration_sec"
const val EXTRA_CALL_TYPE    = "extra_call_type" // "incoming" | "outgoing" | "missed"

class PostCallOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON   or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Important flags to make this activity transient
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        val number      = intent.getStringExtra(EXTRA_NUMBER)       ?: "Unknown"
        val name        = intent.getStringExtra(EXTRA_NAME)
        val photoUri    = intent.getStringExtra(EXTRA_PHOTO_URI)
        val durationSec = intent.getLongExtra(EXTRA_DURATION_SEC, 0L)
        val callType    = intent.getStringExtra(EXTRA_CALL_TYPE)    ?: "incoming"
        val isWaUser    = isWhatsAppInstalled() && isWhatsAppContact(number)

        setContent {
            ContactsAppTheme {
                PostCallOverlayScreen(
                    number      = number,
                    name        = name,
                    photoUri    = photoUri,
                    durationSec = durationSec,
                    callType    = callType,
                    isWhatsAppUser = isWaUser,
                    onDismiss   = { finishAndRemoveTask() },
                    onCallBack  = { launchCall(number) },
                    onSms       = { launchSms(number) },
                    onWhatsApp  = { launchWhatsApp(number) },
                    onAddContact = { launchAddContact(number) },
                    onBlock     = { launchBlockFlow(number) }
                )
            }
        }
    }

    // ── WhatsApp detection ────────────────────────────────────────────────

    private fun isWhatsAppInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
    }

    /**
     * Queries WhatsApp's contact provider to check if this number is registered.
     * Falls back to true (show button) if query fails — better UX than silently hiding.
     */
    private fun isWhatsAppContact(number: String): Boolean {
        return try {
            val uri = Uri.parse("content://com.whatsapp.provider.contact/wa_contacts")
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val waNum = it.getString(it.getColumnIndexOrThrow("number")) ?: continue
                    // Strip non-digits for loose comparison
                    if (waNum.filter(Char::isDigit).endsWith(number.filter(Char::isDigit).takeLast(10)))
                        return@use true
                }
                false
            } ?: true
        } catch (e: Exception) { true }
    }

    // ── Intent launchers ─────────────────────────────────────────────────

    private fun launchCall(number: String) {
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun launchSms(number: String) {
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun launchWhatsApp(number: String) {
        // Normalise: strip leading zeros/+, keep digits only
        val digits = number.filter(Char::isDigit).trimStart('0')
        val waUri  = Uri.parse("https://wa.me/$digits")
        startActivity(Intent(Intent.ACTION_VIEW, waUri).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun launchAddContact(number: String) {
        startActivity(
            Intent(Intent.ACTION_INSERT_OR_EDIT, android.provider.ContactsContract.Contacts.CONTENT_URI).apply {
                putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    private fun launchBlockFlow(number: String) {
        // Wire to your BlockRepository / system block dialog here
        // e.g. TelecomManager.createManageBlockedNumbersIntent()
        finish()
    }
}