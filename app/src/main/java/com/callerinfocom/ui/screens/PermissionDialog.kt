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
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material.icons.filled.Layers
//import androidx.compose.material.icons.filled.Notifications
//import androidx.compose.material.icons.filled.Phone
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
//    val icon     : ImageVector,
//    val label    : String,
//    val isGranted: Boolean = false   // kept for binary compatibility; UI only shows ungranted now
//)
//
///**
// * Base permissions the dialog always requests.
// * ANSWER_PHONE_CALLS deliberately omitted — privileged, not user-grantable.
// */
//private fun getBasePermissions(): List<String> = listOf(
//    Manifest.permission.READ_CONTACTS,
//    Manifest.permission.READ_CALL_LOG,
//    Manifest.permission.WRITE_CONTACTS,
//    Manifest.permission.POST_NOTIFICATIONS,
//    Manifest.permission.READ_PHONE_STATE,
//    Manifest.permission.CALL_PHONE
//)
//
///**
// * Final request list for the system permission launcher.
// * Priority perms first (deduped, in order), then base perms.
// * The OS may reorder dialogs internally, but the *array we pass* puts priority first.
// */
//private fun getRequestablePermissions(priorityPermissions: List<String>): List<String> {
//    val result = LinkedHashSet<String>()
//    priorityPermissions.forEach { result.add(it) }
//    getBasePermissions().forEach { result.add(it) }
//    return result.toList()
//}
//
//private fun getMissingRuntimePermissions(
//    context: android.content.Context,
//    priorityPermissions: List<String>
//): List<String> =
//    getRequestablePermissions(priorityPermissions).filter {
//        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
//    }
//
//private fun isOverlayGranted(context: android.content.Context): Boolean =
//    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
//
//private fun allPermissionsGranted(
//    context: android.content.Context,
//    priorityPermissions: List<String>
//): Boolean =
//    getMissingRuntimePermissions(context, priorityPermissions).isEmpty() && isOverlayGranted(context)
//
//private fun suppressAppOpenAd(context: android.content.Context) {
//    context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
//        .edit().putBoolean("skip_app_open_ad", true).apply()
//}
//
///**
// * Card definitions. Each maps a *card* to the set of underlying perms that must
// * all be granted for the card to disappear. The "Call" card hides only when both
// * READ_CALL_LOG and CALL_PHONE are granted; "Contacts" hides when READ_CONTACTS
// * and WRITE_CONTACTS are granted; etc. This way a single missed perm keeps the
// * relevant card visible.
// */
//private data class CardDef(
//    val icon         : ImageVector,
//    val label        : String,
//    val gatingPerms  : List<String>
//)
//
//private val BASE_CARDS: List<CardDef> = listOf(
//    CardDef(
//        icon        = Icons.Filled.Call,
//        label       = "Call",
//        gatingPerms = listOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE)
//    ),
//    CardDef(
//        icon        = Icons.Filled.Person,
//        label       = "Contacts",
//        gatingPerms = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
//    )
//)
//
///**
// * Per-permission card (used for priority perms denied on PermissionScreen).
// * Returns null for perms already covered by a base card.
// */
//private fun priorityCardFor(permission: String): CardDef? = when (permission) {
//    Manifest.permission.READ_PHONE_STATE   -> CardDef(
//        icon        = Icons.Filled.Phone,
//        label       = "Phone State",
//        gatingPerms = listOf(Manifest.permission.READ_PHONE_STATE)
//    )
//    Manifest.permission.POST_NOTIFICATIONS -> CardDef(
//        icon        = Icons.Filled.Notifications,
//        label       = "Notifications",
//        gatingPerms = listOf(Manifest.permission.POST_NOTIFICATIONS)
//    )
//    // Already covered by a base card
//    Manifest.permission.READ_CONTACTS,
//    Manifest.permission.WRITE_CONTACTS,
//    Manifest.permission.READ_CALL_LOG,
//    Manifest.permission.CALL_PHONE         -> null
//    else                                   -> null
//}
//
//private const val PREFS_NAME             = "settings"
//private const val KEY_DENIED_FROM_SCREEN = "denied_perms_from_permission_screen"
//
//private fun readDeniedPermsFromPrefs(context: android.content.Context): List<String> {
//    val saved = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
//        .getStringSet(KEY_DENIED_FROM_SCREEN, emptySet()) ?: emptySet()
//    val order = listOf(
//        Manifest.permission.READ_PHONE_STATE,
//        Manifest.permission.POST_NOTIFICATIONS
//    )
//    return order.filter { it in saved } + saved.filter { it !in order }
//}
//
///**
// * Shows the permission dialog. Priority perms (denied on PermissionScreen) are
// * placed FIRST in the permission launcher array. Cards for any permission the
// * user has already granted are hidden — the dialog only shows what's still missing.
// */
//@Composable
//fun RequiredPermissionsDialog(
//    priorityPermissions: List<String>? = null,
//    onDismiss: () -> Unit
//) {
//    val context        = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//
//    val priorityPerms = remember(priorityPermissions) {
//        (priorityPermissions ?: readDeniedPermsFromPrefs(context)).toList()
//    }
//    Log.d(TAG, "RequiredPermissionsDialog opened with priorityPerms=$priorityPerms")
//
//    if (allPermissionsGranted(context, priorityPerms)) {
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
//    // Bumped whenever permission state may have changed. Used to re-derive
//    // which cards should still be visible.
//    var permissionCheckTrigger by remember { mutableStateOf(0) }
//
//    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }
//
//    Log.d(TAG, "=== Composition ===  overlayGranted=$overlayGranted  " +
//            "showFallback=$showFallbackDialog  waitingForAppSettings=$waitingForAppSettings  " +
//            "pollingOverlay=$pollingOverlay  priorityPerms=$priorityPerms  " +
//            "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}")
//
//    // Decides what to do once permission requests have fully finished.
//    fun handlePostRequestState() {
//        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
//        val nowOverlay        = isOverlayGranted(context)
//        val runtimeGranted    = nowMissingRuntime.isEmpty()
//        overlayGranted        = nowOverlay
//        permissionCheckTrigger++   // re-derive cards
//
//        Log.d(TAG, "  handlePostRequestState → missingRuntime=$nowMissingRuntime  " +
//                "runtimeGranted=$runtimeGranted  overlayGranted=$overlayGranted")
//
//        when {
//            runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  → ALL granted, dismissing ✓")
//                onDismiss()
//            }
//            runtimeGranted && !overlayGranted -> {
//                Log.d(TAG, "  → runtime OK, overlay missing → suppressing app-open ad, opening overlay settings")
//                suppressAppOpenAd(context)
//                overlaySettingsOpened = true
//            }
//            !runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  → runtime denied, overlay OK → fallback dialog")
//                showFallbackDialog = true
//            }
//            else -> {
//                Log.d(TAG, "  → runtime denied + overlay missing → overlay settings first")
//                overlaySettingsOpened = true
//            }
//        }
//    }
//
//    // ── Runtime permission launcher ───────────────────────────────────────
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { results ->
//        Log.d(TAG, "── Runtime launcher callback ──")
//        Log.d(TAG, "  raw results : $results")
//        handlePostRequestState()
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
//                permissionCheckTrigger++
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
//                val nowMissing     = getMissingRuntimePermissions(context, priorityPerms)
//                val runtimeGranted = nowMissing.isEmpty()
//                Log.d(TAG, "  after overlay grant: runtimeGranted=$runtimeGranted  " +
//                        "missingRuntime=$nowMissing")
//
//                if (runtimeGranted) {
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
//            // Whenever the app comes back, re-check perms so cards refresh
//            // (handles users granting from system Settings outside our flow).
//            permissionCheckTrigger++
//
//            if (pollingOverlay) {
//                val stillDenied = !isOverlayGranted(context)
//                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
//                if (stillDenied) {
//                    pollingOverlay = false
//                    overlayGranted = false
//                    val nowMissing     = getMissingRuntimePermissions(context, priorityPerms)
//                    val runtimeGranted = nowMissing.isEmpty()
//                    Log.d(TAG, "  overlay denied on return  runtimeGranted=$runtimeGranted  " +
//                            "missingRuntime=$nowMissing")
//                    if (runtimeGranted) {
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
//            if (waitingForAppSettings) {
//                val nowMissing     = getMissingRuntimePermissions(context, priorityPerms)
//                val nowOverlay     = isOverlayGranted(context)
//                val runtimeGranted = nowMissing.isEmpty()
//                overlayGranted     = nowOverlay
//                waitingForAppSettings = false
//
//                Log.d(TAG, "  [app-settings path] runtimeGranted=$runtimeGranted  " +
//                        "overlayGranted=$overlayGranted  missingRuntime=$nowMissing")
//
//                if (runtimeGranted) {
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
//    // ── Build the *visible* card list (only cards whose perms are still missing) ──
//    // priorityPerms first (in their requested order), then base cards (Call, Contacts),
//    // then Overlay (if missing). Anything fully granted is omitted entirely.
//    val visibleCards: List<PermissionItem> = remember(
//        priorityPerms,
//        permissionCheckTrigger,
//        overlayGranted
//    ) {
//        val items = mutableListOf<PermissionItem>()
//        val seenLabels = mutableSetOf<String>()
//
//        // 1. Priority cards first
//        for (perm in priorityPerms) {
//            val def = priorityCardFor(perm) ?: continue
//            if (def.label in seenLabels) continue
//            val allGranted = def.gatingPerms.all {
//                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
//            }
//            if (!allGranted) {
//                items.add(PermissionItem(def.icon, def.label, isGranted = false))
//                seenLabels.add(def.label)
//            }
//        }
//
//        // 2. Base cards (Call, Contacts)
//        for (def in BASE_CARDS) {
//            if (def.label in seenLabels) continue
//            val allGranted = def.gatingPerms.all {
//                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
//            }
//            if (!allGranted) {
//                items.add(PermissionItem(def.icon, def.label, isGranted = false))
//                seenLabels.add(def.label)
//            }
//        }
//
//        // 3. Overlay card (only when not granted)
//        if (!overlayGranted) {
//            items.add(PermissionItem(Icons.Filled.Layers, "Overlay", isGranted = false))
//        }
//
//        items
//    }
//
//    // Safety net: if every card just got hidden but the auto-dismiss in handlePostRequestState
//    // didn't catch it (e.g. lifecycle re-check found everything granted), close the dialog now.
//    LaunchedEffect(visibleCards) {
//        if (visibleCards.isEmpty() && allPermissionsGranted(context, priorityPerms)) {
//            Log.d(TAG, "All cards hidden + perms granted → dismissing ✓")
//            onDismiss()
//        }
//    }
//
//    // ── UI ────────────────────────────────────────────────────────────────
//    if (showFallbackDialog) {
//        PermissionsDialogContent(
//            visibleCards = visibleCards,
//            onContinue = {
//                Log.d(TAG, "Fallback Continue tapped → app details settings  " +
//                        "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}  " +
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
//            visibleCards = visibleCards,
//            onContinue = {
//                // Single batch — priority perms first in the array.
//                // Filter to only what's currently missing so we don't ask
//                // already-granted perms again.
//                val perms = getRequestablePermissions(priorityPerms)
//                    .filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
//
//                if (perms.isNotEmpty()) {
//                    Log.d(TAG, "Continue tapped → single batch (priority first): $perms")
//                    permissionLauncher.launch(perms.toTypedArray())
//                } else {
//                    Log.d(TAG, "Continue tapped → no runtime perms missing, going straight to post-request state")
//                    handlePostRequestState()
//                }
//            },
//            onDismiss = onDismiss
//        )
//    }
//}
//
//@Composable
//private fun PermissionsDialogContent(
//    visibleCards: List<PermissionItem>,
//    onContinue  : () -> Unit,
//    onDismiss   : () -> Unit
//) {
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
//                // Only renders cards for perms that are still NOT granted.
//                visibleCards.forEach { item ->
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier          = Modifier.padding(vertical = 8.dp)
//                    ) {
//                        Box(
//                            contentAlignment = Alignment.Center,
//                            modifier         = Modifier
//                                .size(40.dp)
//                                .background(
//                                    color = MaterialTheme.colorScheme.onSurface,
//                                    shape = CircleShape
//                                )
//                        ) {
//                            Icon(
//                                imageVector        = item.icon,
//                                contentDescription = item.label,
//                                tint               = MaterialTheme.colorScheme.surface,
//                                modifier           = Modifier.size(20.dp)
//                            )
//                        }
//                        Spacer(modifier = Modifier.width(16.dp))
//                        Text(
//                            text       = item.label,
//                            fontWeight = FontWeight.SemiBold,
//                            fontSize   = 16.sp,
//                            color      = MaterialTheme.colorScheme.onSurface
//                        )
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
    val icon     : ImageVector,
    val label    : String,
    val isGranted: Boolean = false   // kept for binary compatibility; UI only shows ungranted now
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
 * Final request list for the system permission launcher.
 * Priority perms first (deduped, in order), then base perms.
 * The OS may reorder dialogs internally, but the *array we pass* puts priority first.
 */
private fun getRequestablePermissions(priorityPermissions: List<String>): List<String> {
    val result = LinkedHashSet<String>()
    priorityPermissions.forEach { result.add(it) }
    getBasePermissions().forEach { result.add(it) }
    return result.toList()
}

private fun getMissingRuntimePermissions(
    context: android.content.Context,
    priorityPermissions: List<String>
): List<String> =
    getRequestablePermissions(priorityPermissions).filter {
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

/**
 * Card definitions. Each maps a *card* to the set of underlying perms that must
 * all be granted for the card to disappear. The "Call" card hides only when both
 * READ_CALL_LOG and CALL_PHONE are granted; "Contacts" hides when READ_CONTACTS
 * and WRITE_CONTACTS are granted; etc. This way a single missed perm keeps the
 * relevant card visible.
 */
private data class CardDef(
    val icon         : ImageVector,
    val label        : String,
    val gatingPerms  : List<String>
)

private val BASE_CARDS: List<CardDef> = listOf(
    CardDef(
        icon        = Icons.Filled.Call,
        label       = "Call",
        gatingPerms = listOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE)
    ),
    CardDef(
        icon        = Icons.Filled.Person,
        label       = "Contacts",
        gatingPerms = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
    )
)

/**
 * Per-permission card (used for priority perms denied on PermissionScreen).
 * Returns null for perms already covered by a base card.
 */
private fun priorityCardFor(permission: String): CardDef? = when (permission) {
    Manifest.permission.READ_PHONE_STATE   -> CardDef(
        icon        = Icons.Filled.Phone,
        label       = "Phone State",
        gatingPerms = listOf(Manifest.permission.READ_PHONE_STATE)
    )
    Manifest.permission.POST_NOTIFICATIONS -> CardDef(
        icon        = Icons.Filled.Notifications,
        label       = "Notifications",
        gatingPerms = listOf(Manifest.permission.POST_NOTIFICATIONS)
    )
    // Already covered by a base card
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.WRITE_CONTACTS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.CALL_PHONE         -> null
    else                                   -> null
}

private const val PREFS_NAME             = "settings"
private const val KEY_DENIED_FROM_SCREEN = "denied_perms_from_permission_screen"
private const val KEY_PERMS_EVER_ASKED   = "perms_ever_asked"

private fun readDeniedPermsFromPrefs(context: android.content.Context): List<String> {
    val saved = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getStringSet(KEY_DENIED_FROM_SCREEN, emptySet()) ?: emptySet()
    val order = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.POST_NOTIFICATIONS
    )
    return order.filter { it in saved } + saved.filter { it !in order }
}

