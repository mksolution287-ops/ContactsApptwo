package com.callerinfo.ui.screens

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalConfiguration
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
import com.callerinfo.data.model.AppLanguage
import com.callerinfo.data.viewmodel.OnboardingViewModel
import com.callerinfo.data.viewmodel.SettingsViewModel
import com.callerinfo.service.CallerIdOverlayService
import com.callerinfo.ui.components.NativeAdCard
import com.callerinfo.ui.theme.AppAccentColor
import com.callerinfo.ui.theme.AppThemeMode
import com.callerinfo.utils.AdManager
import com.callerinfo.utils.LocaleHelper
import com.callerinfo.R

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

            NativeAdCard(
                modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
            )
            // GENERAL
            SettingsSectionCard {
                SettingsIconRow(Icons.Default.Call, Color(0xFF1E6B3C), stringResource(R.string.call_settings)) {
                    //interstitial ad
                    AdManager.trackAction(context, activity)
                    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
                SettingsDivider()
                SettingsIconRow(Icons.Default.DarkMode, Color(0xFF37474F), stringResource(R.string.dark_mode)) {
                    //interstitial ad
                    AdManager.trackAction(context, activity)
                    showThemeDialog = true
                }
                SettingsDivider()
                SettingsIconRow(Icons.Default.Palette, Color(0xFF8E24AA), stringResource(R.string.accent_color)) {
                    //interstitial ad
                    AdManager.trackAction(context, activity)
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
                SettingsIconRow(Icons.Default.Accessibility, Color(0xFF1A6B45), stringResource(R.string.accessibility)) {
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

    // LANGUAGE DIALOG
    if (showLanguageDialog) {
        Dialog(
            onDismissRequest = { showLanguageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ── Header ──────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.change_language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Language List ────────────────────────────────────────
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(AppLanguage.entries.toList()) { language ->
                            val isSelected = selectedLanguage == language
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF2C2C2E),
                                animationSpec = tween(150)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                                    .clickable {
                                        onboardingViewModel.saveLanguage(language)
                                        LocaleHelper.saveLanguage(context, language.code)
                                        showLanguageDialog = false
                                        // ── Set flag to suppress the app-open ad on recreate ──
                                        context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE)
                                            .edit().putBoolean("skip_app_open_ad", true).apply()
                                        // ── Show interstitial first, recreate only after dismiss ──
                                        AdManager.immediateInterstitialAd(activity) {
                                            activity?.recreate()
                                        }
//                                        activity?.recreate()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) Color(0xFF34C759) else Color(0xFF8E8E93),
                                            shape = CircleShape
                                        )
                                        .background(
                                            if (isSelected) Color(0xFF34C759) else Color.Transparent
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "${language.flag} ${language.displayName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    NativeAdCard(
                        modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                    )
                }
            }
        }
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
    val context = LocalContext.current
    val activity = context as? Activity

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            }
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