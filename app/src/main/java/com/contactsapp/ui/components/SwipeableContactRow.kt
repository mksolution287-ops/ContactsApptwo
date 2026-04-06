package com.contactsapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Thresholds
// ─────────────────────────────────────────────────────────────────────────────

/** Pixels the user must drag before the action icon becomes fully visible. */
private const val REVEAL_THRESHOLD_PX   = 80f

/** Pixels at which we consider the swipe "confirmed" and trigger the action. */
private const val CONFIRM_THRESHOLD_PX  = 260f

/**
 * Maximum pixels the row can travel in either direction before snapping back.
 * Set to CONFIRM_THRESHOLD so the row triggers exactly at its travel limit.
 */
private const val MAX_OFFSET_PX         = CONFIRM_THRESHOLD_PX

@Composable
fun SwipeableContactRow(
    onCall    : () -> Unit,
    onMessage : () -> Unit,
    modifier  : Modifier = Modifier,
    content   : @Composable () -> Unit
) {
    val scope        = rememberCoroutineScope()
    val offsetX      = remember { Animatable(0f) }

    // Spring used for snap-back and snap-to-confirm
    val snapSpring: SpringSpec<Float> = spring(
        dampingRatio = 0.6f,
        stiffness    = 400f
    )

    // Derived values for icon/label visibility
    val swipeFraction = (abs(offsetX.value) / REVEAL_THRESHOLD_PX).coerceIn(0f, 1f)
    val isSwipingRight = offsetX.value > 0f
    val isSwipingLeft  = offsetX.value < 0f

    // Background color animates between neutral and action colour
    val callBgColor by animateColorAsState(
        targetValue   = if (isSwipingRight)
            Color(0xFF1DB954).copy(alpha = (swipeFraction * 0.9f + 0.1f).coerceIn(0f, 1f))
        else
            Color.Transparent,
        animationSpec = tween(80),
        label         = "call_bg"
    )
    val msgBgColor by animateColorAsState(
        targetValue   = if (isSwipingLeft)
            Color(0xFF1A7FE8).copy(alpha = (swipeFraction * 0.9f + 0.1f).coerceIn(0f, 1f))
        else
            Color.Transparent,
        animationSpec = tween(80),
        label         = "msg_bg"
    )

    // Icon scale pops once threshold is crossed
    val iconScale = if (abs(offsetX.value) >= REVEAL_THRESHOLD_PX)
        (1f + (abs(offsetX.value) - REVEAL_THRESHOLD_PX) / (CONFIRM_THRESHOLD_PX - REVEAL_THRESHOLD_PX) * 0.25f)
            .coerceIn(1f, 1.25f)
    else
        (abs(offsetX.value) / REVEAL_THRESHOLD_PX * 0.85f + 0.15f).coerceIn(0.15f, 1f)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                // Confirm call
                                offsetX.value >= CONFIRM_THRESHOLD_PX -> {
                                    offsetX.animateTo(
                                        size.width.toFloat(), snapSpring
                                    )
                                    onCall()
                                    offsetX.snapTo(0f)
                                }
                                // Confirm message
                                offsetX.value <= -CONFIRM_THRESHOLD_PX -> {
                                    offsetX.animateTo(
                                        -size.width.toFloat(), snapSpring
                                    )
                                    onMessage()
                                    offsetX.snapTo(0f)
                                }
                                // Not far enough — snap back
                                else -> offsetX.animateTo(0f, snapSpring)
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f, snapSpring) }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            val target = (offsetX.value + dragAmount)
                                .coerceIn(-MAX_OFFSET_PX, MAX_OFFSET_PX)
                            offsetX.snapTo(target)
                        }
                    }
                )
            }
    ) {
        // ── Background layers (revealed behind the sliding row) ───────────
        Box(modifier = Modifier.matchParentSize()) {

            // RIGHT swipe → CALL (green, left-aligned)
            Box(
                modifier         = Modifier
                    .matchParentSize()
                    .background(callBgColor),
                contentAlignment = Alignment.CenterStart
            ) {
                if (isSwipingRight) {
                    Row(
                        modifier             = Modifier
                            .padding(start = 24.dp)
                            .alpha(swipeFraction),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(40.dp)
                                .scale(iconScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Call,
                                contentDescription = "Call",
                                tint               = Color.White,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        if (abs(offsetX.value) > REVEAL_THRESHOLD_PX) {
                            Text(
                                text  = if (abs(offsetX.value) >= CONFIRM_THRESHOLD_PX * 0.8f)
                                    "Release to call" else "Call",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(
                                    alpha = ((abs(offsetX.value) - REVEAL_THRESHOLD_PX) /
                                            (CONFIRM_THRESHOLD_PX - REVEAL_THRESHOLD_PX))
                                        .coerceIn(0f, 1f)
                                )
                            )
                        }
                    }
                }
            }

            // LEFT swipe → MESSAGE (blue, right-aligned)
            Box(
                modifier         = Modifier
                    .matchParentSize()
                    .background(msgBgColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isSwipingLeft) {
                    Row(
                        modifier             = Modifier
                            .padding(end = 24.dp)
                            .alpha(swipeFraction),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (abs(offsetX.value) > REVEAL_THRESHOLD_PX) {
                            Text(
                                text  = if (abs(offsetX.value) >= CONFIRM_THRESHOLD_PX * 0.8f)
                                    "Release to message" else "Message",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(
                                    alpha = ((abs(offsetX.value) - REVEAL_THRESHOLD_PX) /
                                            (CONFIRM_THRESHOLD_PX - REVEAL_THRESHOLD_PX))
                                        .coerceIn(0f, 1f)
                                )
                            )
                        }
                        Box(
                            modifier         = Modifier
                                .size(40.dp)
                                .scale(iconScale)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Message,
                                contentDescription = "Message",
                                tint               = Color.White,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── The actual row content, slides horizontally ───────────────────
        Box(
            modifier = Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            content()
        }
    }
}