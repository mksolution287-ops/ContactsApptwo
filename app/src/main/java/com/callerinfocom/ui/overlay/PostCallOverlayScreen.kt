package com.callerinfocom.ui.overlay

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.callerinfocom.R
import com.callerinfocom.data.model.CallLogEntry
import com.callerinfocom.data.model.CallType
import com.callerinfocom.ui.components.AppInstallNativeAdCard
import com.callerinfocom.utils.IntentUtils
import java.util.Date

private val AccentGreen  = Color(0xFF22C55E)   // Avatar / accent text color (matches mockup)
private val IncomingBlue = Color(0xFF3B82F6)   // Blue arrow for incoming calls
private val MissedRed    = Color(0xFFEF4444)   // Red arrow for missed calls


@Composable
fun PostCallOverlayScreen(
    number         : String,
    name           : String?,
    photoUri       : String?,
    durationSec    : Long,
    callType       : String,
    isWhatsAppUser : Boolean,
    isContactSaved : Boolean,
    callHistory    : List<CallLogEntry>,
    onDismiss      : () -> Unit,
    onCallBack     : () -> Unit,
    onSms          : () -> Unit,
    onWhatsApp     : () -> Unit,
    onAddContact   : () -> Unit,
    onBlock        : () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top header row ────────────────────────────────────────
            HeaderRow(
                photoUri    = photoUri,
                name        = name,
                number      = number,
                durationSec = durationSec,
                callType    = callType,
                onClose     = onDismiss
            )

            Spacer(Modifier.height(20.dp))

            // ── Quick action row ──────────────────────────────────────
            QuickActionRow(
                isContactSaved = isContactSaved,
                isWhatsAppUser = isWhatsAppUser,
                onCall         = onCallBack,
                onMessage      = onSms,
                onAddContact   = onAddContact,
                onWhatsApp     = onWhatsApp
            )

            Spacer(Modifier.height(28.dp))

            // ── Call History section ──────────────────────────────────
            Text(
                text       = "Call History",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(12.dp))

            if (callHistory.isEmpty()) {
                Text(
                    text     = "No previous calls",
                    fontSize = 14.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            } else {
                callHistory.forEach { entry ->
                    CallHistoryRow(entry = entry,
                        onCallClick = {
                            IntentUtils.makeCall(
                                context = context,
                                entry.number,
                                context.getString(R.string.error_no_phone_app)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Banner ad ─────────────────────────────────────────────
            AppInstallNativeAdCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────

@Composable
private fun HeaderRow(
    photoUri    : String?,
    name        : String?,
    number      : String,
    durationSec : Long,
    callType    : String,
    onClose     : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar — show contact photo if available, otherwise green Person icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(AccentGreen),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = name ?: number,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(34.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = name ?: number,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = formatHms(durationSec),
                    fontSize = 13.sp,
                    color    = AccentGreen
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text     = formatNowTime(),
                    fontSize = 13.sp,
                    color    = AccentGreen
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text     = callTypeLabel(callType),
                    fontSize = 13.sp,
                    color    = AccentGreen
                )
            }
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = "Close",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Quick action row (4 circular buttons) ────────────────────────────────

@Composable
private fun QuickActionRow(
    isContactSaved : Boolean,
    isWhatsAppUser : Boolean,
    onCall         : () -> Unit,
    onMessage      : () -> Unit,
    onAddContact   : () -> Unit,
    onWhatsApp     : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment     = Alignment.Top
    ) {
        QuickActionItem(
            icon    = Icons.Default.Call,
            label   = "Call",
            onClick = onCall
        )
        QuickActionItem(
            icon    = Icons.Default.ChatBubbleOutline,
            label   = "Message",
            onClick = onMessage
        )
        QuickActionItem(
            icon    = if (isContactSaved) Icons.Default.Edit else Icons.Default.PersonAddAlt1,
            label   = if (isContactSaved) "Edit Contact" else "Add Contact",
            onClick = onAddContact
        )
        if (isWhatsAppUser) {
            QuickActionItem(
                iconPainter         = R.drawable.ic_whatsapp,
                label               = "WhatsApp",
                circleColor         = Color(0xFF25D366),
                useOriginalIconTint = true,
                iconSize            = 26,
                onClick             = onWhatsApp
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon                : ImageVector? = null,
    iconPainter         : Int? = null,
    label               : String,
    circleColor         : Color = MaterialTheme.colorScheme.surfaceVariant,
    useOriginalIconTint : Boolean = false,
    iconSize            : Int = 22,
    onClick             : () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(circleColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when {
                iconPainter != null -> Icon(
                    painter            = painterResource(iconPainter),
                    contentDescription = label,
                    tint               = if (useOriginalIconTint) Color.Unspecified
                    else MaterialTheme.colorScheme.onSurface,
                    modifier           = Modifier.size(iconSize.dp)
                )
                icon != null -> Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = MaterialTheme.colorScheme.onSurface,
                    modifier           = Modifier.size(iconSize.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurface,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

// ── Call history row ─────────────────────────────────────────────────────

@Composable
private fun CallHistoryRow(
    entry       : CallLogEntry,
    onCallClick : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = directionIcon(entry.callType),
            contentDescription = null,
            tint               = directionTint(entry.callType),
            modifier           = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = formatNumber(entry.number),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = formatTime(entry.date),
                fontSize = 13.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onCallClick) {
            Icon(
                imageVector        = Icons.Default.Call,
                contentDescription = "Call",
                tint               = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

private fun directionIcon(type: CallType): ImageVector = when (type) {
    CallType.INCOMING -> Icons.Default.CallReceived
    CallType.OUTGOING -> Icons.Default.CallMade
    CallType.MISSED   -> Icons.Default.CallMissed
    CallType.REJECTED -> Icons.Default.CallMissed
    else              -> Icons.Default.Call
}

private fun directionTint(type: CallType): Color = when (type) {
    CallType.INCOMING -> IncomingBlue
    CallType.OUTGOING -> IncomingBlue
    CallType.MISSED   -> MissedRed
    CallType.REJECTED -> MissedRed
    else              -> IncomingBlue
}

private fun callTypeLabel(callType: String): String = when (callType.lowercase()) {
    "missed"   -> "Missed Call"
    "outgoing" -> "Outgoing Call"
    else       -> "Incoming Call"
}

/** Pretty-print a number like "+918827011244" → "+91 88270 11244" (best-effort). */
private fun formatNumber(raw: String): String {
    val digits = raw.filter { it.isDigit() || it == '+' }
    if (digits.startsWith("+91") && digits.length >= 13) {
        val cc   = digits.substring(0, 3)
        val mid  = digits.substring(3, 8)
        val tail = digits.substring(8)
        return "$cc $mid $tail"
    }
    return raw
}

/** "00:03" style for short calls, "01:02:03" for hour-long. */
private fun formatHms(sec: Long): String {
    if (sec <= 0) return "00:00"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
    else              String.format("%02d:%02d", m, s)
}

private fun formatNowTime(): String =
    DateFormat.format("hh:mm a", Date()).toString()

private fun formatTime(epochMs: Long): String =
    DateFormat.format("hh:mm a", Date(epochMs)).toString()