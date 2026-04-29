//package com.callerinfocom.ui.screens
//
//import android.Manifest
//import android.content.Intent
//import android.net.Uri
//import android.os.Build
//import android.provider.Settings
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
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.window.Dialog
//import androidx.compose.ui.window.DialogProperties
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Call
//import androidx.compose.material.icons.filled.Person
//import androidx.compose.material.icons.filled.Layers
//import androidx.compose.ui.res.stringResource
//import com.callerinfocom.R
//data class PermissionItem(
//    val icon : ImageVector,
//    val label: String
//)
//
//@Composable
//fun RequiredPermissionsDialog(
//    onDismiss: () -> Unit
//) {
//    val context = LocalContext.current
//
//    // Track whether we should open overlay settings after runtime perms
//    var openOverlaySettings by remember { mutableStateOf(false) }
//
//    val permissionLauncher = rememberLauncherForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { _ ->
//        // After runtime permissions, request overlay if needed
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
//            !Settings.canDrawOverlays(context)
//        ) {
//            openOverlaySettings = true
//        } else {
//            onDismiss()
//        }
//    }
//
//    // Open overlay settings screen
//    LaunchedEffect(openOverlaySettings) {
//        if (openOverlaySettings) {
//            val intent = Intent(
//                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                Uri.parse("package:${context.packageName}")
//            )
//            context.startActivity(intent)
//            openOverlaySettings = false
//            onDismiss()
//        }
//    }
//
//    val permissionItems = listOf(
//        PermissionItem(Icons.Filled.Call,   "Call"),
//        PermissionItem(Icons.Filled.Person, "Contacts"),
//        PermissionItem(Icons.Filled.Layers, "Overlay")
//    )
//
//    Dialog(
//        onDismissRequest = { /* not dismissible by back/outside tap */ },
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
//                // Title
//                Text(
//                    text       = stringResource(R.string.permission_required),
//                    fontWeight = FontWeight.Bold,
//                    fontSize   = 20.sp,
//                    color      = MaterialTheme.colorScheme.onSurface
//                )
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Subtitle
//                Text(
//                    text  = stringResource(R.string.permission_subtitle),
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//
//                Spacer(modifier = Modifier.height(20.dp))
//
//                // Permission rows
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
//                            text      = item.label,
//                            fontWeight = FontWeight.SemiBold,
//                            fontSize  = 16.sp,
//                            color     = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Safety note banner
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
//                        imageVector        = Icons.Filled.Person, // replace with Shield icon if available
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
//                // Continue button
//                Row(
//                    modifier          = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.End
//                ) {
//                    TextButton(
//                        onClick = {
//                            val perms = buildList {
//                                add(Manifest.permission.READ_CONTACTS)
//                                add(Manifest.permission.READ_CALL_LOG)
//                                add(Manifest.permission.WRITE_CONTACTS)
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                                    add(Manifest.permission.ANSWER_PHONE_CALLS)
//                                }
//                                add(Manifest.permission.READ_PHONE_STATE)
//                                add(Manifest.permission.CALL_PHONE)
//                            }
//                            permissionLauncher.launch(perms.toTypedArray())
//                        }
//                    ) {
//                        Text(
//                            text      = stringResource(R.string.continue_btn),
//                            color     = MaterialTheme.colorScheme.primary,
//                            fontWeight = FontWeight.Bold,
//                            fontSize  = 16.sp
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.callerinfocom.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "OverlayPermission"

data class PermissionItem(
    val icon: ImageVector,
    val label: String
)

@Composable
fun RequiredPermissionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var openOverlaySettings by remember { mutableStateOf(false) }
    var pollingOverlay by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        Log.d(TAG, "Runtime permission results: $results")

        val overlayAlreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                Settings.canDrawOverlays(context)

        Log.d(TAG, "SDK_INT=${Build.VERSION.SDK_INT}, " +
                "canDrawOverlays=$overlayAlreadyGranted")

        if (!overlayAlreadyGranted) {
            Log.d(TAG, "Overlay NOT granted → setting openOverlaySettings=true")
            openOverlaySettings = true
        } else {
            Log.d(TAG, "Overlay already granted → calling onDismiss()")
            onDismiss()
        }
    }

    // Step 1 – launch the overlay settings screen
    LaunchedEffect(openOverlaySettings) {
        Log.d(TAG, "LaunchedEffect(openOverlaySettings) fired → value=$openOverlaySettings")
        if (openOverlaySettings) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            Log.d(TAG, "Starting overlay settings intent for package=${context.packageName}")
            context.startActivity(intent)
            openOverlaySettings = false
            pollingOverlay = true
            Log.d(TAG, "pollingOverlay set to TRUE — poller will start")
        }
    }

    // Step 2 – poll every 500 ms
    LaunchedEffect(pollingOverlay) {
        Log.d(TAG, "LaunchedEffect(pollingOverlay) fired → value=$pollingOverlay")
        if (!pollingOverlay) {
            Log.d(TAG, "pollingOverlay=false, skipping poll loop")
            return@LaunchedEffect
        }

        Log.d(TAG, "Poll loop STARTED")
        var tickCount = 0
        while (isActive) {
            delay(500L)
            tickCount++

            val granted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Settings.canDrawOverlays(context)

            Log.d(TAG, "Poll tick #$tickCount → canDrawOverlays=$granted")

            if (granted) {
                Log.d(TAG, "Overlay GRANTED on tick #$tickCount → " +
                        "bringing app to foreground and dismissing")

                val bringBack = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                    ?.apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                        )
                    }

                if (bringBack != null) {
                    Log.d(TAG, "Launch intent found → starting activity to close Settings")
                    context.startActivity(bringBack)
                } else {
                    Log.w(TAG, "getLaunchIntentForPackage returned null — " +
                            "Settings screen may stay open")
                }

                pollingOverlay = false
                Log.d(TAG, "Calling onDismiss()")
                onDismiss()
                break
            }
        }
        Log.d(TAG, "Poll loop ENDED (isActive=$isActive)")
    }

    // Step 3 – lifecycle observer: stop polling if user comes back without granting
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            Log.d(TAG, "Lifecycle event=$event  pollingOverlay=$pollingOverlay")
            if (event == Lifecycle.Event.ON_RESUME && pollingOverlay) {
                val stillDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !Settings.canDrawOverlays(context)
                Log.d(TAG, "ON_RESUME: overlay still denied=$stillDenied")
                if (stillDenied) {
                    Log.d(TAG, "User returned WITHOUT granting → stopping poller")
                    pollingOverlay = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            Log.d(TAG, "DisposableEffect disposed → removing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ── UI (unchanged) ────────────────────────────────────────────────────

    val permissionItems = listOf(
        PermissionItem(Icons.Filled.Call,   "Call"),
        PermissionItem(Icons.Filled.Person, "Contacts"),
        PermissionItem(Icons.Filled.Layers, "Overlay")
    )

    Dialog(
        onDismissRequest = { /* not dismissible */ },
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
                    TextButton(
                        onClick = {
                            Log.d(TAG, "Continue button clicked → launching runtime permissions")
                            val perms = buildList {
                                add(Manifest.permission.READ_CONTACTS)
                                add(Manifest.permission.READ_CALL_LOG)
                                add(Manifest.permission.WRITE_CONTACTS)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    add(Manifest.permission.ANSWER_PHONE_CALLS)
                                }
                                add(Manifest.permission.READ_PHONE_STATE)
                                add(Manifest.permission.CALL_PHONE)
                            }
                            Log.d(TAG, "Requesting permissions: $perms")
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    ) {
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