package com.callerinfocom.ui.uninstall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar: back arrow + home ────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
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

            Spacer(Modifier.height(40.dp))

            // ── Header ────────────────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big "???" — using emoji glyphs as a stand-in for the 3D
                // red question marks in the mockup. Replace with an
                // Image(painterResource(R.drawable.ic_question_3d), ...) if
                // you have the asset.
                Text(
                    text     = "❓❓❓",
                    fontSize = 64.sp
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text       = "We're truly sorry!",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text     = "Anything you're unhappy about?",
                    fontSize = 15.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(36.dp))

            // ── Reason cards ──────────────────────────────────────────
            ReasonCard(
                text    = "The app may take up too much space or slow down the device.",
                onTry   = onTryStorage
            )
            Spacer(Modifier.height(12.dp))
            ReasonCard(
                text    = "Contacts were not updating automatically",
                onTry   = onTryContacts
            )
            Spacer(Modifier.height(12.dp))
            ReasonCard(
                text    = "UI is confusing or hard to use",
                onTry   = onTryUi
            )

            Spacer(Modifier.weight(1f))

            // ── Bottom CTAs ───────────────────────────────────────────
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
                    .height(54.dp)
            ) {
                Text(
                    text       = "Don't uninstall yet",
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick  = onContinueUninstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text           = "Still want to uninstall",
                    fontSize       = 14.sp,
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
        shape    = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text      = text,
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurface,
                modifier  = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onTry,
                shape   = RoundedCornerShape(20.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor   = Color.White
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 24.dp,
                    vertical   = 6.dp
                )
            ) {
                Text(
                    text       = "Try",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign  = TextAlign.Center
                )
            }
        }
    }
}