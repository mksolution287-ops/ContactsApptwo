//package com.callerinfocom.ui.screens
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Build
//import android.provider.Settings
//import android.util.Log
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Call
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material.icons.filled.Layers
//import androidx.compose.ui.res.stringResource
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleEventObserver
//import com.callerinfocom.R
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//
//private const val TAG = "OverlayPermission"
//
//data class PermissionItem(
//    val icon: ImageVector,
//    val label: String,
//    val isGranted: Boolean = false
//)
//
///**
// * Permissions we actually request AND that the user can grant manually.
// * ANSWER_PHONE_CALLS is intentionally excluded — it is a privileged permission
// * that is never grantable via the Settings UI and always appears denied even
// * after the user grants everything visible, causing a false "still missing" loop.
// */
//private fun getRequestablPermissions(): List<String> = buildList {
//    add(Manifest.permission.READ_CONTACTS)
//    add(Manifest.permission.READ_CALL_LOG)
//    add(Manifest.permission.WRITE_CONTACTS)
//    add(Manifest.permission.POST_NOTIFICATIONS)
//    add(Manifest.permission.READ_PHONE_STATE)
//    add(Manifest.permission.CALL_PHONE)
//    // ANSWER_PHONE_CALLS deliberately omitted — privileged, not user-grantable
//}
//
//private fun getMissingRuntimePermissions(context: android.content.Context): List<String> =
//    getRequestablPermissions().filter {
//        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
//    }
//
//private fun isOverlayGranted(context: android.content.Context): Boolean =
//    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
//
///**
// * Dialog should stay open only when runtime permissions are missing.
// * Overlay-only denial is NOT a blocker — we still try to get it, but
// * we don't hold the dialog open just because it's missing.
// */
//private fun shouldKeepDialogOpen(context: android.content.Context): Boolean =
//    getMissingRuntimePermissions(context).isNotEmpty()
//
//@Composable
//fun RequiredPermissionsDialog(onDismiss: () -> Unit) {
//    val context        = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    // Dismiss immediately on first composition if nothing is actually blocking
//    if (!shouldKeepDialogOpen(context)) {
//        Log.d(TAG, "=== First composition: nothing blocking → immediate dismiss ===")
//        LaunchedEffect(Unit) { onDismiss() }
//        return
//    }
//
//    var overlaySettingsOpened  by remember { mutableStateOf(false) }
//    var pollingOverlay         by remember { mutableStateOf(false) }
//    var showFallbackDialog     by remember { mutableStateOf(false) }
//    var waitingForAppSettings  by remember { mutableStateOf(false) }
//
//    var runtimeGranted by remember { mutableStateOf(!shouldKeepDialogOpen(context)) }
//    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }
//
//    Log.d(TAG, "=== Composition ===  runtimeGranted=$runtimeGranted  " +
//            "overlayGranted=$overlayGranted  showFallback=$showFallbackDialog  " +
//            "waitingForAppSettings=$waitingForAppSettings  pollingOverlay=$pollingOverlay  " +
//            "missingRuntime=${getMissingRuntimePermissions(context)}")
//
//    // ── Runtime permission launcher ───────────────────────────────────────
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { results ->
//        Log.d(TAG, "── Runtime launcher callback ──")
//        Log.d(TAG, "  raw results        : $results")
//
//        val nowMissingRuntime = getMissingRuntimePermissions(context)
//        val nowOverlay        = isOverlayGranted(context)
//        runtimeGranted = nowMissingRuntime.isEmpty()
//        overlayGranted = nowOverlay
//
//        Log.d(TAG, "  missingRuntime     : $nowMissingRuntime")
//        Log.d(TAG, "  runtimeGranted     : $runtimeGranted")
//        Log.d(TAG, "  overlayGranted     : $overlayGranted")
//
//        when {
//            // All runtime granted + overlay granted → done
//            runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  → ALL granted, dismissing ✓")
//                onDismiss()
//            }
//            // All runtime granted, overlay still needed → open overlay settings
//            runtimeGranted && !overlayGranted -> {
//                Log.d(TAG, "  → runtime OK, overlay missing → opening overlay settings")
//                overlaySettingsOpened = true
//            }
//            // Runtime denied, overlay already fine → skip overlay, go to fallback
//            !runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  → runtime denied, overlay OK → fallback dialog")
//                showFallbackDialog = true
//            }
//            // Both missing → try overlay first, fallback will handle remaining runtime
//            else -> {
//                Log.d(TAG, "  → runtime denied + overlay missing → overlay settings first")
//                overlaySettingsOpened = true
//            }
//        }
//    }
//
//    // ── Open overlay settings ─────────────────────────────────────────────
//    LaunchedEffect(overlaySettingsOpened) {
//        Log.d(TAG, "LaunchedEffect(overlaySettingsOpened=$overlaySettingsOpened)")
//        if (!overlaySettingsOpened) return@LaunchedEffect
//        val intent = Intent(
//            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//            Uri.parse("package:${context.packageName}")
//        )
//        Log.d(TAG, "  startActivity → overlay settings")
//        context.startActivity(intent)
//        overlaySettingsOpened = false
//        pollingOverlay        = true
//        Log.d(TAG, "  pollingOverlay=true")
//    }
//
//    // ── Poll for overlay grant ────────────────────────────────────────────
//    LaunchedEffect(pollingOverlay) {
//        Log.d(TAG, "LaunchedEffect(pollingOverlay=$pollingOverlay)")
//        if (!pollingOverlay) return@LaunchedEffect
//
//        var tick = 0
//        while (isActive) {
//            delay(500L)
//            tick++
//            val granted = isOverlayGranted(context)
//            Log.d(TAG, "  poll tick #$tick → canDrawOverlays=$granted")
//
//            if (granted) {
//                overlayGranted = true
//                pollingOverlay = false
//                Log.d(TAG, "  overlay GRANTED on tick #$tick — bringing app forward")
//
//                context.packageManager
//                    .getLaunchIntentForPackage(context.packageName)
//                    ?.apply {
//                        addFlags(
//                            Intent.FLAG_ACTIVITY_NEW_TASK or
//                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
//                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
//                        )
//                    }?.let { context.startActivity(it) }
//
//                val nowMissing = getMissingRuntimePermissions(context)
//                runtimeGranted = nowMissing.isEmpty()
//                Log.d(TAG, "  after overlay grant: runtimeGranted=$runtimeGranted  " +
//                        "missingRuntime=$nowMissing")
//
//                // Overlay was a bonus — only keep dialog open if runtime is still missing
//                if (runtimeGranted) {
//                    Log.d(TAG, "  → ALL granted, dismissing ✓")
//                    onDismiss()
//                } else {
//                    Log.d(TAG, "  → runtime still missing → fallback dialog")
//                    showFallbackDialog = true
//                }
//                break
//            }
//        }
//        Log.d(TAG, "poll loop ENDED (isActive=$isActive)")
//    }
//
//    // ── Lifecycle observer ────────────────────────────────────────────────
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            Log.d(TAG, "── Lifecycle event=$event ──  pollingOverlay=$pollingOverlay  " +
//                    "waitingForAppSettings=$waitingForAppSettings")
//
//            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
//
//            // Path 1: returned from overlay settings (poll handles granted case;
//            //         we only act here if user came back without granting)
//            if (pollingOverlay) {
//                val stillDenied = !isOverlayGranted(context)
//                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
//                if (stillDenied) {
//                    pollingOverlay = false
//                    overlayGranted = false
//                    val nowMissing = getMissingRuntimePermissions(context)
//                    runtimeGranted = nowMissing.isEmpty()
//                    Log.d(TAG, "  overlay denied on return  runtimeGranted=$runtimeGranted  " +
//                            "missingRuntime=$nowMissing")
//                    // Only keep dialog open if runtime is still missing
//                    if (runtimeGranted) {
//                        Log.d(TAG, "  → runtime OK, overlay was optional → dismissing ✓")
//                        onDismiss()
//                    } else {
//                        Log.d(TAG, "  → runtime still missing → fallback dialog")
//                        showFallbackDialog = true
//                    }
//                }
//                return@LifecycleEventObserver
//            }
//
//            // Path 2: returned from app details settings
//            if (waitingForAppSettings) {
//                val nowMissing = getMissingRuntimePermissions(context)
//                val nowOverlay = isOverlayGranted(context)
//                runtimeGranted = nowMissing.isEmpty()
//                overlayGranted = nowOverlay
//                waitingForAppSettings = false
//
//                Log.d(TAG, "  [app-settings path] runtimeGranted=$runtimeGranted  " +
//                        "overlayGranted=$overlayGranted  missingRuntime=$nowMissing")
//
//                // Dismiss as long as runtime is cleared (overlay denial is non-blocking)
//                if (runtimeGranted) {
//                    Log.d(TAG, "  → runtime granted (overlay=$overlayGranted) → dismissing ✓")
//                    onDismiss()
//                } else {
//                    Log.d(TAG, "  → runtime still missing after app settings → keep fallback")
//                }
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//        onDispose {
//            Log.d(TAG, "DisposableEffect disposed → removing lifecycle observer")
//            lifecycleOwner.lifecycle.removeObserver(observer)
//        }
//    }
//
//    // ── UI ────────────────────────────────────────────────────────────────
//    if (showFallbackDialog) {
//        PermissionsDialogContent(
//            runtimeGranted = runtimeGranted,
//            overlayGranted = overlayGranted,
//            onContinue = {
//                Log.d(TAG, "Fallback Continue tapped → app details settings  " +
//                        "missingRuntime=${getMissingRuntimePermissions(context)}  " +
//                        "overlayGranted=${isOverlayGranted(context)}")
//                waitingForAppSettings = true
//                context.startActivity(
//                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                        data = Uri.parse("package:${context.packageName}")
//                    }
//                )
//            },
//            onDismiss = onDismiss
//        )
//    } else {
//        PermissionsDialogContent(
//            runtimeGranted = runtimeGranted,
//            overlayGranted = overlayGranted,
//            onContinue = {
//                Log.d(TAG, "Initial Continue tapped → requesting runtime permissions  " +
//                        "perms=${getRequestablPermissions()}")
//                permissionLauncher.launch(getRequestablPermissions().toTypedArray())
//            },
//            onDismiss = onDismiss
//        )
//    }
//}
//
//@Composable
//private fun PermissionsDialogContent(
//    runtimeGranted: Boolean,
//    overlayGranted: Boolean,
//    onContinue: () -> Unit,
//    onDismiss: () -> Unit
//) {
//    val permissionItems = listOf(
//        PermissionItem(Icons.Filled.Call,   "Call",     isGranted = runtimeGranted),
//        PermissionItem(Icons.Filled.Person, "Contacts", isGranted = runtimeGranted),
//        PermissionItem(Icons.Filled.Layers, "Overlay",  isGranted = overlayGranted)
//    )
//
//    Dialog(
//        onDismissRequest = { onDismiss() },
//        properties = DialogProperties(
//            dismissOnBackPress    = false,
//            dismissOnClickOutside = false
//        )
//    ) {
//        Card(
//            shape  = RoundedCornerShape(20.dp),
//            colors = CardDefaults.cardColors(
//                containerColor = MaterialTheme.colorScheme.surfaceVariant
//            ),
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 8.dp)
//        ) {
//            Column(modifier = Modifier.padding(24.dp)) {
//
//                Text(
//                    text       = stringResource(R.string.permission_required),
//                    fontWeight = FontWeight.Bold,
//                    fontSize   = 20.sp,
//                    color      = MaterialTheme.colorScheme.onSurface
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                Text(
//                    text  = stringResource(R.string.permission_subtitle),
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                permissionItems.forEach { item ->
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier          = Modifier.padding(vertical = 8.dp)
//                    ) {
//                        Box(
//                            contentAlignment = Alignment.Center,
//                            modifier         = Modifier
//                                .size(40.dp)
//                                .background(
//                                    color = if (item.isGranted) Color(0xFF2E7D32)
//                                    else MaterialTheme.colorScheme.onSurface,
//                                    shape = CircleShape
//                                )
//                        ) {
//                            Icon(
//                                imageVector        = if (item.isGranted) Icons.Filled.Check else item.icon,
//                                contentDescription = item.label,
//                                tint               = MaterialTheme.colorScheme.surface,
//                                modifier           = Modifier.size(20.dp)
//                            )
//                        }
//                        Spacer(modifier = Modifier.width(16.dp))
//                        Column {
//                            Text(
//                                text       = item.label,
//                                fontWeight = FontWeight.SemiBold,
//                                fontSize   = 16.sp,
//                                color      = MaterialTheme.colorScheme.onSurface
//                            )
//                            if (item.isGranted) {
//                                Text(
//                                    text  = "Granted",
//                                    style = MaterialTheme.typography.labelSmall,
//                                    color = Color(0xFF2E7D32)
//                                )
//                            }
//                        }
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Row(
//                    verticalAlignment = Alignment.CenterVertically,
//                    modifier          = Modifier
//                        .fillMaxWidth()
//                        .background(
//                            color = Color(0xFF1B5E20).copy(alpha = 0.85f),
//                            shape = RoundedCornerShape(12.dp)
//                        )
//                        .padding(horizontal = 16.dp, vertical = 14.dp)
//                ) {
//                    Icon(
//                        imageVector        = Icons.Filled.Person,
//                        contentDescription = null,
//                        tint               = Color.White,
//                        modifier           = Modifier.size(20.dp)
//                    )
//                    Spacer(modifier = Modifier.width(10.dp))
//                    Text(
//                        text  = stringResource(R.string.permission_subtitle_desc),
//                        color = Color.White,
//                        style = MaterialTheme.typography.bodySmall
//                    )
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                Row(
//                    modifier              = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    TextButton(onClick = onContinue) {
//                        Text(
//                            text       = stringResource(R.string.continue_btn),
//                            color      = MaterialTheme.colorScheme.primary,
//                            fontWeight = FontWeight.Bold,
//                            fontSize   = 16.sp
//                        )
//                    }
//                }
//            }
//        }
//    }
//}

package com.callerinfocom.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Layers
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.callerinfocom.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "OverlayPermission"

data class PermissionItem(
    val icon: ImageVector,
    val label: String,
    val isGranted: Boolean = false
)

/**
 * Permissions we actually request AND that the user can grant manually.
 * ANSWER_PHONE_CALLS is intentionally excluded — it is a privileged permission
 * that is never grantable via the Settings UI and always appears denied even
 * after the user grants everything visible, causing a false "still missing" loop.
 */
private fun getRequestablPermissions(): List<String> = buildList {
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.READ_CALL_LOG)
    add(Manifest.permission.WRITE_CONTACTS)
    add(Manifest.permission.POST_NOTIFICATIONS)
    add(Manifest.permission.READ_PHONE_STATE)
    add(Manifest.permission.CALL_PHONE)
    // ANSWER_PHONE_CALLS deliberately omitted — privileged, not user-grantable
}

private fun getMissingRuntimePermissions(context: android.content.Context): List<String> =
    getRequestablPermissions().filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }

private fun isOverlayGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

/**
 * ALL permissions are satisfied — runtime granted AND overlay granted.
 * Only when both are true can the dialog safely dismiss on first composition.
 */
private fun allPermissionsGranted(context: android.content.Context): Boolean =
    getMissingRuntimePermissions(context).isEmpty() && isOverlayGranted(context)

@Composable
fun RequiredPermissionsDialog(onDismiss: () -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── CRITICAL FIX ──────────────────────────────────────────────────────
    // Only dismiss immediately if BOTH runtime AND overlay are already granted.
    // Previously this only checked runtime, so after runtime was granted the
    // dialog would recompose, hit this guard, and dismiss before the overlay
    // settings LaunchedEffect had a chance to fire.
    if (allPermissionsGranted(context)) {
        Log.d(TAG, "=== First composition: all permissions granted → immediate dismiss ===")
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var overlaySettingsOpened by remember { mutableStateOf(false) }
    var pollingOverlay        by remember { mutableStateOf(false) }
    var showFallbackDialog    by remember { mutableStateOf(false) }
    var waitingForAppSettings by remember { mutableStateOf(false) }

    var runtimeGranted by remember { mutableStateOf(getMissingRuntimePermissions(context).isEmpty()) }
    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }

    Log.d(TAG, "=== Composition ===  runtimeGranted=$runtimeGranted  " +
            "overlayGranted=$overlayGranted  showFallback=$showFallbackDialog  " +
            "waitingForAppSettings=$waitingForAppSettings  pollingOverlay=$pollingOverlay  " +
            "missingRuntime=${getMissingRuntimePermissions(context)}")

    // ── Runtime permission launcher ───────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "── Runtime launcher callback ──")
        Log.d(TAG, "  raw results        : $results")

        val nowMissingRuntime = getMissingRuntimePermissions(context)
        val nowOverlay        = isOverlayGranted(context)
        runtimeGranted = nowMissingRuntime.isEmpty()
        overlayGranted = nowOverlay

        Log.d(TAG, "  missingRuntime     : $nowMissingRuntime")
        Log.d(TAG, "  runtimeGranted     : $runtimeGranted")
        Log.d(TAG, "  overlayGranted     : $overlayGranted")

        when {
            // All runtime granted + overlay granted → done
            runtimeGranted && overlayGranted -> {
                Log.d(TAG, "  → ALL granted, dismissing ✓")
                onDismiss()
            }
            // All runtime granted, overlay still needed → open overlay settings
            runtimeGranted && !overlayGranted -> {
                Log.d(TAG, "  → runtime OK, overlay missing → opening overlay settings")
                overlaySettingsOpened = true
            }
            // Runtime denied, overlay already fine → skip overlay, go to fallback
            !runtimeGranted && overlayGranted -> {
                Log.d(TAG, "  → runtime denied, overlay OK → fallback dialog")
                showFallbackDialog = true
            }
            // Both missing → try overlay first, fallback will handle remaining runtime
            else -> {
                Log.d(TAG, "  → runtime denied + overlay missing → overlay settings first")
                overlaySettingsOpened = true
            }
        }
    }

    // ── Open overlay settings ─────────────────────────────────────────────
    LaunchedEffect(overlaySettingsOpened) {
        Log.d(TAG, "LaunchedEffect(overlaySettingsOpened=$overlaySettingsOpened)")
        if (!overlaySettingsOpened) return@LaunchedEffect
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        Log.d(TAG, "  startActivity → overlay settings")
        context.startActivity(intent)
        overlaySettingsOpened = false
        pollingOverlay        = true
        Log.d(TAG, "  pollingOverlay=true")
    }

    // ── Poll for overlay grant ────────────────────────────────────────────
    LaunchedEffect(pollingOverlay) {
        Log.d(TAG, "LaunchedEffect(pollingOverlay=$pollingOverlay)")
        if (!pollingOverlay) return@LaunchedEffect

        var tick = 0
        while (isActive) {
            delay(500L)
            tick++
            val granted = isOverlayGranted(context)
            Log.d(TAG, "  poll tick #$tick → canDrawOverlays=$granted")

            if (granted) {
                overlayGranted = true
                pollingOverlay = false
                Log.d(TAG, "  overlay GRANTED on tick #$tick — bringing app forward")

                context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }?.let { context.startActivity(it) }

                val nowMissing = getMissingRuntimePermissions(context)
                runtimeGranted = nowMissing.isEmpty()
                Log.d(TAG, "  after overlay grant: runtimeGranted=$runtimeGranted  " +
                        "missingRuntime=$nowMissing")

                if (runtimeGranted) {
                    Log.d(TAG, "  → ALL granted, dismissing ✓")
                    onDismiss()
                } else {
                    Log.d(TAG, "  → runtime still missing → fallback dialog")
                    showFallbackDialog = true
                }
                break
            }
        }
        Log.d(TAG, "poll loop ENDED (isActive=$isActive)")
    }

    // ── Lifecycle observer ────────────────────────────────────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d(TAG, "── Lifecycle event=$event ──  pollingOverlay=$pollingOverlay  " +
                    "waitingForAppSettings=$waitingForAppSettings")

            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

            // Path 1: returned from overlay settings (poll handles granted case;
            //         we only act here if user came back without granting)
            if (pollingOverlay) {
                val stillDenied = !isOverlayGranted(context)
                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
                if (stillDenied) {
                    pollingOverlay = false
                    overlayGranted = false
                    val nowMissing = getMissingRuntimePermissions(context)
                    runtimeGranted = nowMissing.isEmpty()
                    Log.d(TAG, "  overlay denied on return  runtimeGranted=$runtimeGranted  " +
                            "missingRuntime=$nowMissing")
                    // Runtime was already granted — overlay is optional, so dismiss
                    if (runtimeGranted) {
                        Log.d(TAG, "  → runtime OK, overlay was optional → dismissing ✓")
                        onDismiss()
                    } else {
                        Log.d(TAG, "  → runtime still missing → fallback dialog")
                        showFallbackDialog = true
                    }
                }
                return@LifecycleEventObserver
            }

            // Path 2: returned from app details settings
            if (waitingForAppSettings) {
                val nowMissing = getMissingRuntimePermissions(context)
                val nowOverlay = isOverlayGranted(context)
                runtimeGranted = nowMissing.isEmpty()
                overlayGranted = nowOverlay
                waitingForAppSettings = false

                Log.d(TAG, "  [app-settings path] runtimeGranted=$runtimeGranted  " +
                        "overlayGranted=$overlayGranted  missingRuntime=$nowMissing")

                // Dismiss as long as runtime is cleared (overlay denial is non-blocking)
                if (runtimeGranted) {
                    Log.d(TAG, "  → runtime granted (overlay=$overlayGranted) → dismissing ✓")
                    onDismiss()
                } else {
                    Log.d(TAG, "  → runtime still missing after app settings → keep fallback")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "DisposableEffect disposed → removing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    if (showFallbackDialog) {
        PermissionsDialogContent(
            runtimeGranted = runtimeGranted,
            overlayGranted = overlayGranted,
            onContinue = {
                Log.d(TAG, "Fallback Continue tapped → app details settings  " +
                        "missingRuntime=${getMissingRuntimePermissions(context)}  " +
                        "overlayGranted=${isOverlayGranted(context)}")
                waitingForAppSettings = true
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            },
            onDismiss = onDismiss
        )
    } else {
        PermissionsDialogContent(
            runtimeGranted = runtimeGranted,
            overlayGranted = overlayGranted,
            onContinue = {
                Log.d(TAG, "Initial Continue tapped → requesting runtime permissions  " +
                        "perms=${getRequestablPermissions()}")
                permissionLauncher.launch(getRequestablPermissions().toTypedArray())
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PermissionsDialogContent(
    runtimeGranted: Boolean,
    overlayGranted: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val permissionItems = listOf(
        PermissionItem(Icons.Filled.Call,   "Call",     isGranted = runtimeGranted),
        PermissionItem(Icons.Filled.Person, "Contacts", isGranted = runtimeGranted),
        PermissionItem(Icons.Filled.Layers, "Overlay",  isGranted = overlayGranted)
    )

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress    = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Text(
                    text       = stringResource(R.string.permission_required),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text  = stringResource(R.string.permission_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                permissionItems.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(40.dp)
                                .background(
                                    color = if (item.isGranted) Color(0xFF2E7D32)
                                    else MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector        = if (item.isGranted) Icons.Filled.Check else item.icon,
                                contentDescription = item.label,
                                tint               = MaterialTheme.colorScheme.surface,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text       = item.label,
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            if (item.isGranted) {
                                Text(
                                    text  = "Granted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF1B5E20).copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Person,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text  = stringResource(R.string.permission_subtitle_desc),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onContinue) {
                        Text(
                            text       = stringResource(R.string.continue_btn),
                            color      = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp
                        )
                    }
                }
            }
        }
    }
}