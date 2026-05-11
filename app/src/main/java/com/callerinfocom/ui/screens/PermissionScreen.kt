package com.callerinfocom.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.delay
import com.callerinfocom.R
import com.callerinfocom.ui.components.BannerAd

private const val TAG = "PermissionScreen"

private data class PermissionInfo(
    val icon        : ImageVector,
    val title       : Int,
    val description : Int
)

private val requiredPermissions = arrayOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.POST_NOTIFICATIONS
)

/**
 * Computes which of [requiredPermissions] are NOT currently granted.
 * Used to forward denied perms to RequiredPermissionsDialog so they can be asked first.
 */
private fun computeDeniedPermissions(context: android.content.Context): List<String> =
    requiredPermissions.filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }

/**
 * Backwards-compatible entry point.
 * Existing callers using `(allGranted: Boolean) -> Unit` keep working unchanged.
 */
@Composable
fun PermissionScreen(
    onPermissionsResult: (allGranted: Boolean) -> Unit,
    onSkip: () -> Unit
) {
    PermissionScreen(
        onPermissionsResult = { allGranted, _ -> onPermissionsResult(allGranted) },
        onSkip              = onSkip
    )
}

/**
 * New entry point that ALSO reports which permissions were denied,
 * so the caller can forward them to RequiredPermissionsDialog as priorityPermissions.
 *
 * @param onPermissionsResult (allGranted, deniedPerms) — deniedPerms is empty when allGranted=true.
 */
