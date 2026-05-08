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
//private fun getRequestablPermissions(): List<String> = buildList {
//    add(Manifest.permission.READ_CONTACTS)
//    add(Manifest.permission.READ_CALL_LOG)
//    add(Manifest.permission.WRITE_CONTACTS)
//    add(Manifest.permission.POST_NOTIFICATIONS)
//    add(Manifest.permission.READ_PHONE_STATE)
//    add(Manifest.permission.CALL_PHONE)
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
//private fun allPermissionsGranted(context: android.content.Context): Boolean =
//    getMissingRuntimePermissions(context).isEmpty() && isOverlayGranted(context)
//
//// Convenience: suppress app-open ad in one line
//private fun suppressAppOpenAd(context: android.content.Context) {
//    context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
//        .edit().putBoolean("skip_app_open_ad", true).apply()
//}
//
//@Composable
//fun RequiredPermissionsDialog(onDismiss: () -> Unit) {
//    val context        = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    if (allPermissionsGranted(context)) {
//        Log.d(TAG, "=== First composition: all permissions granted → immediate dismiss ===")
//        LaunchedEffect(Unit) { onDismiss() }
//        return
//    }
//
//    var overlaySettingsOpened by remember { mutableStateOf(false) }
//    var pollingOverlay        by remember { mutableStateOf(false) }
//    var showFallbackDialog    by remember { mutableStateOf(false) }
//    var waitingForAppSettings by remember { mutableStateOf(false) }
//
//    var runtimeGranted by remember { mutableStateOf(getMissingRuntimePermissions(context).isEmpty()) }
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
//            // All runtime + overlay granted → done, no ad suppression needed
//            runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  → ALL granted, dismissing ✓")
//                onDismiss()
//            }
//            // Runtime granted, overlay still needed → suppress app-open ad now
//            // because regardless of what the user does on the overlay screen,
//            // runtime is already fully granted and we don't want the ad to fire.
//            runtimeGranted && !overlayGranted -> {
//                Log.d(TAG, "  → runtime OK, overlay missing → suppressing app-open ad, opening overlay settings")
//                suppressAppOpenAd(context)
//                overlaySettingsOpened = true
//            }
//            // Runtime denied, overlay already fine → fallback dialog
//            !runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  → runtime denied, overlay OK → fallback dialog")
//                showFallbackDialog = true
//            }
//            // Both missing → open overlay settings first
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
//                if (runtimeGranted) {
//                    // Runtime was already granted before overlay screen — suppress app-open ad
//                    Log.d(TAG, "  → ALL granted, suppressing app-open ad and dismissing ✓")
//                    suppressAppOpenAd(context)
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
//            // Path 1: returned from overlay settings without granting
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
//                    if (runtimeGranted) {
//                        // Runtime fully granted, overlay denied/skipped →
//                        // suppress app-open ad and dismiss
//                        Log.d(TAG, "  → runtime OK, overlay skipped → suppressing app-open ad and dismissing ✓")
//                        suppressAppOpenAd(context)
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
//                if (runtimeGranted) {
//                    // Runtime granted from app settings — suppress app-open ad and dismiss
//                    Log.d(TAG, "  → runtime granted (overlay=$overlayGranted) → suppressing app-open ad and dismissing ✓")
//                    suppressAppOpenAd(context)
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
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
 * Base permissions the dialog always requests.
 * ANSWER_PHONE_CALLS deliberately omitted — privileged, not user-grantable.
 */
private fun getBasePermissions(): List<String> = listOf(
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.POST_NOTIFICATIONS,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.CALL_PHONE
)

/**
 * Full required set (priority + base, deduped). Used for "are we done?" checks.
 */
private fun getAllRequiredPermissions(priorityPermissions: List<String>): List<String> {
    val result = LinkedHashSet<String>()
    priorityPermissions.forEach { result.add(it) }
    getBasePermissions().forEach { result.add(it) }
    return result.toList()
}

/** Base perms minus anything already in the priority list. */
private fun getBaseMinusPriority(priorityPermissions: List<String>): List<String> =
    getBasePermissions().filter { it !in priorityPermissions }

private fun getMissingRuntimePermissions(
    context: android.content.Context,
    priorityPermissions: List<String>
): List<String> =
    getAllRequiredPermissions(priorityPermissions).filter {
        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
    }

