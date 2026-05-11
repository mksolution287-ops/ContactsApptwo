package com.callerinfocom.ui.uninstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callerinfocom.ui.components.AppInstallNativeAdCard
import com.callerinfocom.R

private val AccentGreen = Color(0xFF22C55E)

@Composable
fun UninstallRetentionScreen(
    onBack              : () -> Unit,
    onHome              : () -> Unit,
    onTryStorage        : () -> Unit,
    onTryContacts       : () -> Unit,
    onTryUi             : () -> Unit,
    onKeepApp           : () -> Unit,
    onContinueUninstall : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── Top bar ───────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onHome) {
                Icon(
                    imageVector        = Icons.Default.Home,
                    contentDescription = "Home",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // ── Scrollable content ────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
                Spacer(Modifier.height(16.dp))
            }

            item {
                // ── Header ────────────────────────────────────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text     = "❓❓❓",
                        fontSize = 48.sp           // was 64
                    )
                    Spacer(Modifier.height(10.dp)) // was 20
                    Text(
                        text       = stringResource(R.string.uninstall_retention_title),
                        fontSize   = 20.sp,        // was 22
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))  // was 6
                    Text(
                        text     = stringResource(R.string.uninstall_retention_subtitle),
                        fontSize = 13.sp,          // was 15
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))     // was 36
            }

            item {
                // ── Reason cards ──────────────────────────────────
                ReasonCard(
                    text  = stringResource(R.string.uninstall_retention_reason_storage),
                    onTry = onTryStorage
                )
                Spacer(Modifier.height(8.dp))      // was 12
                ReasonCard(
                    text  = stringResource(R.string.uninstall_retention_reason_contacts),
                    onTry = onTryContacts
                )
                Spacer(Modifier.height(8.dp))
                ReasonCard(
                    text  = stringResource(R.string.uninstall_retention_reason_ui),
                    onTry = onTryUi
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Bottom section (pinned) ───────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {

            // Ad above the primary CTA
            AppInstallNativeAdCard()

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = onKeepApp,
                shape    = RoundedCornerShape(28.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor   = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)             // was 54
            ) {
                Text(
                    text       = stringResource(R.string.uninstall_retention_keep),
                    fontSize   = 16.sp,        // was 17
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(6.dp))      // was 8

            TextButton(
                onClick  = onContinueUninstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)    // was 12
            ) {
                Text(
                    text           = stringResource(R.string.uninstall_retention_proceed),
                    fontSize       = 13.sp,    // was 14
                    color          = MaterialTheme.colorScheme.onBackground,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun ReasonCard(
    text  : String,
    onTry : () -> Unit
) {
    Surface(
        color    = MaterialTheme.colorScheme.surfaceVariant,
        shape    = RoundedCornerShape(12.dp),  // was 14
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), // was 16/14
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text     = text,
                fontSize = 13.sp,              // was 14
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))      // was 12
            Button(
                onClick = onTry,
                shape   = RoundedCornerShape(20.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor   = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 20.dp,        // was 24
                    vertical   = 4.dp          // was 6
                )
            ) {
                Text(
                    text       = stringResource(R.string.uninstall_retention_try),
                    fontSize   = 13.sp,        // was 14
                    fontWeight = FontWeight.SemiBold,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}