@Composable
fun PermissionScreen(
    onPermissionsResult: (allGranted: Boolean, deniedPerms: List<String>) -> Unit,
    onSkip: () -> Unit
) {
    val headerAlpha  = remember { Animatable(0f) }
    val cardsVisible = remember { mutableStateOf(false) }
    val btnsAlpha    = remember { Animatable(0f) }
    val context      = LocalContext.current

    var dialogActuallyLaunched by remember { mutableStateOf(false) }
    var preLaunchDeniedPerms   by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "LaunchedEffect(Unit) — starting animations")
        headerAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        delay(100)
        cardsVisible.value = true
        delay(400)
        btnsAlpha.animateTo(1f, tween(300))
        Log.d(TAG, "LaunchedEffect(Unit) — animations complete")
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "──────────────────────────────────────────────")
        Log.d(TAG, "Launcher callback fired")
        Log.d(TAG, "  dialogActuallyLaunched = $dialogActuallyLaunched")
        Log.d(TAG, "  results.size           = ${results.size}")
        results.forEach { (perm, granted) ->
            Log.d(TAG, "  permission: $perm → granted=$granted")
        }

        if (!dialogActuallyLaunched) {
            Log.w(TAG, "WARN: callback fired but dialogActuallyLaunched=false → ignoring")
            return@rememberLauncherForActivityResult
        }

        dialogActuallyLaunched = false
        Log.d(TAG, "Flag reset → dialogActuallyLaunched=false")

        // Already granted before dialog even showed
        if (results.isEmpty()) {
            Log.d(TAG, "DECISION: results empty → already granted → navigating")
            onPermissionsResult(true, emptyList())
            return@rememberLauncherForActivityResult
        }

        val allGranted = results.values.all { it }
        if (allGranted) {
            Log.d(TAG, "DECISION: all granted → navigating")
            onPermissionsResult(true, emptyList())
            return@rememberLauncherForActivityResult
        }

        // Some permissions denied — distinguish back-dismiss vs real deny
        val activity = context as? android.app.Activity
        if (activity == null) {
            Log.w(TAG, "Context is not an Activity → assuming user interacted → navigating")
            val denied = results.filter { !it.value }.keys.toList()
            onPermissionsResult(false, denied)
            return@rememberLauncherForActivityResult
        }

        val deniedPerms = results.filter { !it.value }.keys
        Log.d(TAG, "  Denied permissions: $deniedPerms")

        val rationaleFlags = deniedPerms.associate { perm ->
            val rationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            Log.d(TAG, "  shouldShowRationale($perm) = $rationale")
            perm to rationale
        }

        val anyRationaleTrue = rationaleFlags.values.any { it }
        Log.d(TAG, "  anyRationaleTrue       = $anyRationaleTrue")
        Log.d(TAG, "  preLaunchDeniedPerms   = $preLaunchDeniedPerms")

        if (anyRationaleTrue) {
            Log.d(TAG, "DECISION: rationale=true → explicitly denied → navigating with allGranted=false")
            onPermissionsResult(false, deniedPerms.toList())
        } else {
            val wasDeniedBefore = deniedPerms.any { preLaunchDeniedPerms.contains(it) }
            Log.d(TAG, "  wasDeniedBefore        = $wasDeniedBefore")

            if (wasDeniedBefore) {
                Log.d(TAG, "DECISION: permanent deny → navigating")
                onPermissionsResult(false, deniedPerms.toList())
            } else {
                Log.d(TAG, "DECISION: back-dismissed on first ask → staying on screen")
                // Do NOT call onPermissionsResult — stay on screen
            }
        }

        Log.d(TAG, "──────────────────────────────────────────────")
    }

    fun launchPermissions() {
        val activity = context as? android.app.Activity
        preLaunchDeniedPerms = if (activity != null) {
            requiredPermissions.filter { perm ->
                val rationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                Log.d(TAG, "Pre-launch snapshot: $perm → shouldShowRationale=$rationale")
                rationale
            }.toSet()
        } else {
            emptySet()
        }

        Log.d(TAG, "launchPermissions() → preLaunchDeniedPerms=$preLaunchDeniedPerms")
        dialogActuallyLaunched = true
        Log.d(TAG, "launchPermissions() → dialogActuallyLaunched=true → launching dialog")
        launcher.launch(requiredPermissions)
    }

    BackHandler {
        Log.d(TAG, "BackHandler triggered → re-launching permission dialog")
        launchPermissions()
    }

    val permissionItems = listOf(
        PermissionInfo(
            icon        = Icons.Outlined.Analytics,
            title       = R.string.perm_four,
            description = R.string.perm_four_desc
        ),
        PermissionInfo(
            icon        = Icons.Outlined.Notifications,
            title       = R.string.perm_five,
            description = R.string.perm_five_desc
        )
    )

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .weight(1f)
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(Modifier.height(48.dp)) }

            item {
                Box(
                    modifier         = Modifier.alpha(headerAlpha.value),
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
                                painter            = painterResource(R.drawable.padlock),
                                contentDescription = "Lock Sticker",
                                modifier           = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(50.dp))
                        Text(
                            text       = stringResource(R.string.permissions_needed),
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onBackground,
                            textAlign  = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text      = stringResource(R.string.permissions_needed_desc),
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(36.dp)) }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    permissionItems.forEachIndexed { index, item ->
                        AnimatedVisibility(
                            visible = cardsVisible.value,
                            enter   = fadeIn(tween(300, delayMillis = index * 100)) +
                                    slideInVertically(tween(300, delayMillis = index * 100)) { it / 3 }
                        ) {
                            PermissionCard(item = item)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(btnsAlpha.value)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            Log.d(TAG, "\"Agree and Continue\" button clicked")
                            launchPermissions()
                        },
                        shape   = RoundedCornerShape(14.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Text(
                            text       = "Agree and Continue",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Spacer(Modifier.height(8.dp))

                    ConsentText(
                        onPrivacyClick = {
                            Log.d(TAG, "Privacy Policy tapped")
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://sites.google.com/view/mktechsolutiosrewa?usp=sharing"))
                            )
                        },
                        onTermsClick = {
                            Log.d(TAG, "Terms & Conditions tapped")
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://sites.google.com/view/mktechsolutionsrewa?usp=sharing"))
                            )
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        BannerAd(modifier = Modifier.navigationBarsPadding())
    }
}

// ── Single permission card ────────────────────────────────────────────────────

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

// ── Consent text with clickable links ────────────────────────────────────────

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
        val termsStart   = fullText.indexOf(termsText)
        val termsEnd     = termsStart + termsText.length

        if (privacyStart >= 0) {
            addStyle(SpanStyle(color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline), privacyStart, privacyEnd)
            addStringAnnotation("privacy", "privacy", privacyStart, privacyEnd)
        }
        if (termsStart >= 0) {
            addStyle(SpanStyle(color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline), termsStart, termsEnd)
            addStringAnnotation("terms", "terms", termsStart, termsEnd)
        }
    }

    ClickableText(
        text  = annotatedText,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = { offset ->
            annotatedText.getStringAnnotations(offset, offset).firstOrNull()?.let {
                when (it.tag) {
                    "privacy" -> onPrivacyClick()
                    "terms"   -> onTermsClick()
                }
            }
        }
    )
}