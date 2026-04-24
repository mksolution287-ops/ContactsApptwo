package com.callerinfocom.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.callerinfocom.R
data class PermissionItem(
    val icon : ImageVector,
    val label: String
)

@Composable
fun RequiredPermissionsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Track whether we should open overlay settings after runtime perms
    var openOverlaySettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // After runtime permissions, request overlay if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !Settings.canDrawOverlays(context)
        ) {
            openOverlaySettings = true
        } else {
            onDismiss()
        }
    }

    // Open overlay settings screen
    LaunchedEffect(openOverlaySettings) {
        if (openOverlaySettings) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            openOverlaySettings = false
            onDismiss()
        }
    }

    val permissionItems = listOf(
        PermissionItem(Icons.Filled.Call,   "Call"),
        PermissionItem(Icons.Filled.Person, "Contacts"),
        PermissionItem(Icons.Filled.Layers, "Overlay")
    )

    Dialog(
        onDismissRequest = { /* not dismissible by back/outside tap */ },
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

                // Title
                Text(
                    text       = stringResource(R.string.permission_required),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text  = stringResource(R.string.permission_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Permission rows
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
                            text      = item.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize  = 16.sp,
                            color     = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Safety note banner
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
                        imageVector        = Icons.Filled.Person, // replace with Shield icon if available
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

                // Continue button
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
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
                            permissionLauncher.launch(perms.toTypedArray())
                        }
                    ) {
                        Text(
                            text      = stringResource(R.string.continue_btn),
                            color     = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize  = 16.sp
                        )
                    }
                }
            }
        }
    }
}