private fun isOverlayGranted(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

private fun allPermissionsGranted(
    context: android.content.Context,
    priorityPermissions: List<String>
): Boolean =
    getMissingRuntimePermissions(context, priorityPermissions).isEmpty() && isOverlayGranted(context)

private fun suppressAppOpenAd(context: android.content.Context) {
    context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
        .edit().putBoolean("skip_app_open_ad", true).apply()
}

private fun permissionToCardInfo(permission: String): Pair<ImageVector, String>? = when (permission) {
    Manifest.permission.READ_PHONE_STATE   -> Icons.Filled.Phone to "Phone State"
    Manifest.permission.POST_NOTIFICATIONS -> Icons.Filled.Notifications to "Notifications"
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.CALL_PHONE         -> null
    else                                   -> null
}

private const val PREFS_NAME             = "settings"
private const val KEY_DENIED_FROM_SCREEN = "denied_perms_from_permission_screen"

private fun readDeniedPermsFromPrefs(context: android.content.Context): List<String> {
    val saved = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getStringSet(KEY_DENIED_FROM_SCREEN, emptySet()) ?: emptySet()
    val order = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )
    return order.filter { it in saved } + saved.filter { it !in order }
}

/**
 * Shows the permission dialog. When [priorityPermissions] are denied, they are
 * asked FIRST in their own system dialog (Phase 1), then remaining base perms
 * are asked in a second system dialog (Phase 2). This guarantees the user sees
 * the previously-denied perms first regardless of how Android orders batched
 * permission requests internally.
 */
