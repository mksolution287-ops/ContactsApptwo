package com.contactsapptwomktech.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.contactsapptwomktech.R
import com.contactsapptwomktech.data.model.Contact
import com.contactsapptwomktech.data.viewmodel.ContactsViewModel
import com.contactsapptwomktech.utils.DialTonePlayer
import com.contactsapptwomktech.utils.IntentUtils

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

// ---------------------------------------------------------------------------
// Root Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialpadScreen(
    onSettingsClick: () -> Unit,
//    settingsViewModel: SettingsViewModel,
    viewModel: ContactsViewModel = viewModel()
) {
    val context    = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var dialedNumber by remember { mutableStateOf("") }
    // Sheet is open by default. FAB appears only after the sheet is dismissed.
    var showDialpad  by remember { mutableStateOf(true) }

    // Drive ViewModel's filteredContacts via the shared search query
    LaunchedEffect(dialedNumber) {
        viewModel.setSearchQuery(dialedNumber)
    }

    //dialpad
    LaunchedEffect(Unit) {
        DialTonePlayer.init()
    }

    DisposableEffect(Unit) {
        onDispose {
            DialTonePlayer.release()
        }
    }

    // Show contacts ONLY when the user has typed something; blank screen otherwise
    val allFiltered    by viewModel.filteredContacts.collectAsState()
    val visibleContacts = if (dialedNumber.isEmpty()) emptyList() else allFiltered

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
                            text       = stringResource(R.string.dialpad_title),
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector        = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.nav_settings),
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor    = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 0.5.dp
                )
            }
        },
        // FAB: hidden while sheet is open, slides in after dismiss.
        // Always rendered so the Scaffold reserves the FAB slot and
        // the animation has something to transition from/to.
        floatingActionButton = {
            AnimatedVisibility(
                visible = !showDialpad,
                enter   = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 2 },
                exit    = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it / 2 }
            ) {
                FloatingActionButton(
                    onClick        = { showDialpad = true },
                    shape          = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    elevation      = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Dialpad,
                        contentDescription = "Open Dialpad",
                        tint               = MaterialTheme.colorScheme.onPrimary,
                        modifier           = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filtered contacts — only visible once user types digits
            if (visibleContacts.isNotEmpty()) {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(visibleContacts, key = { it.id }) { contact ->
                        ContactRow(
                            contact     = contact,
                            query       = dialedNumber,
                            onCallClick = {
                                val number = contact.phoneNumbers.firstOrNull()?.number
                                    ?: return@ContactRow
                                IntentUtils.makeCall(
                                    context,
                                    number,
                                    context.getString(R.string.error_no_phone_app)
                                )
                            }
                        )
                    }
                }
            }
            // Empty screen (no digits typed) is intentionally blank
        }
    }

    // ── Bottom Sheet ───────────────────────────────────────────────────────
    if (showDialpad) {
        ModalBottomSheet(
            onDismissRequest = {
                showDialpad  = false
//                dialedNumber = ""
//                viewModel.setSearchQuery("")
            },
            sheetState     = sheetState,
            shape          = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle     = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        ) {
            DialpadContent(
                dialedNumber    = dialedNumber,
//                dialSoundEnabled = settingsViewModel.dialSoundEnabled,
                onDigitAppend   = { digit ->
                    if (dialedNumber.length < 15) dialedNumber += digit
                },
                onLongPressZero = {
                    if (dialedNumber.isNotEmpty() && dialedNumber.length < 15)
                        dialedNumber = dialedNumber.dropLast(1) + "+"
                },
                onBackspace     = {
                    if (dialedNumber.isNotEmpty())
                        dialedNumber = dialedNumber.dropLast(1)
                },
                onLongBackspace = { dialedNumber = "" },
                onCall          = {
                    if (dialedNumber.isNotEmpty()) {
                        IntentUtils.makeCall(
                            context,
                            dialedNumber,
                            context.getString(R.string.error_no_phone_app)
                        )
                        showDialpad  = false
                        dialedNumber = ""
                        viewModel.setSearchQuery("")
                    }
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Dialpad content (inside the bottom sheet)
// ---------------------------------------------------------------------------

@Composable
private fun DialpadContent(
    dialedNumber    : String,
//    dialSoundEnabled: Boolean,
    onDigitAppend   : (String) -> Unit,
    onLongPressZero : () -> Unit,
    onBackspace     : () -> Unit,
    onLongBackspace : () -> Unit,
    onCall          : () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Number display + backspace
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.size(40.dp)) // mirrors backspace button for visual centering

            Text(
                text          = dialedNumber,
                style         = MaterialTheme.typography.displaySmall,
                fontWeight    = FontWeight.Light,
                color         = MaterialTheme.colorScheme.onSurface,
                textAlign     = TextAlign.Center,
                letterSpacing = 4.sp,
                maxLines      = 1,
                modifier      = Modifier.weight(1f)
            )

            BackspaceButton(
                enabled     = dialedNumber.isNotEmpty(),
                onClick     = onBackspace,
                onLongClick = onLongBackspace
            )
        }

        Spacer(Modifier.height(8.dp))

        // Dialpad grid — 4 rows × 3 keys
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

        Spacer(Modifier.height(12.dp))
    }
}

// ---------------------------------------------------------------------------
// Contact row with call icon on right end
// ---------------------------------------------------------------------------

@Composable
private fun ContactRow(
    contact     : Contact,
    query       : String,
    onCallClick : () -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Filled.Person,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier           = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Name (highlighted) + first phone number
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

        // Call icon at right end
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
// Call button (inside dialpad sheet)
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
            imageVector        = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.dialpad_backspace),
            tint               = if (enabled) MaterialTheme.colorScheme.onSurface
            else         MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier           = Modifier.size(22.dp)
        )
    }
}