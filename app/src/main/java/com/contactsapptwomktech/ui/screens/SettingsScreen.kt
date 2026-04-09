//package com.contactsapptwomktech.ui.screens
//
//import android.content.Intent
//import android.os.Build
//import android.provider.Settings
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.rememberScrollState
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.contactsapptwomktech.R
//import com.contactsapptwomktech.data.model.AppLanguage
//import com.contactsapptwomktech.data.viewmodel.OnboardingViewModel
//import com.contactsapptwomktech.data.viewmodel.SettingsViewModel
//import com.contactsapptwomktech.ui.theme.AppAccentColor
//import com.contactsapptwomktech.ui.theme.AppThemeMode
//import com.contactsapptwomktech.utils.LocaleHelper
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SettingsScreen(
//    viewModel: SettingsViewModel,
//    onboardingViewModel: OnboardingViewModel,
//    onBack: () -> Unit
//) {
//    val settings by viewModel.themeSettings.collectAsState()
//    val selectedLanguage by onboardingViewModel.selectedLanguage.collectAsState()
//    val context = LocalContext.current
//
//    val callerIdEnabled by viewModel.callerIdEnabled.collectAsState()
//    val keypadSound by viewModel.keypadSoundEnabled.collectAsState()
//    val callbackScreen by viewModel.callbackScreenEnabled.collectAsState()
//
//    var showLanguageDialog by remember { mutableStateOf(false) }
//    var showThemeDialog by remember { mutableStateOf(false) }
//    var showAccentDialog by remember { mutableStateOf(false) }
//
//    val prefs = remember {
//        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = stringResource(R.string.nav_settings),
//                        fontWeight = FontWeight.SemiBold,
//                        fontSize = 20.sp
//                    )
//                },
//                navigationIcon = {
//                    IconButton(onClick = onBack) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .verticalScroll(rememberScrollState())
//                .padding(innerPadding)
//                .padding(horizontal = 16.dp, vertical = 8.dp),
//            verticalArrangement = Arrangement.spacedBy(20.dp)
//        ) {
//
//            if (!callerIdEnabled) {
//                CallerIdBanner {
//                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
//                    prefs.edit().putBoolean("caller_id_enabled", true).apply()
//                    viewModel.setCallerIdEnabled(true)
//                }
//            }
//
//            // GENERAL
//            SettingsSectionCard {
//                SettingsIconRow(Icons.Default.Call, Color(0xFF1E6B3C), "Call Settings") {
//                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
//                }
//                SettingsDivider()
//
//                SettingsIconRow(Icons.Default.DarkMode, Color(0xFF37474F), "Dark Mode") {
//                    showThemeDialog = true
//                }
//                SettingsDivider()
//
//                // ✅ Accent Color Row
//                SettingsIconRow(Icons.Default.Palette, Color(0xFF8E24AA), "Accent Color") {
//                    showAccentDialog = true
//                }
//                SettingsDivider()
//
//                SettingsIconRow(Icons.Default.Language, Color(0xFF1A5276), "Change Language") {
//                    showLanguageDialog = true
//                }
//            }
//
//            // SOUND
//            SettingsSectionLabel("Sound")
//            SettingsSectionCard {
//                SettingsIconToggleRow(
//                    Icons.Default.VolumeUp,
//                    Color(0xFF6D3B8C),
//                    "Keypad Sound",
//                    keypadSound
//                ) { viewModel.setKeypadSound(it) }
//
//                SettingsDivider()
//
//                SettingsIconRow(Icons.Default.Vibration, Color(0xFF5D4037), "Sound & Vibration") {
//                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
//                }
//            }
//
//            // PHONE
//            SettingsSectionLabel("Phone")
//            SettingsSectionCard {
//                SettingsIconRow(Icons.Default.SimCard, Color(0xFF37474F), "SIM Preference") {
//                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
//                        Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
//                    else
//                        Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
//                    context.startActivity(intent)
//                }
//
//                SettingsDivider()
//
//                SettingsIconRow(Icons.Default.Accessibility, Color(0xFF1A6B45), "Accessibility") {
//                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
//                }
//
//                SettingsDivider()
//
//                SettingsIconToggleRow(
//                    Icons.Default.PhoneCallback,
//                    Color(0xFF1A6B45),
//                    "Callback Screen",
//                    callbackScreen
//                ) { viewModel.setCallbackScreen(it) }
//            }
//
//            // OTHER
//            SettingsSectionLabel("Other")
//            SettingsSectionCard {
//                SettingsIconRow(Icons.Default.Share, Color(0xFF1565C0), "Share App") {
//                    val intent = Intent(Intent.ACTION_SEND).apply {
//                        type = "text/plain"
//                        putExtra(Intent.EXTRA_TEXT, "Check out this app!")
//                    }
//                    context.startActivity(Intent.createChooser(intent, "Share via"))
//                }
//
//                SettingsDivider()
//
//                SettingsIconRow(
//                    Icons.Default.PrivacyTip,
//                    Color(0xFF37474F),
//                    stringResource(R.string.privacy_policy)
//                ) {
//                    context.startActivity(
//                        Intent(
//                            Intent.ACTION_VIEW,
//                            android.net.Uri.parse("https://sites.google.com/view/mktechsolutiosrewa?usp=sharing")
//                        )
//                    )
//                }
//            }
//        }
//    }
//
//    // LANGUAGE DIALOG
//    if (showLanguageDialog) {
//        AlertDialog(
//            onDismissRequest = { showLanguageDialog = false },
//            title = { Text("Change Language", fontWeight = FontWeight.Bold) },
//            text = {
//                Column {
//                    AppLanguage.entries.forEach { language ->
//                        Row(
//                            Modifier
//                                .fillMaxWidth()
//                                .clickable {
//                                    onboardingViewModel.saveLanguage(language)
//                                    LocaleHelper.saveLanguage(context, language.code)
//                                    showLanguageDialog = false
//                                }
//                                .padding(10.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text("${language.flag} ${language.nativeName}")
//                            if (selectedLanguage == language) {
//                                Icon(Icons.Default.Check, null)
//                            }
//                        }
//                    }
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = {showLanguageDialog = false}) { Text(text = "Close") }
//            }
//        )
//    }
//
//    // THEME DIALOG
//    if (showThemeDialog) {
//        AlertDialog(
//            onDismissRequest = { showThemeDialog = false },
//            title = { Text("Dark Mode", fontWeight = FontWeight.Bold) },
//            text = {
//                Column {
//                    AppThemeMode.entries.forEach { mode ->
//                        Row(
//                            Modifier
//                                .fillMaxWidth()
//                                .clickable {
//                                    viewModel.setThemeMode(mode)
//                                    showThemeDialog = false
//                                }
//                                .padding(10.dp),
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text(mode.getLabel())
//                            if (settings.themeMode == mode) {
//                                Icon(Icons.Default.Check, null)
//                            }
//                        }
//                    }
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = {showThemeDialog = false}){
//                    Text(text = "Close")
//                }
//            }
//        )
//    }
//
//    // ✅ ACCENT COLOR DIALOG
//    if (showAccentDialog) {
//        AlertDialog(
//            onDismissRequest = { showAccentDialog = false },
//            title = { Text("Choose Accent Color", fontWeight = FontWeight.Bold) },
//            text = {
//                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
//
//                    Row(
//                        Modifier
//                            .fillMaxWidth()
//                            .clickable {
//                                viewModel.setDynamicColor(true)
//                                showAccentDialog = false
//                            },
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Text("Dynamic (System)")
//                        if (settings.useDynamicColor) {
//                            Icon(Icons.Default.Check, null)
//                        }
//                    }
//
//                    HorizontalDivider()
//
//                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//                        items(AppAccentColor.entries.toList()) { accent ->
//                            val selected =
//                                settings.accentColor == accent && !settings.useDynamicColor
//
//                            Box(
//                                modifier = Modifier
//                                    .size(40.dp)
//                                    .clip(CircleShape)
//                                    .background(accent.seed)
//                                    .border(
//                                        if (selected) 3.dp else 0.dp,
//                                        MaterialTheme.colorScheme.outline,
//                                        CircleShape
//                                    )
//                                    .clickable {
//                                        viewModel.setAccentColor(accent)
//                                        viewModel.setDynamicColor(false)
//                                        showAccentDialog = false
//                                    },
//                                contentAlignment = Alignment.Center
//                            ) {
//                                if (selected) {
//                                    Icon(Icons.Default.Check, null, tint = Color.White)
//                                }
//                            }
//                        }
//                    }
//                }
//            },
//            confirmButton = {
//                TextButton(onClick = { showAccentDialog = false }) {
//                    Text("Close")
//                }
//            }
//        )
//    }
//}
//// ── Caller ID Banner ───────────────────────────────────────────────────────
//
//@Composable
//private fun CallerIdBanner(onClick: () -> Unit) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Color(0xFF1B8C3C), RoundedCornerShape(14.dp))
//            .clickable { onClick() }
//            .padding(horizontal = 16.dp, vertical = 14.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        // Warning icon circle
//        Box(
//            contentAlignment = Alignment.Center,
//            modifier = Modifier
//                .size(48.dp)
//                .background(Color.White, RoundedCornerShape(50))
//        ) {
//            Icon(
//                imageVector        = Icons.Default.Warning,
//                contentDescription = null,
//                tint               = Color(0xFFF9A825),
//                modifier           = Modifier.size(28.dp)
//            )
//        }
//        Spacer(modifier = Modifier.width(14.dp))
//        Column {
//            Text(
//                text       = "Caller ID Disabled!",
//                color      = Color.White,
//                fontWeight = FontWeight.Bold,
//                fontSize   = 16.sp
//            )
//            Text(
//                text     = "Caller ID & Spam Protection needed.",
//                color    = Color.White.copy(alpha = 0.85f),
//                fontSize = 13.sp
//            )
//        }
//    }
//}
//
//// ── Section label ──────────────────────────────────────────────────────────
//
//@Composable
//private fun SettingsSectionLabel(title: String) {
//    Text(
//        text       = title,
//        style      = MaterialTheme.typography.titleSmall,
//        fontWeight = FontWeight.Normal,
//        color      = MaterialTheme.colorScheme.onSurfaceVariant,
//        modifier   = Modifier.padding(start = 4.dp, bottom = 0.dp)
//    )
//}
//
//// ── Section card container ─────────────────────────────────────────────────
//
//@Composable
//private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
//    Card(
//        modifier = Modifier.fillMaxWidth(),
//        shape    = RoundedCornerShape(14.dp),
//        colors   = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
//        ),
//        elevation = CardDefaults.cardElevation(0.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
//            content  = content
//        )
//    }
//}
//
//// ── Thin divider between rows ──────────────────────────────────────────────
//
//@Composable
//private fun SettingsDivider() {
//    HorizontalDivider(
//        modifier  = Modifier.padding(start = 60.dp, end = 8.dp),
//        thickness = 0.5.dp,
//        color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
//    )
//}
//
//// ── Icon row (clickable, no toggle) ───────────────────────────────────────
//
//@Composable
//private fun SettingsIconRow(
//    icon   : ImageVector,
//    iconBg : Color,
//    title  : String,
//    onClick: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onClick() }
//            .padding(horizontal = 12.dp, vertical = 13.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        SettingsIconBox(icon = icon, bg = iconBg)
//        Spacer(modifier = Modifier.width(14.dp))
//        Text(
//            text     = title,
//            style    = MaterialTheme.typography.bodyLarge,
//            color    = MaterialTheme.colorScheme.onSurface,
//            modifier = Modifier.weight(1f)
//        )
//        Icon(
//            imageVector        = Icons.Default.ChevronRight,
//            contentDescription = null,
//            tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
//            modifier           = Modifier.size(18.dp)
//        )
//    }
//}
//
//// ── Icon row with toggle ──────────────────────────────────────────────────
//
//@Composable
//private fun SettingsIconToggleRow(
//    icon           : ImageVector,
//    iconBg         : Color,
//    title          : String,
//    checked        : Boolean,
//    onCheckedChange: (Boolean) -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onCheckedChange(!checked) }
//            .padding(horizontal = 12.dp, vertical = 8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        SettingsIconBox(icon = icon, bg = iconBg)
//        Spacer(modifier = Modifier.width(14.dp))
//        Text(
//            text     = title,
//            style    = MaterialTheme.typography.bodyLarge,
//            color    = MaterialTheme.colorScheme.onSurface,
//            modifier = Modifier.weight(1f)
//        )
//        Switch(
//            checked         = checked,
//            onCheckedChange = onCheckedChange,
//            colors          = SwitchDefaults.colors(
//                checkedThumbColor  = Color.White,
//                checkedTrackColor  = MaterialTheme.colorScheme.primary
//            )
//        )
//    }
//}
//
//// ── Rounded icon box ──────────────────────────────────────────────────────
//
//@Composable
//private fun SettingsIconBox(icon: ImageVector, bg: Color) {
//    Box(
//        contentAlignment = Alignment.Center,
//        modifier         = Modifier
//            .size(36.dp)
//            .background(bg, RoundedCornerShape(9.dp))
//    ) {
//        Icon(
//            imageVector        = icon,
//            contentDescription = null,
//            tint               = Color.White,
//            modifier           = Modifier.size(20.dp)
//        )
//    }
//}
//
//@Composable
//private fun SettingsGroup(
//    title: String,
//    content: @Composable ColumnScope.() -> Unit
//) {
//    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
//        Text(
//            text       = title,
//            style      = MaterialTheme.typography.labelLarge,
//            fontWeight = FontWeight.SemiBold,
//            modifier = Modifier.padding(horizontal = 5.dp),
//            color      = MaterialTheme.colorScheme.primary
//        )
//        Card(
//            modifier = Modifier.fillMaxWidth(),
//            colors   = CardDefaults.cardColors(
//                containerColor = Color.Transparent
//            ),
//            shape = MaterialTheme.shapes.medium
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                content = content
//            )
//        }
//    }
//}
//@Composable
//fun AppThemeMode.getLabel(): String {
//    return when (this) {
//        AppThemeMode.SYSTEM -> stringResource(R.string.system)
//        AppThemeMode.LIGHT  -> stringResource(R.string.light)
//        AppThemeMode.DARK   -> stringResource(R.string.dark)
//    }
//}
package com.contactsapptwomktech.ui.screens

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.contactsapptwomktech.R
import com.contactsapptwomktech.data.model.AppLanguage
import com.contactsapptwomktech.data.viewmodel.OnboardingViewModel
import com.contactsapptwomktech.data.viewmodel.SettingsViewModel
import com.contactsapptwomktech.service.CallerIdOverlayService
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
    val activity = context as? Activity
    val lifecycleOwner   = LocalLifecycleOwner.current
    val callerIdEnabled by viewModel.callerIdEnabled.collectAsState()
    val callerIdOverlayEnabled by viewModel.callerIdOverlayEnabled.collectAsState()
    val keypadSound by viewModel.keypadSoundEnabled.collectAsState()
    val callbackScreen by viewModel.callbackScreenEnabled.collectAsState()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showCallerIdDialog by remember { mutableStateOf(false) }   // ← NEW

    val prefs = remember {
        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
    }

    // ── RoleManager result launcher ───────────────────────────────────────────
    // rememberLauncherForActivityResult captures the user's choice from the
    // native system dialog and updates state immediately — no screen navigation.
    val roleRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        viewModel.setCallerIdEnabled(granted)
        prefs.edit().putBoolean("caller_id_enabled", granted).apply()
    }

    // ── Trigger native system Caller ID dialog ────────────────────────────────
    fun requestCallerIdRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            when {
                // Already default — sync state
                roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) -> {
                    viewModel.setCallerIdEnabled(true)
                    prefs.edit().putBoolean("caller_id_enabled", true).apply()
                }
                // Role available — show the native dialog (exactly the screenshot)
                roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) -> {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    roleRequestLauncher.launch(intent)
                }
                // Role not available on this device
                else -> {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
            }
        } else {
            // Pre-API 29 fallback
            context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
    }

    // ── Re-check on resume (handles the pre-Q fallback path) ─────────────────
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val isDefault = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val roleManager = context.getSystemService(RoleManager::class.java)
                    roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
                } else {
                    prefs.getBoolean("caller_id_enabled", false)
                }
                viewModel.setCallerIdEnabled(isDefault)
                prefs.edit().putBoolean("caller_id_enabled", isDefault).apply()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // Helper: check overlay permission & start/stop service
    fun toggleOverlayService(enabled: Boolean) {
        viewModel.setCallerIdOverlay(enabled)
        val serviceIntent = Intent(context, CallerIdOverlayService::class.java)
        if (enabled) {
            if (!Settings.canDrawOverlays(context)) {
                // Ask for SYSTEM_ALERT_WINDOW permission
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                )
                viewModel.setCallerIdOverlay(false) // re-check after user returns
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            context.stopService(serviceIntent)
        }
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

            // ── Caller ID banner — tap opens custom dialog ─────────────────────
//            if (!callerIdEnabled) {
//                CallerIdBanner { showCallerIdDialog = true }   // ← opens dialog now
//            }
            if (!callerIdEnabled) {
                CallerIdBanner { requestCallerIdRole() }
            }

            // GENERAL
            SettingsSectionCard {
                SettingsIconRow(Icons.Default.Call, Color(0xFF1E6B3C), stringResource(R.string.call_settings)) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
                SettingsDivider()
                SettingsIconRow(Icons.Default.DarkMode, Color(0xFF37474F), stringResource(R.string.dark_mode)) {
                    showThemeDialog = true
                }
                SettingsDivider()
                SettingsIconRow(Icons.Default.Palette, Color(0xFF8E24AA), stringResource(R.string.accent_color)) {
                    showAccentDialog = true
                }
                SettingsDivider()
                SettingsIconRow(Icons.Default.Language, Color(0xFF1A5276), stringResource(R.string.change_language)) {
                    showLanguageDialog = true
                }
            }

            // SOUND
            SettingsSectionLabel(stringResource(R.string.sound))
            SettingsSectionCard {
                SettingsIconToggleRow(
                    Icons.Default.VolumeUp, Color(0xFF6D3B8C), stringResource(R.string.keypad_sound), keypadSound
                ) { viewModel.setKeypadSound(it) }
                SettingsDivider()
                SettingsIconRow(Icons.Default.Vibration, Color(0xFF5D4037), stringResource(R.string.soundnvibration)) {
                    context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
                }
            }

            // PHONE
            SettingsSectionLabel(stringResource(R.string.section_phone))
            SettingsSectionCard {
                // ── Caller ID toggle — only shown when caller ID is enabled ──────
                if (callerIdEnabled) {
                    SettingsIconToggleRow(
                        icon    = Icons.Default.ContactPhone,
                        iconBg  = Color(0xFF1A6B45),
                        title   = stringResource(R.string.calleridoverlay),
                        checked = callerIdOverlayEnabled
                    ) { toggleOverlayService(it) }
                    SettingsDivider()
                }

                SettingsIconRow(Icons.Default.SimCard, Color(0xFF1A6B45), stringResource(R.string.sim_preferences)) {
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
                    Icons.Default.PhoneCallback, Color(0xFF1A6B45), stringResource(R.string.call_back), callbackScreen
                ) { viewModel.setCallbackScreen(it) }
            }

            // OTHER
            SettingsSectionLabel(stringResource(R.string.legal))
            SettingsSectionCard {
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
                SettingsDivider()
                SettingsIconRow(
                    Icons.Default.PrivacyTip,
                    Color(0xFF37474F),
                    stringResource(R.string.terms_conditions)
                ) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://sites.google.com/view/mktechsolutionsrewa?usp=sharing")
                        )
                    )
                }
            }
        }
    }

    // ── Caller ID Default App Dialog (matches system sheet style) ─────────────
