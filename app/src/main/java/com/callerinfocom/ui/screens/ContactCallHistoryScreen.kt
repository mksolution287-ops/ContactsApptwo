package com.callerinfocom.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callerinfocom.data.model.CallLogEntry
import com.callerinfocom.data.model.CallType
import com.callerinfocom.data.viewmodel.ContactsViewModel
import com.callerinfocom.data.viewmodel.UiState
import com.callerinfocom.ui.components.BannerAd
import com.callerinfocom.utils.IntentUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.callerinfocom.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCallHistoryScreen(
    contactId: Long,
    onBack: () -> Unit,
    viewModel: ContactsViewModel = viewModel()
) {
    val context = LocalContext.current
    val contact by viewModel.getContactByIdAsFlow(contactId).collectAsState(initial = null)
    val callLogState by viewModel.callLogState.collectAsState()

    // Filter call logs to only those matching this contact's phone numbers
    val contactLogs = remember(callLogState, contact) {
        val c = contact ?: return@remember emptyList()
        val numbers = c.phoneNumbers.map { normalizeNumber(it.number) }.toSet()
        when (val s = callLogState) {
            is UiState.Success -> s.data.filter { entry ->
                normalizeNumber(entry.number) in numbers ||
                        // also match by cached name in case number format differs
                        (!entry.contactName.isNullOrBlank() &&
                                entry.contactName.equals(c.name, ignoreCase = true))
            }
            else -> emptyList()
        }
    }

    // Fade-in animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "fade")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = contact?.name ?: stringResource(R.string.call_history),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                        if (contact != null) {
                            Text(
                                text = "Call history",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        when {
            // Loading
            callLogState is UiState.Loading -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Empty state
            contactLogs.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues).alpha(alpha),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(72.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "No call history",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(
                                R.string.calls_with_contact_placeholder,
                                contact?.name ?: stringResource(R.string.this_contact)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // List
            else -> {
                // Group calls by date section (Today / Yesterday / date)
                val grouped = remember(contactLogs) { groupCallsByDate(contactLogs) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(alpha),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {

                        // Summary chip row at top
                        item {
                            CallSummaryRow(logs = contactLogs)
                            Spacer(Modifier.height(8.dp))
                        }

                        grouped.forEach { (dateLabel, entries) ->

                            // Date section header
                            item(key = "header_$dateLabel") {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        start = 16.dp, end = 16.dp,
                                        top = 16.dp, bottom = 6.dp
                                    )
                                )
                            }

                            // Call entries card
                            item(key = "card_$dateLabel") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ) {
                                    Column {
                                        entries.forEachIndexed { idx, entry ->
                                            if (idx > 0) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 56.dp, end = 12.dp),
                                                    thickness = 0.5.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                                )
                                            }
                                            CallLogRow(
                                                entry = entry,
                                                onCallBack = {
                                                    IntentUtils.makeCall(
                                                        context,
                                                        entry.number,
                                                        context.getString(R.string.error_no_phone_app)
                                                    )
                                                },
                                                onSms = {
                                                    IntentUtils.sendSms(
                                                        context,
                                                        entry.number,
                                                        context.getString(R.string.error_no_messaging_app)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                    BannerAd()
                }
            }
        }
    }
}

// ── Summary chips (total / incoming / outgoing / missed) ─────────────────────

@Composable
private fun CallSummaryRow(logs: List<CallLogEntry>) {
    val incoming = logs.count { it.callType == CallType.INCOMING }
    val outgoing = logs.count { it.callType == CallType.OUTGOING }
    val missed   = logs.count { it.callType == CallType.MISSED }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            label = "${logs.size}",
            sublabel = stringResource(R.string.total),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "$incoming",
            sublabel = stringResource(R.string.incoming),
            color = Color(0xFF2E7D32),
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "$outgoing",
            sublabel = stringResource(R.string.outgoing),
            color = Color(0xFF1565C0),
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "$missed",
            sublabel = stringResource(R.string.missed),
            color = Color(0xFFC62828),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    sublabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = color
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

// ── Individual call log row ───────────────────────────────────────────────────

@Composable
private fun CallLogRow(
    entry: CallLogEntry,
    onCallBack: () -> Unit,
    onSms: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Call type icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    callTypeColor(entry.callType).copy(alpha = 0.12f),
                    RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = callTypeIcon(entry.callType),
                contentDescription = null,
                tint = callTypeColor(entry.callType),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Type + time + duration
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = callTypeLabel(context,entry.callType),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTime(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (entry.callType != CallType.MISSED && entry.duration > 0) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = formatDuration(entry.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // SMS icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onSms)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "SMS",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Call-back icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onCallBack)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "Call back",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun callTypeIcon(type: CallType): ImageVector = when (type) {
    CallType.INCOMING -> Icons.Outlined.CallReceived
    CallType.OUTGOING -> Icons.Outlined.CallMade
    CallType.MISSED   -> Icons.Outlined.CallMissed
    else              -> Icons.Outlined.Call
}

fun callTypeLabel(context: Context, type: CallType): String {
    return when (type) {
        CallType.INCOMING -> context.getString(R.string.incoming)
        CallType.OUTGOING -> context.getString(R.string.outgoing)
        CallType.MISSED   -> context.getString(R.string.missed)
        else              -> context.getString(R.string.call)
    }
}

@Composable
private fun callTypeColor(type: CallType): Color = when (type) {
    CallType.INCOMING -> Color(0xFF2E7D32)
    CallType.OUTGOING -> Color(0xFF1565C0)
    CallType.MISSED   -> Color(0xFFC62828)
    else              -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(seconds: Long): String {
    val h = TimeUnit.SECONDS.toHours(seconds)
    val m = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val s = seconds % 60
    return when {
        h > 0  -> "${h}h ${m}m"
        m > 0  -> "${m}m ${s}s"
        else   -> "${s}s"
    }
}

private fun normalizeNumber(number: String): String =
    number.filter { it.isDigit() }.takeLast(10)

private fun groupCallsByDate(logs: List<CallLogEntry>): List<Pair<String, List<CallLogEntry>>> {
    val today     = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val sdf       = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    return logs
        .groupBy { entry ->
            val cal = Calendar.getInstance().apply { timeInMillis = entry.date }
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        .entries
        .sortedByDescending { it.key }
        .map { (epochDay, entries) ->
            val label = when {
                epochDay >= today.timeInMillis     -> "Today"
                epochDay >= yesterday.timeInMillis -> "Yesterday"
                else -> sdf.format(Date(epochDay))
            }
            label to entries.sortedByDescending { it.date }
        }
}