@Composable
fun RequiredPermissionsDialog(
    priorityPermissions: List<String>? = null,
    onDismiss: () -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val priorityPerms = remember(priorityPermissions) {
        (priorityPermissions ?: readDeniedPermsFromPrefs(context)).toList()
    }
    Log.d(TAG, "RequiredPermissionsDialog opened with priorityPerms=$priorityPerms")

    if (allPermissionsGranted(context, priorityPerms)) {
        Log.d(TAG, "=== First composition: all permissions granted → immediate dismiss ===")
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    var overlaySettingsOpened by remember { mutableStateOf(false) }
    var pollingOverlay        by remember { mutableStateOf(false) }
    var showFallbackDialog    by remember { mutableStateOf(false) }
    var waitingForAppSettings by remember { mutableStateOf(false) }

    // Two-phase request flag. true = we just launched Phase 1 (priority perms)
    // and the next launcher callback should fire Phase 2 (base perms).
    var pendingBaseRequest by remember { mutableStateOf(false) }

    // Set by the launcher callback when Phase 2 needs to fire. A LaunchedEffect
    // observes this and launches the next system dialog (we can't call
    // permissionLauncher.launch from inside the launcher's own initializer).
    var phase2Perms by remember { mutableStateOf<Array<String>?>(null) }

    var runtimeGranted by remember {
        mutableStateOf(getMissingRuntimePermissions(context, priorityPerms).isEmpty())
    }
    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }

    Log.d(TAG, "=== Composition ===  runtimeGranted=$runtimeGranted  " +
            "overlayGranted=$overlayGranted  showFallback=$showFallbackDialog  " +
            "waitingForAppSettings=$waitingForAppSettings  pollingOverlay=$pollingOverlay  " +
            "pendingBaseRequest=$pendingBaseRequest  priorityPerms=$priorityPerms  " +
            "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}")

    // Decides what to do once permission requests have fully finished.
    fun handlePostRequestState() {
        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
        val nowOverlay        = isOverlayGranted(context)
        runtimeGranted = nowMissingRuntime.isEmpty()
        overlayGranted = nowOverlay

        Log.d(TAG, "  handlePostRequestState → missingRuntime=$nowMissingRuntime  " +
                "runtimeGranted=$runtimeGranted  overlayGranted=$overlayGranted")

        when {
            runtimeGranted && overlayGranted -> {
                Log.d(TAG, "  → ALL granted, dismissing ✓")
                onDismiss()
            }
            runtimeGranted && !overlayGranted -> {
                Log.d(TAG, "  → runtime OK, overlay missing → suppressing app-open ad, opening overlay settings")
                suppressAppOpenAd(context)
                overlaySettingsOpened = true
            }
            !runtimeGranted && overlayGranted -> {
                Log.d(TAG, "  → runtime denied, overlay OK → fallback dialog")
                showFallbackDialog = true
            }
            else -> {
                Log.d(TAG, "  → runtime denied + overlay missing → overlay settings first")
                overlaySettingsOpened = true
            }
        }
    }

    // ── Runtime permission launcher ───────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "── Runtime launcher callback ──  pendingBaseRequest=$pendingBaseRequest")
        Log.d(TAG, "  raw results        : $results")

        if (pendingBaseRequest) {
            // Phase 1 (priority perms) just finished. Stage Phase 2 (base perms);
            // a LaunchedEffect below will actually fire it. We can't call
            // permissionLauncher.launch from inside its own initializer lambda.
            pendingBaseRequest = false
            val baseMinusPriority = getBaseMinusPriority(priorityPerms)
                .filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

            if (baseMinusPriority.isNotEmpty()) {
                Log.d(TAG, "  Phase 1 done → staging Phase 2 (base perms): $baseMinusPriority")
                phase2Perms = baseMinusPriority.toTypedArray()
                return@rememberLauncherForActivityResult
            } else {
                Log.d(TAG, "  Phase 1 done → no base perms missing, skipping Phase 2")
            }
        }

        handlePostRequestState()
    }

    // Fires Phase 2 once the launcher reference is fully constructed.
    LaunchedEffect(phase2Perms) {
        val perms = phase2Perms ?: return@LaunchedEffect
        Log.d(TAG, "LaunchedEffect → firing Phase 2: ${perms.toList()}")
        phase2Perms = null
        permissionLauncher.launch(perms)
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

                val nowMissing = getMissingRuntimePermissions(context, priorityPerms)
                runtimeGranted = nowMissing.isEmpty()
                Log.d(TAG, "  after overlay grant: runtimeGranted=$runtimeGranted  " +
                        "missingRuntime=$nowMissing")

                if (runtimeGranted) {
                    Log.d(TAG, "  → ALL granted, suppressing app-open ad and dismissing ✓")
                    suppressAppOpenAd(context)
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

            if (pollingOverlay) {
                val stillDenied = !isOverlayGranted(context)
                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
                if (stillDenied) {
                    pollingOverlay = false
                    overlayGranted = false
                    val nowMissing = getMissingRuntimePermissions(context, priorityPerms)
                    runtimeGranted = nowMissing.isEmpty()
                    Log.d(TAG, "  overlay denied on return  runtimeGranted=$runtimeGranted  " +
                            "missingRuntime=$nowMissing")
                    if (runtimeGranted) {
                        Log.d(TAG, "  → runtime OK, overlay skipped → suppressing app-open ad and dismissing ✓")
                        suppressAppOpenAd(context)
                        onDismiss()
                    } else {
                        Log.d(TAG, "  → runtime still missing → fallback dialog")
                        showFallbackDialog = true
                    }
                }
                return@LifecycleEventObserver
            }

            if (waitingForAppSettings) {
                val nowMissing = getMissingRuntimePermissions(context, priorityPerms)
                val nowOverlay = isOverlayGranted(context)
                runtimeGranted = nowMissing.isEmpty()
                overlayGranted = nowOverlay
                waitingForAppSettings = false

                Log.d(TAG, "  [app-settings path] runtimeGranted=$runtimeGranted  " +
                        "overlayGranted=$overlayGranted  missingRuntime=$nowMissing")

                if (runtimeGranted) {
                    Log.d(TAG, "  → runtime granted (overlay=$overlayGranted) → suppressing app-open ad and dismissing ✓")
                    suppressAppOpenAd(context)
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

    // ── Build extra cards for priority permissions ────────────────────────
    val extraCards: List<PermissionItem> = remember(priorityPerms, runtimeGranted) {
        priorityPerms
            .mapNotNull { perm ->
                val info = permissionToCardInfo(perm) ?: return@mapNotNull null
                val granted = context.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED
                PermissionItem(icon = info.first, label = info.second, isGranted = granted)
            }
            .distinctBy { it.label }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    if (showFallbackDialog) {
        PermissionsDialogContent(
            runtimeGranted = runtimeGranted,
            overlayGranted = overlayGranted,
            extraCards     = extraCards,
            onContinue = {
                Log.d(TAG, "Fallback Continue tapped → app details settings  " +
                        "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}  " +
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
            extraCards     = extraCards,
            onContinue = {
                // Two-phase launch: Phase 1 = priority perms (asked FIRST),
                // Phase 2 = base perms minus priority (asked from launcher callback).
                val missingPriority = priorityPerms
                    .filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

                if (missingPriority.isNotEmpty()) {
                    pendingBaseRequest = true
                    Log.d(TAG, "Continue tapped → Phase 1 (priority perms): $missingPriority  (Phase 2 will follow)")
                    permissionLauncher.launch(missingPriority.toTypedArray())
                } else {
                    pendingBaseRequest = false
                    val perms = getAllRequiredPermissions(priorityPerms)
                    Log.d(TAG, "Continue tapped → single batch (no missing priority): $perms")
                    permissionLauncher.launch(perms.toTypedArray())
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PermissionsDialogContent(
    runtimeGranted: Boolean,
    overlayGranted: Boolean,
    extraCards: List<PermissionItem>,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    val permissionItems = buildList {
        addAll(extraCards)
        add(PermissionItem(Icons.Filled.Call,   "Call",     isGranted = runtimeGranted))
        add(PermissionItem(Icons.Filled.Person, "Contacts", isGranted = runtimeGranted))
        add(PermissionItem(Icons.Filled.Layers, "Overlay",  isGranted = overlayGranted))
    }

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