//    if (showCallerIdDialog) {
//        CallerIdDefaultDialog(
//            onDismiss = { showCallerIdDialog = false },
//            onSetDefault = {
//                showCallerIdDialog = false
//                // Navigate to system default apps settings
//                context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
//                prefs.edit().putBoolean("caller_id_enabled", true).apply()
//                viewModel.setCallerIdEnabled(true)
//            }
//        )
//    }

    // LANGUAGE DIALOG
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.change_language), fontWeight = FontWeight.Bold) },
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
                                    activity?.recreate()
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${language.flag} ${language.nativeName}")
                            if (selectedLanguage == language) Icon(Icons.Default.Check, null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    // THEME DIALOG
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) },
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
                            if (settings.themeMode == mode) Icon(Icons.Default.Check, null)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    // ACCENT COLOR DIALOG
    if (showAccentDialog) {
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text(stringResource(R.string.choose_accent_color), fontWeight = FontWeight.Bold) },
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
                        Text(stringResource(R.string.dynamic_system))
                        if (settings.useDynamicColor) Icon(Icons.Default.Check, null)
                    }
                    HorizontalDivider()
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(AppAccentColor.entries.toList()) { accent ->
                            val selected = settings.accentColor == accent && !settings.useDynamicColor
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
                                if (selected) Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentDialog = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

//@Composable
//private fun CallerIdDefaultDialog(
//    onDismiss: () -> Unit,
//    onSetDefault: () -> Unit
//) {
//    // Track which option the user has selected; default is "None"
//    var selectedOption by remember { mutableStateOf("none") }
//
//    Dialog(
//        onDismissRequest = onDismiss,
//        properties = DialogProperties(usePlatformDefaultWidth = false)
//    ) {
//        Surface(
//            modifier = Modifier
//                .fillMaxWidth(0.92f)
//                .wrapContentHeight(),
//            shape = RoundedCornerShape(20.dp),
//            color = MaterialTheme.colorScheme.surface,
//            tonalElevation = 8.dp
//        ) {
//            Column(
//                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//
//                // App icon
//                Box(
//                    modifier = Modifier
//                        .size(56.dp)
//                        .background(Color(0xFF1B8C3C), RoundedCornerShape(16.dp)),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        Icons.Default.Call,
//                        contentDescription = null,
//                        tint = Color.White,
//                        modifier = Modifier.size(30.dp)
//                    )
//                }
//
//                Spacer(Modifier.height(14.dp))
//
//                Text(
//                    text = "Set #Contacts as your default\ncaller ID & spam app?",
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Bold,
//                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
//                )
//
//                Spacer(Modifier.height(20.dp))
//
//                // Option: None
//                CallerIdOptionRow(
//                    iconContent = {
//                        Box(
//                            Modifier
//                                .size(36.dp)
//                                .background(Color(0xFFD32F2F), CircleShape),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                Icons.Default.Remove,
//                                contentDescription = null,
//                                tint = Color.White,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    },
//                    label = "None",
//                    subtitle = "Current default",
//                    selected = selectedOption == "none",
//                    onSelect = { selectedOption = "none" }
//                )
//
//                Spacer(Modifier.height(2.dp))
//
//                // Option: This app
//                CallerIdOptionRow(
//                    iconContent = {
//                        Box(
//                            Modifier
//                                .size(36.dp)
//                                .background(Color(0xFF1B8C3C), RoundedCornerShape(9.dp)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                Icons.Default.Call,
//                                contentDescription = null,
//                                tint = Color.White,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    },
//                    label = "#Contacts",
//                    subtitle = null,
//                    selected = selectedOption == "contacts",
//                    onSelect = { selectedOption = "contacts" }
//                )
//
//                Spacer(Modifier.height(24.dp))
//
//                // Buttons row
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
//                ) {
//                    TextButton(onClick = onDismiss) {
//                        Text("Cancel")
//                    }
//                    TextButton(
//                        // "Set as default" is only active when the user picked #Contacts
//                        onClick = { if (selectedOption == "contacts") onSetDefault() },
//                        enabled = selectedOption == "contacts"
//                    ) {
//                        Text("Set as default")
//                    }
//                }
//            }
//        }
//    }
//}

//@Composable
//private fun CallerIdOptionRow(
//    iconContent: @Composable () -> Unit,
//    label: String,
//    subtitle: String?,
//    selected: Boolean,
//    onSelect: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .clickable { onSelect() }
//            .padding(vertical = 10.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        iconContent()
//        Spacer(Modifier.width(14.dp))
//        Column(modifier = Modifier.weight(1f)) {
//            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
//            if (subtitle != null) {
//                Text(
//                    subtitle,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//        RadioButton(
//            selected = selected,
//            onClick = onSelect
//        )
//    }
//}

// ── Caller ID Banner ──────────────────────────────────────────────────────────

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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFF9A825),
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = stringResource(R.string.caller_id_disable),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(R.string.caller_id_disable_desc),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

// ── Reusable composables (unchanged) ─────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 0.dp)
    )
}

@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 60.dp, end = 8.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SettingsIconRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
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
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsIconToggleRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    checked: Boolean,
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
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsIconBox(icon: ImageVector, bg: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .background(bg, RoundedCornerShape(9.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
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