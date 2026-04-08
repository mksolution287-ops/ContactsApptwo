package com.contactsapptwomktech.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contactsapptwomktech.R
import com.contactsapptwomktech.data.model.AppLanguage
import com.contactsapptwomktech.data.viewmodel.OnboardingViewModel
import com.contactsapptwomktech.data.viewmodel.SettingsViewModel
import com.contactsapptwomktech.ui.theme.AppAccentColor
import com.contactsapptwomktech.ui.theme.AppThemeMode
import com.contactsapptwomktech.utils.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onboardingViewModel: OnboardingViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.themeSettings.collectAsState()
    val selectedLanguage by onboardingViewModel.selectedLanguage.collectAsState()
    val context = LocalContext.current

    val callerIdEnabled by viewModel.callerIdEnabled.collectAsState()
    val keypadSound by viewModel.keypadSoundEnabled.collectAsState()
    val callbackScreen by viewModel.callbackScreenEnabled.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }

    val prefs = remember {
        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            if (!callerIdEnabled) {
                CallerIdBanner {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    prefs.edit().putBoolean("caller_id_enabled", true).apply()
                    viewModel.setCallerIdEnabled(true)
                }
            }

            // GENERAL
            SettingsSectionCard {
                SettingsIconRow(Icons.Default.Call, Color(0xFF1E6B3C), "Call Settings") {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
                SettingsDivider()

                SettingsIconRow(Icons.Default.DarkMode, Color(0xFF37474F), "Dark Mode") {
                    showThemeDialog = true
                }
                SettingsDivider()

                // ✅ Accent Color Row
                SettingsIconRow(Icons.Default.Palette, Color(0xFF8E24AA), "Accent Color") {
                    showAccentDialog = true
                }
                SettingsDivider()

                SettingsIconRow(Icons.Default.Language, Color(0xFF1A5276), "Change Language") {
                    showLanguageDialog = true
                }
            }

            // SOUND
            SettingsSectionLabel("Sound")
            SettingsSectionCard {
                SettingsIconToggleRow(
                    Icons.Default.VolumeUp,
                    Color(0xFF6D3B8C),
                    "Keypad Sound",
                    keypadSound
                ) { viewModel.setKeypadSound(it) }

                SettingsDivider()

                SettingsIconRow(Icons.Default.Vibration, Color(0xFF5D4037), "Sound & Vibration") {
                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                }
            }

            // PHONE
            SettingsSectionLabel("Phone")
            SettingsSectionCard {
                SettingsIconRow(Icons.Default.SimCard, Color(0xFF37474F), "SIM Preference") {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                    else
                        Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                    context.startActivity(intent)
                }

                SettingsDivider()

                SettingsIconRow(Icons.Default.Accessibility, Color(0xFF1A6B45), "Accessibility") {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }

                SettingsDivider()

                SettingsIconToggleRow(
                    Icons.Default.PhoneCallback,
                    Color(0xFF1A6B45),
                    "Callback Screen",
                    callbackScreen
                ) { viewModel.setCallbackScreen(it) }
            }

            // OTHER
            SettingsSectionLabel("Other")
            SettingsSectionCard {
                SettingsIconRow(Icons.Default.Share, Color(0xFF1565C0), "Share App") {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out this app!")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share via"))
                }

                SettingsDivider()

                SettingsIconRow(
                    Icons.Default.PrivacyTip,
                    Color(0xFF37474F),
                    stringResource(R.string.privacy_policy)
                ) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://sites.google.com/view/mktechsolutiosrewa?usp=sharing")
                        )
                    )
                }
            }
        }
    }

    // LANGUAGE DIALOG
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Change Language", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppLanguage.entries.forEach { language ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onboardingViewModel.saveLanguage(language)
                                    LocaleHelper.saveLanguage(context, language.code)
                                    showLanguageDialog = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${language.flag} ${language.nativeName}")
                            if (selectedLanguage == language) {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {showLanguageDialog = false}) { Text(text = "Close") }
            }
        )
    }

    // THEME DIALOG
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Dark Mode", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode.getLabel())
                            if (settings.themeMode == mode) {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {showThemeDialog = false}){
                    Text(text = "Close")
                }
            }
        )
    }

    // ✅ ACCENT COLOR DIALOG
    if (showAccentDialog) {
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text("Choose Accent Color", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setDynamicColor(true)
                                showAccentDialog = false
                            },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dynamic (System)")
                        if (settings.useDynamicColor) {
                            Icon(Icons.Default.Check, null)
                        }
                    }

                    HorizontalDivider()

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AppAccentColor.entries.toList()) { accent ->
                            val selected =
                                settings.accentColor == accent && !settings.useDynamicColor

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accent.seed)
                                    .border(
                                        if (selected) 3.dp else 0.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                    .clickable {
                                        viewModel.setAccentColor(accent)
                                        viewModel.setDynamicColor(false)
                                        showAccentDialog = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, null, tint = Color.White)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
// ── Caller ID Banner ───────────────────────────────────────────────────────

@Composable
private fun CallerIdBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B8C3C), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Warning icon circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, RoundedCornerShape(50))
        ) {
            Icon(
                imageVector        = Icons.Default.Warning,
                contentDescription = null,
                tint               = Color(0xFFF9A825),
                modifier           = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text       = "Caller ID Disabled!",
                color      = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
            Text(
                text     = "Caller ID & Spam Protection needed.",
                color    = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

// ── Section label ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Normal,
        color      = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier   = Modifier.padding(start = 4.dp, bottom = 0.dp)
    )
}

// ── Section card container ─────────────────────────────────────────────────

@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            content  = content
        )
    }
}

// ── Thin divider between rows ──────────────────────────────────────────────

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 60.dp, end = 8.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

// ── Icon row (clickable, no toggle) ───────────────────────────────────────

@Composable
private fun SettingsIconRow(
    icon   : ImageVector,
    iconBg : Color,
    title  : String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon = icon, bg = iconBg)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text     = title,
            style    = MaterialTheme.typography.bodyLarge,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector        = Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier           = Modifier.size(18.dp)
        )
    }
}

// ── Icon row with toggle ──────────────────────────────────────────────────

@Composable
private fun SettingsIconToggleRow(
    icon           : ImageVector,
    iconBg         : Color,
    title          : String,
    checked        : Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconBox(icon = icon, bg = iconBg)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text     = title,
            style    = MaterialTheme.typography.bodyLarge,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = MaterialTheme.colorScheme.primary
            )
        )
    }
}

// ── Rounded icon box ──────────────────────────────────────────────────────

@Composable
private fun SettingsIconBox(icon: ImageVector, bg: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .size(36.dp)
            .background(bg, RoundedCornerShape(9.dp))
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = Color.White,
            modifier           = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text       = title,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 5.dp),
            color      = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}
@Composable
fun AppThemeMode.getLabel(): String {
    return when (this) {
        AppThemeMode.SYSTEM -> stringResource(R.string.system)
        AppThemeMode.LIGHT  -> stringResource(R.string.light)
        AppThemeMode.DARK   -> stringResource(R.string.dark)
    }
}