package com.contactsapptwomktech.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.contactsapptwomktech.R

private data class PermissionInfo(
    val icon        : ImageVector,
    val title       : Int,
    val description : Int
)

private val requiredPermissions = arrayOf(
//    Manifest.permission.READ_CONTACTS,
//    Manifest.permission.CALL_PHONE,
//    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.POST_NOTIFICATIONS
)

/**
 * Permission rationale screen. Shows what permissions are needed and why,
 * then launches the system permission dialog.
 *
 * @param onPermissionsResult Called after the user responds to the dialog.
 *                            [allGranted] is true if every permission was granted.
 * @param onSkip              Called if the user taps "Skip for now".
 */
@Composable
fun PermissionScreen(
    onPermissionsResult: (allGranted: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    // ── Animations ────────────────────────────────────────
    val headerAlpha = remember { Animatable(0f) }
    val cardsVisible = remember { mutableStateOf(false) }
    val btnsAlpha = remember { Animatable(0f) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        delay(100)
        cardsVisible.value = true
        delay(400)
        btnsAlpha.animateTo(1f, tween(300))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onPermissionsResult(results.values.all { it })
    }

    val permissionItems = listOf(
        PermissionInfo(
            icon = Icons.Outlined.Analytics,
            title = R.string.perm_four,
            description = R.string.perm_four_desc
        ),
        PermissionInfo(
            icon = Icons.Outlined.Notifications,
            title = R.string.perm_five,
            description = R.string.perm_five_desc
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // ── Header ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.alpha(headerAlpha.value),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.padlock),
                        contentDescription = "Lock Sticker",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.height(50.dp))
                Text(
                    text = stringResource(R.string.permissions_needed),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.permissions_needed_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        // ── Permission Cards ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)           // This pushes buttons to bottom
                .fillMaxWidth()
        ) {
            permissionItems.forEachIndexed { index, item ->
                AnimatedVisibility(
                    visible = cardsVisible.value,
                    enter = fadeIn(tween(300, delayMillis = index * 100)) +
                            slideInVertically(tween(300, delayMillis = index * 100)) { it / 3 }
                ) {
                    PermissionCard(item = item)
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // ── Action Buttons (Fixed at bottom) ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(btnsAlpha.value)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { launcher.launch(requiredPermissions) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "Agree and Continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Uncomment if you want Skip button later
            // OutlinedButton(...) { ... }

            Spacer(Modifier.height(8.dp))

            ConsentText(
                onPrivacyClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://sites.google.com/view/mktechsolutiosrewa?usp=sharing")
                    )
                    context.startActivity(intent)
                },
                onTermsClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://sites.google.com/view/mktechsolutionsrewa?usp=sharing")
                    )
                    context.startActivity(intent)
                }
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------------------
// Single permission card
// ---------------------------------------------------------------------------

@Composable
private fun PermissionCard(item: PermissionInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text       = stringResource(item.title),
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = stringResource(item.description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Consent text with clickable links
// ---------------------------------------------------------------------------

@Composable
fun ConsentText(
    onPrivacyClick: () -> Unit,
    onTermsClick  : () -> Unit
) {
    val fullText    = stringResource(R.string.permission_consent)
    val privacyText = "Privacy Policy"
    val termsText   = "Terms & Conditions"

    val annotatedText = buildAnnotatedString {
        append(fullText)

        val privacyStart = fullText.indexOf(privacyText)
        val privacyEnd   = privacyStart + privacyText.length

        val termsStart = fullText.indexOf(termsText)
        val termsEnd   = termsStart + termsText.length

        if (privacyStart >= 0) {
            addStyle(
                style = SpanStyle(
                    color          = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                start = privacyStart,
                end   = privacyEnd
            )
            addStringAnnotation(
                tag        = "privacy",
                annotation = "privacy",
                start      = privacyStart,
                end        = privacyEnd
            )
        }

        if (termsStart >= 0) {
            addStyle(
                style = SpanStyle(
                    color          = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                ),
                start = termsStart,
                end   = termsEnd
            )
            addStringAnnotation(
                tag        = "terms",
                annotation = "terms",
                start      = termsStart,
                end        = termsEnd
            )
        }
    }

    ClickableText(
        text  = annotatedText,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(offset, offset)
                .firstOrNull()?.let {
                    when (it.tag) {
                        "privacy" -> onPrivacyClick()
                        "terms"   -> onTermsClick()
                    }
                }
        }
    )
}