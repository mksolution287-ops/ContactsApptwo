package com.callerinfocom.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.SwipeLeft
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.callerinfocom.R
import com.callerinfocom.data.model.CallLogEntry
import com.callerinfocom.data.model.CallType
import com.callerinfocom.data.viewmodel.ContactsViewModel
import com.callerinfocom.data.viewmodel.UiState
import com.callerinfocom.ui.components.ContactAvatar
import com.callerinfocom.ui.components.EmptyState
import com.callerinfocom.ui.components.PermissionScreen
import com.callerinfocom.ui.components.SwipeableContactRow
import com.callerinfocom.ui.theme.IncomingGreen
import com.callerinfocom.ui.theme.MissedCallRed
import com.callerinfocom.ui.theme.OutgoingBlue
import com.callerinfocom.utils.FormatUtils
import com.callerinfocom.utils.IntentUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay

// ── Filter chip definitions ────────────────────────────────────────────────

private data class CallFilterChip(
    val type  : CallType?,
    val label : String,
    val icon  : ImageVector
)

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    viewModel       : ContactsViewModel,
    onFavoritesClick: () -> Unit,
    onSettingsClick : () -> Unit,
    onOpenContactHistory: (String) -> Unit
) {
    val context = LocalContext.current

    // ── Build the permission list ──────────────────────────────────────────
    val permissions = buildList {
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.WRITE_CONTACTS)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.CALL_PHONE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
    }

    val permissionsState = rememberMultiplePermissionsState(permissions) { results ->
        if (results[Manifest.permission.READ_CALL_LOG] == true) {
            viewModel.loadCallLog()
        }
    }

    val callLogGranted = permissionsState.permissions
        .firstOrNull { it.permission == Manifest.permission.READ_CALL_LOG }
        ?.status?.isGranted == true

    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var showPermissionDialog by remember {
        mutableStateOf(!prefs.getBoolean("permissions_asked", false))
    }
    var showSwipeHint by remember {
        mutableStateOf(!prefs.getBoolean("swipe_hint_shown", false))
    }

    if (showPermissionDialog) {
        RequiredPermissionsDialog(
            onDismiss = {
                prefs.edit().putBoolean("permissions_asked", true).apply()
                showPermissionDialog = false
            }
        )
    }

    val searchQuery    by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    val focusRequester  = remember { FocusRequester() }
    val focusManager    = LocalFocusManager.current

    var activeFilter by rememberSaveable { mutableStateOf<CallType?>(null) }
    // Tracks which entry's action row is currently expanded (null = none)
    var expandedEntryId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) viewModel.setSearchQuery("")
    }

    LaunchedEffect(callLogGranted) {
        if (callLogGranted) viewModel.loadCallLog()
    }

    if (!callLogGranted) {
        PermissionScreen(
            icon           = Icons.Outlined.History,
            message        = stringResource(R.string.call_log_permission_message),
            onGrantClicked = { permissionsState.launchMultiplePermissionRequest() }  // ← only change here
        )
        return
    }

    val state          by viewModel.callLogState.collectAsState()
    val todayLabel     = stringResource(R.string.today)
    val yesterdayLabel = stringResource(R.string.yesterday)

    val filterChips = listOf(
        CallFilterChip(null,              stringResource(R.string.filter_chip_all),      Icons.Outlined.Call),
        CallFilterChip(CallType.MISSED,   stringResource(R.string.filter_chip_missed),   Icons.Outlined.CallMissed),
        CallFilterChip(CallType.INCOMING, stringResource(R.string.filter_chip_incoming), Icons.Outlined.CallReceived),
        CallFilterChip(CallType.OUTGOING, stringResource(R.string.filter_chip_outgoing), Icons.Outlined.CallMade),
        CallFilterChip(CallType.REJECTED, stringResource(R.string.filter_chip_rejected), Icons.Outlined.PhoneDisabled),
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
                                modifier        = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester),
                                singleLine      = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                colors          = TextFieldDefaults.colors(
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
                            selected    = selected,
                            onClick     = {
                                activeFilter = if (selected && chip.type != null) null else chip.type
                            },
                            label       = { Text(chip.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector        = chip.icon,
                                    contentDescription = null,
                                    modifier           = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (chip.type) {
                                    CallType.MISSED, CallType.REJECTED -> MissedCallRed.copy(alpha = 0.18f)
                                    CallType.INCOMING                  -> IncomingGreen.copy(alpha = 0.18f)
                                    CallType.OUTGOING                  -> OutgoingBlue.copy(alpha = 0.18f)
                                    else                               -> MaterialTheme.colorScheme.primaryContainer
                                },
                                selectedLabelColor = when (chip.type) {
                                    CallType.MISSED, CallType.REJECTED -> MissedCallRed
                                    CallType.INCOMING                  -> IncomingGreen
                                    CallType.OUTGOING                  -> OutgoingBlue
                                    else                               -> MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                selectedLeadingIconColor = when (chip.type) {
                                    CallType.MISSED, CallType.REJECTED -> MissedCallRed
                                    CallType.INCOMING                  -> IncomingGreen
                                    CallType.OUTGOING                  -> OutgoingBlue
                                    else                               -> MaterialTheme.colorScheme.onPrimaryContainer
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
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is UiState.Error -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.error_loading_call_log),
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Success -> {
                val afterSearch = s.data.let { all ->
                    if (searchQuery.isBlank()) all
                    else all.filter { entry ->
                        entry.displayName.contains(searchQuery, ignoreCase = true) ||
                                entry.number.contains(searchQuery, ignoreCase = true)
                    }
                }
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
                        if (showSwipeHint && logs.isNotEmpty()) {
                            item(key = "swipe_hint") {
                                SwipeGestureHint(
                                    onDismissed = {
                                        prefs.edit().putBoolean("swipe_hint_shown", true).apply()
                                        showSwipeHint = false
                                    }
                                )
                            }
                        }

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
                                val isExpanded = expandedEntryId == entry.id
                                val isUnsaved  = entry.displayName == entry.number ||
                                        entry.displayName.isBlank()

                                AnimatedVisibility(
                                    visible = true,
                                    enter   = fadeIn(tween(200 + index * 30)) +
                                            slideInVertically(tween(200 + index * 30))
                                ) {
                                    Column {
                                        SwipeableContactRow(
                                            onCall = {
                                                IntentUtils.makeCall(
                                                    context,
                                                    entry.number,
                                                    context.getString(R.string.error_no_phone_app)
                                                )
                                            },
                                            onMessage = {
                                                IntentUtils.sendSms(
                                                    context,
                                                    entry.number,
                                                    errorMsg = context.getString(R.string.error_no_messaging_app)
                                                )
                                            }
                                        ) {
                                            CallLogItem(
                                                entry       = entry,
                                                isExpanded  = isExpanded,
                                                onRowClick  = {
                                                    // Toggle: tapping same row collapses it
                                                    expandedEntryId = if (isExpanded) null else entry.id
                                                },
                                                onCallClick = {
                                                    IntentUtils.makeCall(
                                                        context,
                                                        entry.number,
                                                        context.getString(R.string.error_no_phone_app)
                                                    )
                                                }
                                            )
                                        }

                                        // ── Inline expandable action row ───────────────
                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter   = expandVertically(
                                                animationSpec = tween(220),
                                                expandFrom    = Alignment.Top
                                            ) + fadeIn(tween(180)),
                                            exit    = shrinkVertically(
                                                animationSpec = tween(180),
                                                shrinkTowards = Alignment.Top
                                            ) + fadeOut(tween(140))
                                        ) {
                                            InlineActionRow(
                                                entry     = entry,
                                                isUnsaved = isUnsaved,
                                                context   = context,
                                                onDone    = { expandedEntryId = null },
                                                onHistoryClick = {onOpenContactHistory(entry.number)}
                                            )
                                        }

                                        HorizontalDivider(
                                            color     = MaterialTheme.colorScheme.outlineVariant,
                                            thickness = 0.4.dp,
                                            modifier  = Modifier.padding(horizontal = 20.dp)
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
// Inline action row — slides open directly below the tapped contact row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InlineActionRow(
    entry    : CallLogEntry,
    isUnsaved: Boolean,
    context  : Context,
    onDone   : () -> Unit,
    onHistoryClick: (String) -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        InlineActionButton(
            icon  = painterResource(R.drawable.baseline_message_24),
            label = "Message",
            tint  = MaterialTheme.colorScheme.primary
        ) {
            IntentUtils.sendSms(context, entry.number, errorMsg = "No messaging app found")
            onDone()
        }

        InlineActionButton(
            icon  = painterResource(R.drawable.ic_whatsapp),          // swap for a WhatsApp asset if you have one
            label = "WhatsApp",
            tint  = MaterialTheme.colorScheme.primary
        ) {
            openWhatsApp(context, entry.number)
            onDone()
        }

        InlineActionButton(
            icon  = painterResource(R.drawable.ic_history),
            label = "History",
            tint  = MaterialTheme.colorScheme.primary
        ) {
            onHistoryClick(entry.number)
            onDone()
        }

        // "Add" only appears for numbers not saved in contacts
        if (isUnsaved) {
            InlineActionButton(
                icon  = painterResource(R.drawable.ic_add),
                label = "Add",
                tint  = MaterialTheme.colorScheme.primary
            ) {
                IntentUtils.addContact(context)
                onDone()
            }
        }
    }
}

@Composable
private fun InlineActionButton(
    icon   : Painter,
    label  : String,
    tint   : Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Surface(
            shape    = CircleShape,
            color    = Color.Transparent,
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter        = icon,
                    contentDescription = label,
                    tint               = tint,
                    modifier           = Modifier.size(23.dp)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

fun openWhatsApp(context: Context, rawNumber: String) {
    try {
        val number = rawNumber.replace(Regex("[^\\d+]"), "")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://wa.me/$number")
            setPackage("com.whatsapp")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Call log item row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CallLogItem(
    entry      : CallLogEntry,
    isExpanded : Boolean,
    onRowClick : () -> Unit,
    onCallClick: () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .background(
                if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = entry.displayName, photoUri = entry.photoUri, size = 46.dp)
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
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.onPrimaryContainer
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

@Composable
fun SwipeGestureHint(onDismissed: () -> Unit) {
    val offsetX = remember { Animatable(-60f) }
    val alpha   = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(300))
        repeat(2) {
            offsetX.animateTo( 60f, tween(500, easing = FastOutSlowInEasing))
            offsetX.animateTo(-60f, tween(500, easing = FastOutSlowInEasing))
        }
        delay(300)
        alpha.animateTo(0f, tween(400))
        onDismissed()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .alpha(alpha.value)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier              = Modifier.offset(x = offsetX.value.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector        = Icons.Outlined.SwipeLeft,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(28.dp)
            )
            Text(
                text  = stringResource(R.string.swipe_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}