/** Permissions that have ever been launched through this app's permission flow. */
private fun readPermsEverAsked(context: android.content.Context): Set<String> =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getStringSet(KEY_PERMS_EVER_ASKED, emptySet()) ?: emptySet()

private fun appendPermsEverAsked(context: android.content.Context, perms: Collection<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val current = prefs.getStringSet(KEY_PERMS_EVER_ASKED, emptySet())?.toMutableSet() ?: mutableSetOf()
    current.addAll(perms)
    prefs.edit().putStringSet(KEY_PERMS_EVER_ASKED, current).apply()
}

/**
 * Shows the permission dialog. Priority perms (denied on PermissionScreen) are
 * placed FIRST in the permission launcher array. Cards for any permission the
 * user has already granted are hidden — the dialog only shows what's still missing.
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

    // Bumped whenever permission state may have changed. Used to re-derive
    // which cards should still be visible.
    var permissionCheckTrigger by remember { mutableStateOf(0) }

    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }

    Log.d(TAG, "=== Composition ===  overlayGranted=$overlayGranted  " +
            "showFallback=$showFallbackDialog  waitingForAppSettings=$waitingForAppSettings  " +
            "pollingOverlay=$pollingOverlay  priorityPerms=$priorityPerms  " +
            "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}")

    // Decides what to do once permission requests have fully finished.
    fun handlePostRequestState() {
        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
        val nowOverlay        = isOverlayGranted(context)
        val runtimeGranted    = nowMissingRuntime.isEmpty()
        overlayGranted        = nowOverlay
        permissionCheckTrigger++   // re-derive cards

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
            !runtimeGranted -> {
                // Any runtime perm still denied → send the user to app-permission
                // settings (fallback dialog), NOT to overlay settings. Overlay is
                // only handled once all runtime perms are granted.
                Log.d(TAG, "  → runtime denied (overlay=$overlayGranted) → fallback dialog")
                showFallbackDialog = true
            }
            else -> {
                // Defensive: runtimeGranted && overlayGranted is handled above;
                // this branch shouldn't be reachable, but keep it as a safe no-op.
                Log.d(TAG, "  → unreachable branch reached, dismissing as a safety")
                onDismiss()
            }
        }
    }

    // ── Runtime permission launcher ───────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "── Runtime launcher callback ──")
        Log.d(TAG, "  raw results : $results")

        // The launcher walked through every permission it was given.
        // Android shows them in sequence inside the same launcher invocation,
        // so by the time we get here the user has seen every prompt.
        permissionCheckTrigger++
        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
        val nowOverlay        = isOverlayGranted(context)
        overlayGranted        = nowOverlay
        val runtimeGranted    = nowMissingRuntime.isEmpty()

        when {
            // Everything granted → done.
            runtimeGranted && overlayGranted -> {
                Log.d(TAG, "  launcher → ALL granted, dismissing ✓")
                onDismiss()
            }
            // Runtime fully granted, overlay missing → existing overlay flow.
            runtimeGranted && !overlayGranted -> {
                Log.d(TAG, "  launcher → runtime OK, overlay missing → opening overlay settings")
                suppressAppOpenAd(context)
                overlaySettingsOpened = true
            }
            // Some runtime perm still denied after seeing all prompts →
            // go straight to app-settings WITHOUT re-showing the dialog.
            // The lifecycle observer's app-settings path will react when
            // the user returns. `showFallbackDialog` stays false so the
            // dialog UI doesn't pop back up.
            else -> {
                Log.d(TAG, "  launcher → runtime denied after full prompt → opening app-settings  missingRuntime=$nowMissingRuntime")
                waitingForAppSettings = true
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
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
                permissionCheckTrigger++
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

                val nowMissing     = getMissingRuntimePermissions(context, priorityPerms)
                val runtimeGranted = nowMissing.isEmpty()
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

            // Whenever the app comes back, re-check perms so cards refresh
            // (handles users granting from system Settings outside our flow).
            permissionCheckTrigger++

            if (pollingOverlay) {
                val stillDenied = !isOverlayGranted(context)
                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
                if (stillDenied) {
                    pollingOverlay = false
                    overlayGranted = false
                    val nowMissing     = getMissingRuntimePermissions(context, priorityPerms)
                    val runtimeGranted = nowMissing.isEmpty()
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
                val nowMissing     = getMissingRuntimePermissions(context, priorityPerms)
                val nowOverlay     = isOverlayGranted(context)
                val runtimeGranted = nowMissing.isEmpty()
                overlayGranted     = nowOverlay
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

    val visibleCards: List<PermissionItem> = remember(
        priorityPerms,
        permissionCheckTrigger,
        overlayGranted
    ) {
        val items = mutableListOf<PermissionItem>()
        val seenLabels = mutableSetOf<String>()

        // 1. Priority cards first
        for (perm in priorityPerms) {
            val def = priorityCardFor(perm) ?: continue
            if (def.label in seenLabels) continue
            val allGranted = def.gatingPerms.all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
            if (!allGranted) {
                items.add(PermissionItem(def.icon, def.label, isGranted = false))
                seenLabels.add(def.label)
            }
        }

        // 2. Base cards (Call, Contacts)
        for (def in BASE_CARDS) {
            if (def.label in seenLabels) continue
            val allGranted = def.gatingPerms.all {
                context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
            }
            if (!allGranted) {
                items.add(PermissionItem(def.icon, def.label, isGranted = false))
                seenLabels.add(def.label)
            }
        }

        // 3. Overlay card (only when not granted)
        if (!overlayGranted) {
            items.add(PermissionItem(Icons.Filled.Layers, "Overlay", isGranted = false))
        }

        items
    }

    // Safety net: if every card just got hidden but the auto-dismiss in handlePostRequestState
    // didn't catch it (e.g. lifecycle re-check found everything granted), close the dialog now.
    LaunchedEffect(visibleCards) {
        if (visibleCards.isEmpty() && allPermissionsGranted(context, priorityPerms)) {
            Log.d(TAG, "All cards hidden + perms granted → dismissing ✓")
            onDismiss()
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────
    if (showFallbackDialog) {
        PermissionsDialogContent(
            visibleCards = visibleCards,
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
            visibleCards = visibleCards,
            onContinue = {
                // Single batch — priority perms first in the array.
                // Filter to only what's currently missing so we don't ask
                // already-granted perms again.
                val perms = getRequestablePermissions(priorityPerms)
                    .filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

                if (perms.isEmpty()) {
                    Log.d(TAG, "Continue tapped → no runtime perms missing, going straight to post-request state")
                    handlePostRequestState()
                    return@PermissionsDialogContent
                }

                val activity = context as? android.app.Activity
                val everAsked = readPermsEverAsked(context)
                val allPermanentlyDenied = activity != null && perms.all { perm ->
                    perm in everAsked &&
                            !androidx.core.app.ActivityCompat
                                .shouldShowRequestPermissionRationale(activity, perm)
                }

                if (allPermanentlyDenied) {
                    Log.d(TAG, "Continue tapped → all perms permanently denied → skipping launcher, opening app-settings")
                    waitingForAppSettings = true
                    showFallbackDialog    = true
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                } else {
                    Log.d(TAG, "Continue tapped → single batch (priority first): $perms")
                    // Record that these perms have now been asked from this Activity.
                    appendPermsEverAsked(context, perms)
                    permissionLauncher.launch(perms.toTypedArray())
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun PermissionsDialogContent(
    visibleCards: List<PermissionItem>,
    onContinue  : () -> Unit,
    onDismiss   : () -> Unit
) {
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

                // Only renders cards for perms that are still NOT granted.
                visibleCards.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier
                                .size(40.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector        = item.icon,
                                contentDescription = item.label,
                                tint               = MaterialTheme.colorScheme.surface,
                                modifier           = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text       = item.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 16.sp,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
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