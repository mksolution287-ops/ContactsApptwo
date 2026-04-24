package com.callerinfocom.ui.overlay

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callerinfocom.ui.components.NativeAdCard
import kotlin.math.roundToInt
import com.callerinfocom.R

@Composable
fun PostCallOverlayScreen(
    number         : String,
    name           : String?,
    photoUri       : String?,
    durationSec    : Long,
    callType       : String,
    isWhatsAppUser : Boolean,
    isContactSaved: Boolean,
    onDismiss      : () -> Unit,
    onCallBack     : () -> Unit,
    onSms          : () -> Unit,
    onWhatsApp     : () -> Unit,
    onAddContact   : () -> Unit,
    onBlock        : () -> Unit
) {
    // Drag-to-dismiss state
    var dragOffsetPx by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue   = dragOffsetPx,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "drag_offset"
    )



    // Scrim + sheet anchored at bottom
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = true,
            enter   = slideInVertically(tween(360, easing = FastOutSlowInEasing)) { it }
                    + fadeIn(tween(260))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, animatedOffset.roundToInt()) }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffsetPx > 120f) onDismiss()
                                else dragOffsetPx = 0f
                            }
                        ) { _, delta ->
                            if (delta > 0)
                                dragOffsetPx = (dragOffsetPx + delta).coerceAtMost(400f)
                        }
                    }
            ) {
                // ── Hero section (dark) ───────────────────────────────
                HeroSection(
                    name        = name,
                    number      = number,
                    durationSec = durationSec,
                    callType    = callType,
                    isWaUser    = isWhatsAppUser
                )

                // ── Action section ────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1: Call back + Quick SMS
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton(
                            modifier    = Modifier.weight(1f),
                            label       = stringResource(R.string.call_back),
                            icon        = Icons.Default.Call,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary,
                            onClick     = onCallBack
                        )
                        PrimaryButton(
                            modifier    = Modifier.weight(1f),
                            label       = stringResource(R.string.quick_sms),
                            icon        = Icons.Default.Message,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick     = onSms
                        )
                    }

                    // Row 2: WhatsApp (conditional full-width)
                    if (isWhatsAppUser) {
                        WhatsAppButton(onClick = onWhatsApp)
                    }

                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    // Row 3: Add contact + Block/Report
                    Row(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryIconButton(
                            modifier = Modifier.weight(1f),
                            label = if (isContactSaved) stringResource(R.string.edit_contact) else stringResource(R.string.add_to_contacts),
                            icon = if (isContactSaved) Icons.Default.Edit else Icons.Default.PersonAdd,
                            onClick = onAddContact
                        )
                        SecondaryIconButton(
                            modifier     = Modifier.weight(1f),
                            label        = stringResource(R.string.block_spam),
                            icon         = Icons.Default.Block,
                            onClick      = onBlock,
                            isDangerous  = true
                        )
                    }

                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp
                    )

                    // Dismiss
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = null,
                            modifier           = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.dismiss_btn))
                    }
                }
                NativeAdCard(
                    modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                )
            }
        }
    }
}

// ── Hero section ─────────────────────────────────────────────────────────

@Composable
private fun HeroSection(
    name        : String?,
    number      : String,
    durationSec : Long,
    callType    : String,
    isWaUser    : Boolean
) {
    val heroBg = Color(0xFF1A1A2E)

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .background(heroBg)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Drag pill
        Box(
            Modifier
                .width(36.dp).height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.2f))
        )

        Spacer(Modifier.height(4.dp))

        // Avatar
        AvatarCircle(name = name, size = 72)

        // Name / number
        Text(
            text       = name ?: number,
            fontSize   = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color      = Color(0xFFF1F5F9),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
        if (name != null) {
            Text(
                text     = number,
                fontSize = 14.sp,
                color    = Color(0xFF94A3B8)
            )
        }

        // Chips row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            CallTypeChip(callType)
            if (isWaUser) WaChip()
        }

        // Duration
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.AccessTime,
                contentDescription = null,
                tint               = Color(0xFF64748B),
                modifier           = Modifier.size(14.dp)
            )
            Text(
                text     = "Duration  ${formatDuration(durationSec)}",
                fontSize = 13.sp,
                color    = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun AvatarCircle(name: String?, size: Int) {
    val initials = name
        ?.split(" ")
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.take(2)
        ?.joinToString("") ?: "#"

    Box(
        modifier         = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(0xFF2D2D50)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = initials,
            fontSize   = (size * 0.33f).sp,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFFA5B4FC)
        )
    }
}

@Composable
private fun CallTypeChip(callType: String) {
    val (label, bg, fg) = when (callType) {
        "missed"   -> Triple(stringResource(R.string.call_type_missed),   Color(0x30E24B4A), Color(0xFFF09595))
        "outgoing" -> Triple(stringResource(R.string.call_type_outgoing), Color(0x301D9E75), Color(0xFF5DCAA5))
        else       -> Triple(stringResource(R.string.call_type_incoming), Color(0x30378ADD), Color(0xFF85B7EB))
    }
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Text(
            text     = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color    = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WaChip() {
    Surface(
        color = Color(0x2625D366),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text     = "WhatsApp",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color    = Color(0xFF4ADE80),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// ── Action buttons ────────────────────────────────────────────────────────

@Composable
private fun PrimaryButton(
    modifier       : Modifier,
    label          : String,
    icon           : ImageVector,
    containerColor : Color,
    contentColor   : Color,
    onClick        : () -> Unit
) {
    Button(
        onClick  = onClick,
        modifier = modifier.height(50.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 14.sp)
    }
}

@Composable
private fun WhatsAppButton(onClick: () -> Unit) {
    val waGreen = Color(0xFF25D366)
    Button(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = waGreen.copy(alpha = 0.12f),
            contentColor   = waGreen
        )
    ) {
        // WhatsApp icon via Canvas (no external dep needed)
//        WhatsAppIcon(size = 18.dp)
        Icon(
            painter           = painterResource(R.drawable.ic_whatsapp),
            contentDescription = null,
            tint              = Color.Unspecified, // preserve original green
            modifier          = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.msg_whatsapp), fontSize = 14.sp, color = Color(0xFF15803D))
    }
}

@Composable
private fun WhatsAppIcon(size: androidx.compose.ui.unit.Dp) {
    // Simple phone-in-bubble SVG path via Canvas
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(color = Color(0xFF25D366), radius = r)
    }
    // (for a proper icon, add the WhatsApp SVG path via your asset or a vector drawable)
}

@Composable
private fun SecondaryIconButton(
    modifier    : Modifier,
    label       : String,
    icon        : ImageVector,
    onClick     : () -> Unit,
    isDangerous : Boolean = false
) {
    val contentColor = if (isDangerous)
        MaterialTheme.colorScheme.error
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedButton(
        onClick        = onClick,
        modifier       = modifier.height(68.dp),
        shape          = RoundedCornerShape(12.dp),
        colors         = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private fun formatDuration(sec: Long): String = when {
    sec <= 0  -> "0 sec"
    sec < 60  -> "$sec sec"
    else      -> "${sec / 60} min ${sec % 60} sec"
}