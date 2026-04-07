package com.contactsapptwomktech.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CallMade
import androidx.compose.material.icons.outlined.CallMissed
import androidx.compose.material.icons.outlined.CallReceived
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Voicemail
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.contactsapptwomktech.R
import com.contactsapptwomktech.data.model.CallLogEntry
import com.contactsapptwomktech.data.model.CallType
import com.contactsapptwomktech.data.viewmodel.ContactsViewModel
import com.contactsapptwomktech.data.viewmodel.UiState
import com.contactsapptwomktech.ui.components.ContactAvatar
import com.contactsapptwomktech.ui.components.EmptyState
import com.contactsapptwomktech.ui.components.PermissionScreen
import com.contactsapptwomktech.ui.components.SwipeableContactRow
import com.contactsapptwomktech.ui.theme.IncomingGreen
import com.contactsapptwomktech.ui.theme.MissedCallRed
import com.contactsapptwomktech.ui.theme.OutgoingBlue
import com.contactsapptwomktech.utils.FormatUtils
import com.contactsapptwomktech.utils.IntentUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// ── Filter chip definitions ────────────────────────────────────────────────

private data class CallFilterChip(
    val type  : CallType?,   // null = "All"
    val label : String,
    val icon  : ImageVector
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    viewModel       : ContactsViewModel,
    onFavoritesClick: () -> Unit,
    onSettingsClick : () -> Unit
) {
    val context = LocalContext.current

    val permissionState = rememberPermissionState(android.Manifest.permission.READ_CALL_LOG) { granted ->
        if (granted) viewModel.loadCallLog()
    }
    val searchQuery  by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    // Active call-type filter — null means "All"
    var activeFilter by rememberSaveable { mutableStateOf<CallType?>(null) }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) viewModel.loadCallLog()
    }
    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) viewModel.setSearchQuery("")
    }

    if (!permissionState.status.isGranted) {
        PermissionScreen(
            icon      = Icons.Outlined.History,
            message   = stringResource(R.string.call_log_permission_message),
            onGrantClicked = { permissionState.launchPermissionRequest() }
        )
        return
    }

    val state          by viewModel.callLogState.collectAsState()
    val todayLabel     = stringResource(R.string.today)
    val yesterdayLabel = stringResource(R.string.yesterday)

    // Build chip list with localised labels
    val filterChips = listOf(
        CallFilterChip(null,               "All",      Icons.Outlined.Call),
        CallFilterChip(CallType.MISSED,    "Missed",   Icons.Outlined.CallMissed),
        CallFilterChip(CallType.INCOMING,  "Incoming", Icons.Outlined.CallReceived),
        CallFilterChip(CallType.OUTGOING,  "Outgoing", Icons.Outlined.CallMade),
        CallFilterChip(CallType.REJECTED,  "Rejected", Icons.Outlined.PhoneDisabled),
    )

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value         = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder   = {
                                    Text(
                                        stringResource(R.string.search_contacts_hint),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                modifier       = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine     = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors         = TextFieldDefaults.colors(
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedContainerColor   = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge
                            )
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            Text(
                                stringResource(R.string.recents_title),
                                style      = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        if (isSearchActive) {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.setSearchQuery("")
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Outlined.Close, stringResource(R.string.cd_search_clear))
                            }
                        } else {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Outlined.Search, stringResource(R.string.search_button_description))
                            }
                            IconButton(onClick = onFavoritesClick) {
                                Icon(
                                    imageVector        = Icons.Outlined.StarBorder,
                                    contentDescription = stringResource(R.string.nav_favorites),
                                    tint               = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector        = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // ── Filter chips row ───────────────────────────────────────
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    filterChips.forEach { chip ->
                        val selected = activeFilter == chip.type
                        FilterChip(
                            selected  = selected,
                            onClick   = {
                                // Tapping the already-active filter deselects → "All"
                                activeFilter = if (selected && chip.type != null) null else chip.type
                            },
                            label     = { Text(chip.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector        = chip.icon,
                                    contentDescription = null,
                                    modifier           = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (chip.type) {
                                    CallType.MISSED, CallType.REJECTED ->
                                        MissedCallRed.copy(alpha = 0.18f)
                                    CallType.INCOMING ->
                                        IncomingGreen.copy(alpha = 0.18f)
                                    CallType.OUTGOING ->
                                        OutgoingBlue.copy(alpha = 0.18f)
                                    else ->
                                        MaterialTheme.colorScheme.primaryContainer
                                },
                                selectedLabelColor = when (chip.type) {
                                    CallType.MISSED, CallType.REJECTED -> MissedCallRed
                                    CallType.INCOMING                  -> IncomingGreen
                                    CallType.OUTGOING                  -> OutgoingBlue
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                selectedLeadingIconColor = when (chip.type) {
                                    CallType.MISSED, CallType.REJECTED -> MissedCallRed
                                    CallType.INCOMING                  -> IncomingGreen
                                    CallType.OUTGOING                  -> OutgoingBlue
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        )
                    }
                }

                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
            }
        }
    ) { paddingValues ->
        when (val s = state) {
            is UiState.Loading -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is UiState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.error_loading_call_log),
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Success -> {
                // 1. Apply text search
                val afterSearch = s.data.let { all ->
                    if (searchQuery.isBlank()) all
                    else all.filter { entry ->
                        entry.displayName.contains(searchQuery, ignoreCase = true) ||
                                entry.number.contains(searchQuery, ignoreCase = true)
                    }
                }
                // 2. Apply call-type filter chip
                val logs = if (activeFilter == null) afterSearch
                else afterSearch.filter { it.callType == activeFilter }

                if (logs.isEmpty()) {
                    EmptyState(
                        icon     = Icons.Outlined.History,
                        title    = if (searchQuery.isBlank() && activeFilter == null)
                            stringResource(R.string.no_recent_calls)
                        else
                            stringResource(R.string.no_contacts_found),
                        subtitle = if (searchQuery.isBlank() && activeFilter == null)
                            stringResource(R.string.no_recents_message)
                        else
                            stringResource(R.string.no_contacts_found),
                        modifier = Modifier.padding(paddingValues)
                    )
                } else {
                    val grouped = LinkedHashMap<String, MutableList<CallLogEntry>>()
                    logs.forEach { entry ->
                        val label = FormatUtils.getDateLabel(entry.date, todayLabel, yesterdayLabel)
                        grouped.getOrPut(label) { mutableListOf() }.add(entry)
                    }

                    LazyColumn(
                        modifier       = Modifier.padding(paddingValues),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        grouped.forEach { (dateLabel, entries) ->
                            item {
                                Text(
                                    text       = dateLabel,
                                    style      = MaterialTheme.typography.labelMedium,
                                    color      = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(
                                        start  = 20.dp,
                                        end    = 20.dp,
                                        top    = 16.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }
                            itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter   = fadeIn(tween(200 + index * 30)) +
                                            slideInVertically(tween(200 + index * 30))
                                ) {
                                    SwipeableContactRow(
                                        onCall = {
                                            IntentUtils.makeCall(
                                                context,
                                                entry.number,
                                                context.getString(R.string.error_no_phone_app)
                                            )
                                        },
                                        onMessage = {
                                            IntentUtils.sendSms(context, entry.number, errorMsg = context.getString(R.string.error_no_messaging_app))
                                        }
                                    ) {
                                        CallLogItem(
                                            entry       = entry,
                                            onCallClick = {
                                                IntentUtils.makeCall(
                                                    context,
                                                    entry.number,
                                                    context.getString(R.string.error_no_phone_app)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Call log item row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CallLogItem(entry: CallLogEntry, onCallClick: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCallClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = entry.displayName, photoUri = null, size = 46.dp)
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = entry.displayName,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = if (entry.callType == CallType.MISSED) MissedCallRed
                else MaterialTheme.colorScheme.onSurface
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector        = callTypeIcon(entry.callType),
                    contentDescription = stringResource(
                        R.string.call_type_icon_description,
                        callTypeLabel(entry.callType)
                    ),
                    modifier = Modifier.size(14.dp),
                    tint     = callTypeColor(entry.callType)
                )
                Text(
                    text  = buildCallSubtitle(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = FormatUtils.formatCallDate(entry.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onCallClick)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Phone,
                        contentDescription = stringResource(R.string.action_call_back),
                        modifier = Modifier.size(16.dp),
                        tint     = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun callTypeLabel(type: CallType): String = when (type) {
    CallType.INCOMING  -> stringResource(R.string.call_type_incoming)
    CallType.OUTGOING  -> stringResource(R.string.call_type_outgoing)
    CallType.MISSED    -> stringResource(R.string.call_type_missed)
    CallType.REJECTED  -> stringResource(R.string.call_type_rejected)
    CallType.VOICEMAIL -> stringResource(R.string.call_type_voicemail)
    CallType.UNKNOWN   -> stringResource(R.string.call_type_unknown)
}

@Composable
private fun buildCallSubtitle(entry: CallLogEntry): String {
    val typeLabel = callTypeLabel(entry.callType)
    return if (entry.duration > 0)
        "$typeLabel · ${FormatUtils.formatDuration(entry.duration)}"
    else
        typeLabel
}

private fun callTypeIcon(type: CallType): ImageVector = when (type) {
    CallType.INCOMING  -> Icons.Outlined.CallReceived
    CallType.OUTGOING  -> Icons.Outlined.CallMade
    CallType.MISSED    -> Icons.Outlined.CallMissed
    CallType.REJECTED  -> Icons.Outlined.PhoneDisabled
    CallType.VOICEMAIL -> Icons.Outlined.Voicemail
    CallType.UNKNOWN   -> Icons.Outlined.Call
}

@Composable
private fun callTypeColor(type: CallType): Color = when (type) {
    CallType.INCOMING                  -> IncomingGreen
    CallType.OUTGOING                  -> OutgoingBlue
    CallType.MISSED, CallType.REJECTED -> MissedCallRed
    else                               -> MaterialTheme.colorScheme.onSurfaceVariant
}