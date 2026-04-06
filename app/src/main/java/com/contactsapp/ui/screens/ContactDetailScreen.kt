package com.contactsapp.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contactsapp.R
import com.contactsapp.data.viewmodel.ContactsViewModel
import com.contactsapp.ui.components.ContactAvatar
import com.contactsapp.utils.AvatarColorUtils
import com.contactsapp.utils.IntentUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    viewModel: ContactsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val contact = viewModel.getContactById(contactId)

    if (contact == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_contacts_found))
        }
        return
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f, tween(400), label = "a")
    val avatarScale by animateFloatAsState(targetValue = if (visible) 1f else 0.7f, tween(500), label = "s")

    val avatarBg = AvatarColorUtils.getColor(contact.displayName)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back_button_description))
                    }
                },
                actions = {
                    IconButton(onClick = { IntentUtils.editContact(context, contact.id) }) {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.action_edit))
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options_description))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            onClick = {
                                menuExpanded = false
                                IntentUtils.shareContact(context, contact.id)
                            },
                            leadingIcon = { Icon(Icons.Outlined.Share, null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Hero header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    avatarBg.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(top = 8.dp, bottom = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ContactAvatar(
                            name = contact.displayName,
                            photoUri = contact.photoUri,
                            size = 100.dp,
                            fontSize = 36.sp,
                            modifier = Modifier.scale(avatarScale)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = contact.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        contact.company?.let {
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Business,
                                    null,
                                    Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Quick action chips
            item {
                val primaryPhone = contact.primaryPhone
                val primaryEmail = contact.emails.firstOrNull()?.address

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    if (primaryPhone != null) {
                        QuickActionButton(
                            icon = Icons.Outlined.Call,
                            label = stringResource(R.string.action_call),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                IntentUtils.dialNumber(
                                    context, primaryPhone,
                                    context.getString(R.string.error_no_phone_app)
                                )
                            }
                        )
                        QuickActionButton(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            label = stringResource(R.string.action_message),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                IntentUtils.sendSms(
                                    context, primaryPhone,
                                    context.getString(R.string.error_no_messaging_app)
                                )
                            }
                        )
                        QuickActionButton(
                            icon = Icons.Outlined.Videocam,
                            label = stringResource(R.string.action_video),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = { IntentUtils.videoCall(context, primaryPhone) }
                        )
                    }
                    if (primaryEmail != null) {
                        QuickActionButton(
                            icon = Icons.Outlined.Email,
                            label = stringResource(R.string.action_email),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                IntentUtils.sendEmail(
                                    context, primaryEmail,
                                    context.getString(R.string.error_no_email_app)
                                )
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Phone numbers
            if (contact.phoneNumbers.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.section_phone)) {
                        contact.phoneNumbers.forEachIndexed { i, phone ->
                            if (i > 0) HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                            ContactInfoRow(
                                icon = Icons.Outlined.Phone,
                                label = phoneTypeLabel(phone.type),
                                value = phone.number,
                                onClick = {
                                    IntentUtils.dialNumber(
                                        context, phone.number,
                                        context.getString(R.string.error_no_phone_app)
                                    )
                                },
                                trailingAction = {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                IntentUtils.sendSms(
                                                    context, phone.number,
                                                    context.getString(R.string.error_no_messaging_app)
                                                )
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Outlined.ChatBubbleOutline,
                                                stringResource(R.string.action_message),
                                                Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Email addresses
            if (contact.emails.isNotEmpty()) {
                item {
                    SectionCard(title = stringResource(R.string.section_email)) {
                        contact.emails.forEachIndexed { i, email ->
                            if (i > 0) HorizontalDivider(
                                Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp
                            )
                            ContactInfoRow(
                                icon = Icons.Outlined.Email,
                                label = emailTypeLabel(email.type),
                                value = email.address,
                                onClick = {
                                    IntentUtils.sendEmail(
                                        context, email.address,
                                        context.getString(R.string.error_no_email_app)
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Notes
            contact.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                item {
                    SectionCard(title = stringResource(R.string.section_notes)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Notes,
                                null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            item { Spacer(Modifier.navigationBarsPadding()) }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = containerColor,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        trailingAction?.invoke()
    }
}

@Composable
private fun phoneTypeLabel(type: com.contactsapp.data.model.PhoneType): String = when (type) {
    com.contactsapp.data.model.PhoneType.MOBILE -> stringResource(R.string.phone_mobile)
    com.contactsapp.data.model.PhoneType.HOME -> stringResource(R.string.phone_home)
    com.contactsapp.data.model.PhoneType.WORK -> stringResource(R.string.phone_work)
    com.contactsapp.data.model.PhoneType.OTHER -> stringResource(R.string.phone_other)
}

@Composable
private fun emailTypeLabel(type: com.contactsapp.data.model.EmailType): String = when (type) {
    com.contactsapp.data.model.EmailType.HOME -> stringResource(R.string.phone_home)
    com.contactsapp.data.model.EmailType.WORK -> stringResource(R.string.phone_work)
    com.contactsapp.data.model.EmailType.OTHER -> stringResource(R.string.phone_other)
}
