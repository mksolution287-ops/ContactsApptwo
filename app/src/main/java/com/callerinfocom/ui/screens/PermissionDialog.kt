//
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
//    Manifest.permission.READ_CONTACTS,
//    Manifest.permission.WRITE_CONTACTS,
//    Manifest.permission.READ_CALL_LOG,
//    Manifest.permission.CALL_PHONE         -> null
//    else                                   -> null
//}
//
//private const val PREFS_NAME             = "settings"
//private const val KEY_DENIED_FROM_SCREEN = "denied_perms_from_permission_screen"
//private const val KEY_PERMS_EVER_ASKED   = "perms_ever_asked"
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
//private fun readPermsEverAsked(context: android.content.Context): Set<String> =
//    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
//        .getStringSet(KEY_PERMS_EVER_ASKED, emptySet()) ?: emptySet()
//
//private fun appendPermsEverAsked(context: android.content.Context, perms: Collection<String>) {
//    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
//    val current = prefs.getStringSet(KEY_PERMS_EVER_ASKED, emptySet())?.toMutableSet() ?: mutableSetOf()
//    current.addAll(perms)
//    prefs.edit().putStringSet(KEY_PERMS_EVER_ASKED, current).apply()
//}
//
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
//    // FLICKER FIX 1:
//    // Synchronous early-return: don't render ANYTHING if perms already granted.
//    // Using LaunchedEffect(Unit) { onDismiss() } caused a 1-frame flicker of the
//    // empty card because LaunchedEffect schedules its body for AFTER the first
//    // frame is laid out. `remember` + `SideEffect` + an early `return` ensures
//    // the Dialog composable subtree is never entered at all.
//    val alreadyGrantedAtStart = remember { allPermissionsGranted(context, priorityPerms) }
//    if (alreadyGrantedAtStart) {
//        Log.d(TAG, "=== First composition: all permissions granted → immediate dismiss (no render) ===")
//        SideEffect { onDismiss() }
//        return
//    }
//
//    var overlaySettingsOpened by remember { mutableStateOf(false) }
//    var pollingOverlay        by remember { mutableStateOf(false) }
//    var showFallbackDialog    by remember { mutableStateOf(false) }
//    var waitingForAppSettings by remember { mutableStateOf(false) }
//
//    var dismissAfterOverlayReturn by remember { mutableStateOf(false) }
//
//    // NEW: tracks whether we've already done the one-shot "retry denied perms"
//    // pass for this dialog session. Prevents an infinite re-prompt loop if the
//    // user keeps denying.
//    var retryAttempted by remember { mutableStateOf(false) }
//
//    var permissionCheckTrigger by remember { mutableStateOf(0) }
//
//    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }
//
//    Log.d(TAG, "=== Composition ===  overlayGranted=$overlayGranted  " +
//            "showFallback=$showFallbackDialog  waitingForAppSettings=$waitingForAppSettings  " +
//            "pollingOverlay=$pollingOverlay  dismissAfterOverlayReturn=$dismissAfterOverlayReturn  " +
//            "priorityPerms=$priorityPerms  " +
//            "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}")
//
//    fun handlePostRequestState() {
//        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
//        val nowOverlay        = isOverlayGranted(context)
//        val runtimeGranted    = nowMissingRuntime.isEmpty()
//        overlayGranted        = nowOverlay
//        permissionCheckTrigger++
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
//            !runtimeGranted -> {
//                Log.d(TAG, "  → runtime denied (overlay=$overlayGranted) → fallback dialog")
//                showFallbackDialog = true
//            }
//            else -> {
//                Log.d(TAG, "  → unreachable branch reached, dismissing as a safety")
//                onDismiss()
//            }
//        }
//    }
//
//    // ── Runtime permission launcher ───────────────────────────────────────
//    // Held in a mutable ref so the callback below can re-invoke the launcher
//    // for the retry pass. (You can't reference `permissionLauncher` from
//    // inside the lambda that initializes it.)
//    val launcherRef = remember {
//        mutableStateOf<androidx.activity.result.ActivityResultLauncher<Array<String>>?>(null)
//    }
//
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { results ->
//        Log.d(TAG, "── Runtime launcher callback ──")
//        Log.d(TAG, "  raw results : $results")
//
//        permissionCheckTrigger++
//        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
//        val nowOverlay        = isOverlayGranted(context)
//        overlayGranted        = nowOverlay
//        val runtimeGranted    = nowMissingRuntime.isEmpty()
//
//        when {
//            runtimeGranted && overlayGranted -> {
//                Log.d(TAG, "  launcher → ALL granted, dismissing ✓")
//                onDismiss()
//            }
//            runtimeGranted && !overlayGranted -> {
//                Log.d(TAG, "  launcher → runtime OK, overlay missing → opening overlay settings")
//                suppressAppOpenAd(context)
//                overlaySettingsOpened = true
//            }
//            else -> {
//                // Before falling back to app-settings, give denied perms one more
//                // chance through the system dialog. Only re-prompt perms for which
//                // the OS will still show a dialog (shouldShowRequestPermissionRationale
//                // == true). Permanently-denied perms wouldn't show anything, so
//                // they skip the retry and go straight to app-settings.
//                val activity = context as? android.app.Activity
//                val retriablePerms = if (activity != null) {
//                    nowMissingRuntime.filter { perm ->
//                        androidx.core.app.ActivityCompat
//                            .shouldShowRequestPermissionRationale(activity, perm)
//                    }
//                } else {
//                    emptyList()
//                }
//
//                val retryLauncher = launcherRef.value
//                if (!retryAttempted && retriablePerms.isNotEmpty() && retryLauncher != null) {
//                    Log.d(TAG, "  launcher → ${nowMissingRuntime.size} denied, retrying ${retriablePerms.size}: $retriablePerms")
//                    retryAttempted = true
//                    appendPermsEverAsked(context, retriablePerms)
//                    retryLauncher.launch(retriablePerms.toTypedArray())
//                } else {
//                    Log.d(TAG, "  launcher → runtime denied (retryAttempted=$retryAttempted, retriable=$retriablePerms) → opening app-settings  missingRuntime=$nowMissingRuntime")
//                    waitingForAppSettings = true
//                    context.startActivity(
//                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                            data = Uri.parse("package:${context.packageName}")
//                        }
//                    )
//                }
//            }
//        }
//    }
//
//    // Publish the launcher into the ref so the callback can use it for retries.
//    SideEffect { launcherRef.value = permissionLauncher }
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
//                // NEW: if we're in the “dismiss-after-overlay-return” mode,
//                // close the dialog regardless of runtime state.
//                if (dismissAfterOverlayReturn) {
//                    Log.d(TAG, "  → dismissAfterOverlayReturn=true (overlay granted) → dismissing ✓")
//                    suppressAppOpenAd(context)
//                    onDismiss()
//                    break
//                }
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
//                    "waitingForAppSettings=$waitingForAppSettings  " +
//                    "dismissAfterOverlayReturn=$dismissAfterOverlayReturn")
//
//            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
//
//            permissionCheckTrigger++
//
//            if (pollingOverlay) {
//                val stillDenied = !isOverlayGranted(context)
//                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
//                if (stillDenied) {
//                    pollingOverlay = false
//                    overlayGranted = false
//
//                    // NEW: if the user came here via the "post app-settings overlay" flow,
//                    // dismiss now regardless of overlay outcome (denied case).
//                    if (dismissAfterOverlayReturn) {
//                        Log.d(TAG, "  → dismissAfterOverlayReturn=true (overlay denied on return) → dismissing ✓")
//                        suppressAppOpenAd(context)
//                        onDismiss()
//                        return@LifecycleEventObserver
//                    }
//
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
//                when {
//                    // Everything granted → done.
//                    runtimeGranted && overlayGranted -> {
//                        Log.d(TAG, "  → ALL granted → suppressing app-open ad and dismissing ✓")
//                        suppressAppOpenAd(context)
//                        onDismiss()
//                    }
//
//                    runtimeGranted && !overlayGranted -> {
//                        Log.d(TAG, "  → runtime granted, overlay missing → showing overlay card, arming dismiss-after-overlay-return")
//                        showFallbackDialog          = false
//                        dismissAfterOverlayReturn   = true
//                    }
//                    else -> {
//                        Log.d(TAG, "  → runtime still missing after app settings → keep fallback")
//                    }
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
//    val visibleCards: List<PermissionItem> = remember(
//        priorityPerms,
//        permissionCheckTrigger,
//        overlayGranted
//    ) {
//        val items = mutableListOf<PermissionItem>()
//        val seenLabels = mutableSetOf<String>()
//
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
//        if (!overlayGranted) {
//            items.add(PermissionItem(Icons.Filled.Layers, "Overlay", isGranted = false))
//        }
//
//        items
//    }
//
//    // FLICKER FIX 3:
//    // If `visibleCards` is empty AND all perms are granted, dismiss synchronously
//    // (SideEffect) rather than via LaunchedEffect, which would let an empty card
//    // render for a frame first.
//    if (visibleCards.isEmpty()) {
//        SideEffect {
//            if (allPermissionsGranted(context, priorityPerms)) {
//                Log.d(TAG, "All cards hidden + perms granted → dismissing ✓")
//                onDismiss()
//            }
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
//                // If the only thing missing is overlay (typical after the
//                // app-settings round-trip granted all runtime perms), arm the
//                // dismiss-after-overlay-return flag so that whether the user
//                // grants or denies overlay, the dialog closes when they come back.
//                val runtimeStillMissing = getMissingRuntimePermissions(context, priorityPerms)
//                val overlayStillMissing = !isOverlayGranted(context)
//
//                if (runtimeStillMissing.isEmpty() && overlayStillMissing) {
//                    Log.d(TAG, "Continue tapped → only overlay missing → arming dismissAfterOverlayReturn and opening overlay settings")
//                    dismissAfterOverlayReturn = true
//                    suppressAppOpenAd(context)
//                    overlaySettingsOpened = true
//                    return@PermissionsDialogContent
//                }
//
//                val perms = getRequestablePermissions(priorityPerms)
//                    .filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
//
//                if (perms.isEmpty()) {
//                    Log.d(TAG, "Continue tapped → no runtime perms missing, going straight to post-request state")
//                    handlePostRequestState()
//                    return@PermissionsDialogContent
//                }
//
//                val activity = context as? android.app.Activity
//                val everAsked = readPermsEverAsked(context)
//                val allPermanentlyDenied = activity != null && perms.all { perm ->
//                    perm in everAsked &&
//                            !androidx.core.app.ActivityCompat
//                                .shouldShowRequestPermissionRationale(activity, perm)
//                }
//
//                if (allPermanentlyDenied) {
//                    Log.d(TAG, "Continue tapped → all perms permanently denied → skipping launcher, opening app-settings")
//                    waitingForAppSettings = true
//                    showFallbackDialog    = true
//                    context.startActivity(
//                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                            data = Uri.parse("package:${context.packageName}")
//                        }
//                    )
//                } else {
//                    Log.d(TAG, "Continue tapped → single batch (priority first): $perms")
//                    appendPermsEverAsked(context, perms)
//                    permissionLauncher.launch(perms.toTypedArray())
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
//    // FLICKER FIX 2:
//    // Don't render an empty shell — caller should be dismissing via the
//    // SideEffect above. Returning here prevents the bare Card (title + Continue
//    // button with no card rows) from briefly appearing on screen during
//    // navigation transitions or rapid state changes.
//    if (visibleCards.isEmpty()) return
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
//
//
//

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

private fun readPermsEverAsked(context: android.content.Context): Set<String> =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getStringSet(KEY_PERMS_EVER_ASKED, emptySet()) ?: emptySet()

private fun appendPermsEverAsked(context: android.content.Context, perms: Collection<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val current = prefs.getStringSet(KEY_PERMS_EVER_ASKED, emptySet())?.toMutableSet() ?: mutableSetOf()
    current.addAll(perms)
    prefs.edit().putStringSet(KEY_PERMS_EVER_ASKED, current).apply()
}

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

    // FLICKER FIX 1:
    // Synchronous early-return: don't render ANYTHING if perms already granted.
    // Using LaunchedEffect(Unit) { onDismiss() } caused a 1-frame flicker of the
    // empty card because LaunchedEffect schedules its body for AFTER the first
    // frame is laid out. `remember` + `SideEffect` + an early `return` ensures
    // the Dialog composable subtree is never entered at all.
    val alreadyGrantedAtStart = remember { allPermissionsGranted(context, priorityPerms) }
    if (alreadyGrantedAtStart) {
        Log.d(TAG, "=== First composition: all permissions granted → immediate dismiss (no render) ===")
        SideEffect { onDismiss() }
        return
    }

    var overlaySettingsOpened by remember { mutableStateOf(false) }
    var pollingOverlay        by remember { mutableStateOf(false) }
    var showFallbackDialog    by remember { mutableStateOf(false) }
    var waitingForAppSettings by remember { mutableStateOf(false) }

    var dismissAfterOverlayReturn by remember { mutableStateOf(false) }

    // NEW: tracks whether we've already done the one-shot "retry denied perms"
    // pass for this dialog session. Prevents an infinite re-prompt loop if the
    // user keeps denying.
    var retryAttempted by remember { mutableStateOf(false) }

    var permissionCheckTrigger by remember { mutableStateOf(0) }

    var overlayGranted by remember { mutableStateOf(isOverlayGranted(context)) }

    Log.d(TAG, "=== Composition ===  overlayGranted=$overlayGranted  " +
            "showFallback=$showFallbackDialog  waitingForAppSettings=$waitingForAppSettings  " +
            "pollingOverlay=$pollingOverlay  dismissAfterOverlayReturn=$dismissAfterOverlayReturn  " +
            "priorityPerms=$priorityPerms  " +
            "missingRuntime=${getMissingRuntimePermissions(context, priorityPerms)}")

    fun handlePostRequestState() {
        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
        val nowOverlay        = isOverlayGranted(context)
        val runtimeGranted    = nowMissingRuntime.isEmpty()
        overlayGranted        = nowOverlay
        permissionCheckTrigger++

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
                Log.d(TAG, "  → runtime denied (overlay=$overlayGranted) → fallback dialog")
                showFallbackDialog = true
            }
            else -> {
                Log.d(TAG, "  → unreachable branch reached, dismissing as a safety")
                onDismiss()
            }
        }
    }

    // ── Runtime permission launcher ───────────────────────────────────────
    // Held in a mutable ref so the callback below can re-invoke the launcher
    // for the retry pass. (You can't reference `permissionLauncher` from
    // inside the lambda that initializes it.)
    val launcherRef = remember {
        mutableStateOf<androidx.activity.result.ActivityResultLauncher<Array<String>>?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "── Runtime launcher callback ──")
        Log.d(TAG, "  raw results : $results")

        permissionCheckTrigger++
        val nowMissingRuntime = getMissingRuntimePermissions(context, priorityPerms)
        val nowOverlay        = isOverlayGranted(context)
        overlayGranted        = nowOverlay
        val runtimeGranted    = nowMissingRuntime.isEmpty()

        when {
            runtimeGranted && overlayGranted -> {
                Log.d(TAG, "  launcher → ALL granted, dismissing ✓")
                onDismiss()
            }
            runtimeGranted && !overlayGranted -> {
                Log.d(TAG, "  launcher → runtime OK, overlay missing → opening overlay settings")
                suppressAppOpenAd(context)
                overlaySettingsOpened = true
            }
            else -> {
                // Before falling back to app-settings, give denied perms one more
                // chance through the system dialog. Only re-prompt perms for which
                // the OS will still show a dialog (shouldShowRequestPermissionRationale
                // == true). Permanently-denied perms wouldn't show anything, so
                // they skip the retry and go straight to app-settings.
                val activity = context as? android.app.Activity
                val retriablePerms = if (activity != null) {
                    nowMissingRuntime.filter { perm ->
                        androidx.core.app.ActivityCompat
                            .shouldShowRequestPermissionRationale(activity, perm)
                    }
                } else {
                    emptyList()
                }

                val retryLauncher = launcherRef.value
                if (!retryAttempted && retriablePerms.isNotEmpty() && retryLauncher != null) {
                    Log.d(TAG, "  launcher → ${nowMissingRuntime.size} denied, retrying ${retriablePerms.size}: $retriablePerms")
                    retryAttempted = true
                    appendPermsEverAsked(context, retriablePerms)
                    retryLauncher.launch(retriablePerms.toTypedArray())
                } else {
                    Log.d(TAG, "  launcher → runtime denied (retryAttempted=$retryAttempted, retriable=$retriablePerms) → opening app-settings  missingRuntime=$nowMissingRuntime")
                    waitingForAppSettings = true
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            }
        }
    }

    // Publish the launcher into the ref so the callback can use it for retries.
    SideEffect { launcherRef.value = permissionLauncher }

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

                // NEW: if we're in the “dismiss-after-overlay-return” mode,
                // close the dialog regardless of runtime state.
                if (dismissAfterOverlayReturn) {
                    Log.d(TAG, "  → dismissAfterOverlayReturn=true (overlay granted) → dismissing ✓")
                    suppressAppOpenAd(context)
                    onDismiss()
                    break
                }

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
                    "waitingForAppSettings=$waitingForAppSettings  " +
                    "dismissAfterOverlayReturn=$dismissAfterOverlayReturn")

            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver

            permissionCheckTrigger++

            if (pollingOverlay) {
                val stillDenied = !isOverlayGranted(context)
                Log.d(TAG, "  [overlay path] stillDenied=$stillDenied")
                if (stillDenied) {
                    pollingOverlay = false
                    overlayGranted = false

                    // NEW: if the user came here via the "post app-settings overlay" flow,
                    // dismiss now regardless of overlay outcome (denied case).
                    if (dismissAfterOverlayReturn) {
                        Log.d(TAG, "  → dismissAfterOverlayReturn=true (overlay denied on return) → dismissing ✓")
                        suppressAppOpenAd(context)
                        onDismiss()
                        return@LifecycleEventObserver
                    }

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

                when {
                    // Everything granted → done.
                    runtimeGranted && overlayGranted -> {
                        Log.d(TAG, "  → ALL granted → suppressing app-open ad and dismissing ✓")
                        suppressAppOpenAd(context)
                        onDismiss()
                    }

                    runtimeGranted && !overlayGranted -> {
                        Log.d(TAG, "  → runtime granted, overlay missing → showing overlay card, arming dismiss-after-overlay-return")
                        showFallbackDialog          = false
                        dismissAfterOverlayReturn   = true
                    }
                    else -> {
                        Log.d(TAG, "  → runtime still missing after app settings → keep fallback")
                    }
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

        if (!overlayGranted) {
            items.add(PermissionItem(Icons.Filled.Layers, "Overlay", isGranted = false))
        }

        items
    }

    // FLICKER FIX 3:
    // If `visibleCards` is empty AND all perms are granted, dismiss synchronously
    // (SideEffect) rather than via LaunchedEffect, which would let an empty card
    // render for a frame first.
    if (visibleCards.isEmpty()) {
        SideEffect {
            if (allPermissionsGranted(context, priorityPerms)) {
                Log.d(TAG, "All cards hidden + perms granted → dismissing ✓")
                onDismiss()
            }
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
                // If the only thing missing is overlay (typical after the
                // app-settings round-trip granted all runtime perms), arm the
                // dismiss-after-overlay-return flag so that whether the user
                // grants or denies overlay, the dialog closes when they come back.
                val runtimeStillMissing = getMissingRuntimePermissions(context, priorityPerms)
                val overlayStillMissing = !isOverlayGranted(context)

                if (runtimeStillMissing.isEmpty() && overlayStillMissing) {
                    Log.d(TAG, "Continue tapped → only overlay missing → arming dismissAfterOverlayReturn and opening overlay settings")
                    dismissAfterOverlayReturn = true
                    suppressAppOpenAd(context)
                    overlaySettingsOpened = true
                    return@PermissionsDialogContent
                }

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
    // FLICKER FIX 2:
    // Don't render an empty shell — caller should be dismissing via the
    // SideEffect above. Returning here prevents the bare Card (title + Continue
    // button with no card rows) from briefly appearing on screen during
    // navigation transitions or rapid state changes.
    if (visibleCards.isEmpty()) return

    Dialog(
        // Back press is wired to onDismiss so the user can simply close the
        // dialog with the system/back gesture. Outside taps are still ignored
        // so an accidental fumble outside the card won't close it.
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress    = true,
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