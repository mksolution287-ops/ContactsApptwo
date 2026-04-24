package com.callerinfo.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callerinfo.R
import com.callerinfo.data.viewmodel.ContactsViewModel
import com.callerinfo.ui.components.ContactAvatar
import com.callerinfo.ui.components.NativeAdCard
import com.callerinfo.utils.IntentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    viewModel: ContactsViewModel,
    onBack: () -> Unit,
    onCallHistory: (contactId: Long) -> Unit
) {
    val context = LocalContext.current

    // Observe contact live so favourite toggle reflects immediately
    val contact by viewModel.getContactByIdAsFlow(contactId).collectAsState(initial = null)

    if (contact == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_contacts_found))
        }
        return
    }

    val c = contact!!   // safe after null check above

    var visible          by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val alpha       by animateFloatAsState(if (visible) 1f else 0f,  tween(400), label = "a")
    val avatarScale by animateFloatAsState(if (visible) 1f else 0.7f, tween(500), label = "s")

    // ── Delete confirmation dialog ─────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text(stringResource(R.string.delete_contact)) },
            text    = { Text(stringResource(R.string.delete_contact_confirmation,c.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    IntentUtils.deleteContactDirect(context, c.id)
                    onBack()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.dialog_cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.contacts_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back_button_description))
                    }
                },
                actions = {
                    TextButton(onClick = { IntentUtils.editContact(context, c.id) }) {
                        Text(
                            text  = stringResource(R.string.action_edit),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors   = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier       = Modifier
                    .weight(1f)
                    .alpha(alpha)
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 40.dp)
            ) {

                // ── Avatar + name + primary phone ──────────────────────────────
                item {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ContactAvatar(
                            name     = c.displayName,
                            photoUri = c.photoUri,
                            size     = 96.dp,
                            fontSize = 36.sp,
                            modifier = Modifier.scale(avatarScale)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text      = c.displayName,
                            style     = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color     = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 24.dp)
                        )
                        c.primaryPhone?.let { phone ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text  = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── 4 action buttons ───────────────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        // Call
                        ActionButton(
                            icon    = Icons.Outlined.Call,
                            label   = stringResource(R.string.action_call),
                            enabled = c.primaryPhone != null,
                            onClick = {
                                c.primaryPhone?.let {
                                    IntentUtils.makeCall(context, it, context.getString(R.string.error_no_phone_app))
                                }
                            }
                        )
                        // Share
                        ActionButton(
                            icon    = Icons.Outlined.Share,
                            label   = stringResource(R.string.action_share),
                            onClick = { IntentUtils.shareContact(context, c.id) }
                        )
                        // Message
                        ActionButton(
                            icon    = Icons.Outlined.ChatBubbleOutline,
                            label   = stringResource(R.string.action_message),
                            enabled = c.primaryPhone != null,
                            onClick = {
                                c.primaryPhone?.let {
                                    IntentUtils.sendSms(context, it, context.getString(R.string.error_no_messaging_app))
                                }
                            }
                        )
                        // Favourite toggle
                        ActionButton(
                            icon    = if (c.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            label   = "Favourite",
                            tint    = if (c.isFavorite) Color(0xFFFFD700) else null,
                            onClick = { viewModel.toggleFavorite(c.id) }
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // ── Phone section ──────────────────────────────────────────────
                if (c.phoneNumbers.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(R.string.phone_home))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            shape  = RoundedCornerShape(12.dp),
                            color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column {
                                c.phoneNumbers.forEachIndexed { i, phone ->
                                    if (i > 0) HorizontalDivider(
                                        modifier  = Modifier.padding(horizontal = 16.dp),
                                        color     = MaterialTheme.colorScheme.outlineVariant,
                                        thickness = 0.5.dp
                                    )
                                    PhoneRow(
                                        number   = phone.number,
                                        label    = phoneTypeLabel(phone.type),
                                        onCall   = {
                                            IntentUtils.makeCall(context, phone.number, context.getString(R.string.error_no_phone_app))
                                        },
                                        onSms    = {
                                            IntentUtils.sendSms(context, phone.number, context.getString(R.string.error_no_messaging_app))
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── Delete + Call History row ──────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Delete button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDeleteDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3A1A1A)
                        ) {
                            Row(
                                modifier            = Modifier.padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment   = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector        = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint               = Color(0xFFE57373),
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text       = stringResource(R.string.delete),
                                    color      = Color(0xFFE57373),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 15.sp
                                )
                            }
                        }

                        // Call History button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
//                                c.primaryPhone?.let {
//                                    IntentUtils.openCallHistory(context, it)
//                                }
                                    onCallHistory(contactId)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier              = Modifier.padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector        = Icons.Outlined.History,
                                    contentDescription = "Call History",
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text       = stringResource(R.string.call_history),
                                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 15.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                item { Spacer(Modifier.navigationBarsPadding()) }
            }
            NativeAdCard(modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp))
        }

    }
}

// ---------------------------------------------------------------------------
// Action button (Call / Share / Message / Favourite)
// ---------------------------------------------------------------------------

@Composable
private fun ActionButton(
    icon    : ImageVector,
    label   : String,
    enabled : Boolean = true,
    tint    : Color?  = null,
    onClick : () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.alpha(alpha)
    ) {
        Surface(
            shape    = RoundedCornerShape(14.dp),
            color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector        = icon,
                    contentDescription = label,
                    tint               = tint ?: MaterialTheme.colorScheme.onSurface,
                    modifier           = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color    = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp, top = 4.dp)
    )
}

// ---------------------------------------------------------------------------
// Phone row with message + call icons on the right
// ---------------------------------------------------------------------------

@Composable
private fun PhoneRow(
    number  : String,
    label   : String,
    onCall  : () -> Unit,
    onSms   : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = number,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Message icon
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onSms)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "SMS",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        // Call icon
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onCall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Outlined.Call,
                contentDescription = "Call",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun phoneTypeLabel(type: com.callerinfo.data.model.PhoneType): String = when (type) {
    com.callerinfo.data.model.PhoneType.MOBILE -> stringResource(R.string.phone_mobile)
    com.callerinfo.data.model.PhoneType.HOME   -> stringResource(R.string.phone_home)
    com.callerinfo.data.model.PhoneType.WORK   -> stringResource(R.string.phone_work)
    com.callerinfo.data.model.PhoneType.OTHER  -> stringResource(R.string.phone_other)
}