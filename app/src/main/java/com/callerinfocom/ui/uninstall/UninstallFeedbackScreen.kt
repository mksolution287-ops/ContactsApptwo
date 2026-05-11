package com.callerinfocom.ui.uninstall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callerinfocom.ui.components.AppInstallNativeAdCard
import com.callerinfocom.R

private val AccentGreen   = Color(0xFF22C55E)
private val DisabledGreen = Color(0xFF14532D)

private data class FeedbackReason(val key: String, val label: Int)

private val FeedbackReasons = listOf(
    FeedbackReason("contact_feature_unhappy", R.string.uninstall_feedback_reason_1),
    FeedbackReason("loading_too_long",        R.string.uninstall_feedback_reason_2),
    FeedbackReason("not_feature_rich",        R.string.uninstall_feedback_reason_3),
    FeedbackReason("ui_complicated",          R.string.uninstall_feedback_reason_4),
    FeedbackReason("too_many_ads",            R.string.uninstall_feedback_reason_5),
    FeedbackReason("better_alternative",      R.string.uninstall_feedback_reason_6)
)

@Composable
fun UninstallFeedbackScreen(
    onBack   : () -> Unit,
    onCancel : () -> Unit,
    onConfirm: (reasonKey: String) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── Scrollable content ────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                // ── Top bar ───────────────────────────────────────
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp), // was 8
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text       = stringResource(R.string.uninstall_feedback_title),
                        fontSize   = 16.sp,        // was 18
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }  // was 8

            item {
                // ── Reason list ───────────────────────────────────
                FeedbackReasons.forEach { reason ->
                    ReasonRow(
                        label      = stringResource(reason.label),
                        selected   = selected == reason.key,
                        onSelected = { selected = reason.key }
                    )
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }
            }
        }

        // ── Pinned bottom section ─────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            // Hint banner
            Surface(
                color    = MaterialTheme.colorScheme.surfaceVariant,
                shape    = RoundedCornerShape(20.dp),              // was 28
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp) // was 8
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), // was 16/14
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Outlined.Info,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier           = Modifier.size(16.dp)  // was 20
                    )
                    Spacer(Modifier.width(10.dp))                   // was 12
                    Text(
                        text     = stringResource(R.string.uninstall_feedback_select_required),
                        fontSize = 12.sp,                           // was 13
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ad above buttons
            AppInstallNativeAdCard()

            // Cancel / Uninstall buttons
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp), // was 12
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick  = onCancel,
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor   = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)                              // was 50
                ) {
                    Text(
                        text       = stringResource(R.string.uninstall_feedback_cancel),
                        fontSize   = 15.sp,                         // was 16
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick  = { selected?.let(onConfirm) },
                    enabled  = selected != null,
                    shape    = RoundedCornerShape(28.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = AccentGreen,
                        contentColor           = Color.White,
                        disabledContainerColor = DisabledGreen,
                        disabledContentColor   = Color.White.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)                              // was 50
                ) {
                    Text(
                        text       = stringResource(R.string.uninstall_feedback_uninstall),
                        fontSize   = 15.sp,                         // was 16
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonRow(
    label      : String,
    selected   : Boolean,
    onSelected : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(horizontal = 20.dp, vertical = 12.dp),        // was 18
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            fontSize = 14.sp,                                       // was 16
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        RadioButton(
            selected = selected,
            onClick  = onSelected,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = AccentGreen,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}