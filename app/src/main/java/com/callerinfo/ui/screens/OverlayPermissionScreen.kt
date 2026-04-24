package com.callerinfo.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callerinfo.R

@Composable
fun OverlayPermissionScreen(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var hasOverlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Top spacer ─────────────────────────────────────────────────────
        item { Spacer(Modifier.height(48.dp)) }

        // ── Icon ───────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(40.dp)
                )
            }
        }

        item { Spacer(Modifier.height(32.dp)) }

        // ── Title ──────────────────────────────────────────────────────────
        item {
            Text(
                text       = stringResource(R.string.overlay_permission_title),
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Description ────────────────────────────────────────────────────
        item {
            Text(
                text      = stringResource(R.string.overlay_permission_desc),
                style     = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { Spacer(Modifier.height(40.dp)) }

        // ── Benefits card ──────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text       = stringResource(R.string.why_do_we_need_this),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = stringResource(R.string.why_do_we_need_this_desc),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }

        // ── Action buttons ─────────────────────────────────────────────────
        item {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text  = stringResource(R.string.open_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                OutlinedButton(
                    onClick  = onSkip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(R.string.skip_now))
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}