package com.callerinfocom.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callerinfocom.data.model.Contact
import com.callerinfocom.data.viewmodel.ContactsViewModel
import com.callerinfocom.ui.components.ContactAvatar
import com.callerinfocom.utils.DialTonePlayer
import com.callerinfocom.utils.IntentUtils
import androidx.compose.runtime.setValue
import com.callerinfocom.data.viewmodel.SettingsViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.callerinfocom.R

private val dialpadKeys = listOf(
    Triple("1", "", ""),
    Triple("2", "ABC", ""),
    Triple("3", "DEF", ""),
    Triple("4", "GHI", ""),
    Triple("5", "JKL", ""),
    Triple("6", "MNO", ""),
    Triple("7", "PQRS", ""),
    Triple("8", "TUV", ""),
    Triple("9", "WXYZ", ""),
    Triple("*", "", ""),
    Triple("0", "+", ""),
    Triple("#", "", "")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialpadScreen(
    onSettingsClick: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    viewModel: ContactsViewModel = viewModel()          // renamed for clarity
) {
    val context = LocalContext.current

    var dialedNumber by remember { mutableStateOf("") }
    var showDialpad by remember { mutableStateOf(true) }

//    // This will reactively update when user toggles the setting
//    val keypadSoundEnabled by settingsViewModel.keypadSoundEnabled.collectAsState()
    val keypadSoundEnabledState by settingsViewModel.keypadSoundEnabled.collectAsState()
    val keypadSoundEnabled by rememberUpdatedState(keypadSoundEnabledState)

    LaunchedEffect(dialedNumber) {
        viewModel.setSearchQuery(dialedNumber)
    }

    LaunchedEffect(Unit) {
        DialTonePlayer.init()
    }

    DisposableEffect(Unit) {
        onDispose { DialTonePlayer.release() }
    }

    val allFiltered by viewModel.filteredContacts.collectAsState()

    val contactsToShow: List<Contact> = when {
        dialedNumber.isNotEmpty() -> allFiltered
        else -> allFiltered.filter { it.isFavorite }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dialpad_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !showDialpad,
                enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
                exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 }
            ) {
                FloatingActionButton(
                    onClick = { showDialpad = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dialpad,
                        contentDescription = "Open Dialpad",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Contacts List
            if (contactsToShow.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    if (dialedNumber.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.favorites_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                    }

                    items(contactsToShow, key = { it.id }) { contact ->
                        ContactRow(
                            contact = contact,
                            query = dialedNumber,
                            onRowClick = {
                                contact.phoneNumbers.firstOrNull()?.number?.let { number ->
                                    val cleanNumber = number.filter { it.isDigit() || it == '+' }
                                    dialedNumber = cleanNumber
                                    showDialpad = true
                                }
                            },
                            onCallClick = {
                                val number = contact.phoneNumbers.firstOrNull()?.number ?: return@ContactRow
                                IntentUtils.makeCall(context, number, context.getString(R.string.error_no_phone_app))
                            }
                        )
                    }
                }
            } else if (dialedNumber.isEmpty()) {
                // No favorites empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Star, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(R.string.no_favorites), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_favorites_message),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                    }
                }
            }

            // Bottom Dialpad
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                AnimatedVisibility(
                    visible = showDialpad,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(250)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200))
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        DialpadContent(
                            dialedNumber = dialedNumber,
                            keypadSoundEnabled = keypadSoundEnabled,
                            onDigitAppend = { digit ->
                                if (dialedNumber.length < 15) {
                                    dialedNumber += digit
                                    // Always use the latest value from state
                                    if (keypadSoundEnabled) {
                                        DialTonePlayer.playTone(digit.first())
                                    }
                                }
                            },
                            onLongPressZero = {
                                if (dialedNumber.length < 15) {
                                    if (dialedNumber.isNotEmpty()) dialedNumber = dialedNumber.dropLast(1)
                                    dialedNumber += "+"
                                    if (keypadSoundEnabled) {
                                        DialTonePlayer.playTone('+')
                                    }
                                }
                            },
                            onBackspace = {
                                if (dialedNumber.isNotEmpty()) dialedNumber = dialedNumber.dropLast(1)
                            },
                            onLongBackspace = { dialedNumber = "" },
                            onCall = {
                                if (dialedNumber.isNotEmpty()) {
                                    IntentUtils.makeCall(context, dialedNumber, context.getString(R.string.error_no_phone_app))
                                    showDialpad = false
                                    dialedNumber = ""
                                    viewModel.setSearchQuery("")
                                }
                            },
                            onDismiss = { showDialpad = false }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dialpad content
// ---------------------------------------------------------------------------

@Composable
private fun DialpadContent(
    dialedNumber    : String,
    keypadSoundEnabled: Boolean,
    onDigitAppend   : (String) -> Unit,
    onLongPressZero : () -> Unit,
    onBackspace     : () -> Unit,
    onLongBackspace : () -> Unit,
    onCall          : () -> Unit,
    onDismiss       : () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Drag handle — swipe down or tap to dismiss ─────────────────────
        Box(
            modifier         = Modifier
                .padding(top = 4.dp, bottom = 12.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (dragOffset > 80f) onDismiss()
                            dragOffset = 0f
                        }
                    ) { _, dragAmount ->
                        if (dragAmount > 0) dragOffset += dragAmount
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {}

        // ── Number display + backspace ─────────────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.size(30.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val listState = rememberLazyListState()

                LaunchedEffect(dialedNumber) {
                    listState.animateScrollToItem(0)
                }

                LazyRow(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    item {
                        Text(
                            text = dialedNumber,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            letterSpacing = 4.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            BackspaceButton(
                enabled     = dialedNumber.isNotEmpty(),
                onClick     = onBackspace,
                onLongClick = onLongBackspace
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── Dialpad grid — 4 rows × 3 keys ────────────────────────────────
        dialpadKeys.chunked(3).forEach { rowKeys ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowKeys.forEach { (digit, letters, _) ->
                    DialKey(
                        digit       = digit,
                        letters     = letters,
                        onClick     = { onDigitAppend(digit) },
                        onLongClick = { if (digit == "0") onLongPressZero() }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        CallButton(
            enabled = dialedNumber.isNotEmpty(),
            onClick = onCall
        )

//        Spacer(Modifier.navigationBarsPadding())
//        Spacer(Modifier.height(12.dp))
    }
}

// ---------------------------------------------------------------------------
// Contact row
// ---------------------------------------------------------------------------

@Composable
private fun ContactRow(
    contact     : Contact,
    query       : String,
    onRowClick: () -> Unit,
    onCallClick : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable{onRowClick()}
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
//            Icon(
//                imageVector        = Icons.Filled.Person,
//                contentDescription = null,
//                tint               = MaterialTheme.colorScheme.onSecondaryContainer,
//                modifier           = Modifier.size(24.dp)
//            )
            ContactAvatar(
                name = contact.displayName,
                photoUri = contact.photoUri,
                size = 48.dp,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            HighlightedText(
                text           = contact.name,
                highlight      = query,
                style          = MaterialTheme.typography.bodyLarge,
                highlightColor = MaterialTheme.colorScheme.primary
            )
            Text(
                text     = contact.phoneNumbers.firstOrNull()?.number ?: "",
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onCallClick) {
            Icon(
                imageVector        = Icons.Outlined.Call,
                contentDescription = "Call ${contact.name}",
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(22.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Highlighted name text
// ---------------------------------------------------------------------------

@Composable
private fun HighlightedText(
    text           : String,
    highlight      : String,
    style          : androidx.compose.ui.text.TextStyle,
    highlightColor : Color
) {
    if (highlight.isEmpty()) {
        Text(text = text, style = style, color = MaterialTheme.colorScheme.onSurface)
        return
    }
    val idx       = text.indexOf(highlight, ignoreCase = true)
    val annotated = buildAnnotatedString {
        if (idx < 0) {
            append(text)
        } else {
            append(text.substring(0, idx))
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(idx, idx + highlight.length))
            }
            append(text.substring(idx + highlight.length))
        }
    }
    Text(text = annotated, style = style, color = MaterialTheme.colorScheme.onSurface)
}

// ---------------------------------------------------------------------------
// Dial key
// ---------------------------------------------------------------------------

@Composable
private fun DialKey(
    digit       : String,
    letters     : String,
    onClick     : () -> Unit,
    onLongClick : () -> Unit = {}
) {
    var pressed by remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(if (pressed) 0.92f else 1f, tween(80), label = "scale")
    val bgColor by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primaryContainer
        else         MaterialTheme.colorScheme.surfaceVariant,
        tween(80), label = "bg"
    )

    Surface(
        shape    = CircleShape,
        color    = bgColor,
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress     = { pressed = true; tryAwaitRelease(); pressed = false },
                    onTap       = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = digit,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Normal,
                color      = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text          = letters,
                    style         = MaterialTheme.typography.labelSmall,
                    color         = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Call button
// ---------------------------------------------------------------------------

@Composable
private fun CallButton(enabled: Boolean, onClick: () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(if (pressed) 0.92f else 1f, tween(80), label = "scale")

    Surface(
        shape    = CircleShape,
        color    = if (enabled) MaterialTheme.colorScheme.primary
        else         MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap   = { onClick() }
                    )
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector        = Icons.Outlined.Call,
                contentDescription = stringResource(R.string.dialpad_call_button),
                tint               = if (enabled) MaterialTheme.colorScheme.onPrimary
                else         MaterialTheme.colorScheme.onSurfaceVariant,
                modifier           = Modifier.size(28.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Backspace button
// ---------------------------------------------------------------------------

@Composable
private fun BackspaceButton(
    enabled     : Boolean,
    onClick     : () -> Unit,
    onLongClick : () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale   by animateFloatAsState(if (pressed) 0.92f else 1f, tween(80), label = "scale")

    Box(
        modifier         = Modifier
            .size(40.dp)
            .scale(scale)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress     = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap       = { onClick() },
                        onLongPress = { onLongClick() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter        = painterResource(R.drawable.clear),
            contentDescription = stringResource(R.string.dialpad_backspace),
            tint               = if (enabled) MaterialTheme.colorScheme.onSurface
            else         MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier           = Modifier.size(22.dp)
        )
    }
}