package com.contactsapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.contactsapp.data.viewmodel.SettingsViewModel
import com.contactsapp.ui.theme.AppAccentColor
import com.contactsapp.ui.theme.AppThemeMode
import com.contactsapp.R
import com.contactsapp.data.model.AppLanguage
import com.contactsapp.data.viewmodel.OnboardingViewModel
import com.contactsapp.ui.Screen
import com.contactsapp.utils.LocaleHelper
import android.provider.Settings
import androidx.compose.material.icons.filled.ArrowForwardIos


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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            SettingsGroup(title = stringResource(R.string.default_apps)) {
                SettingsClickableRow(
                    title = stringResource(R.string.default_apps),
                    subtitle = stringResource(R.string.default_apps_desc)
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    )
                }
            }

            // ── Theme Mode ──────────────────────────────────────────────
            SettingsGroup(title = stringResource(R.string.theme)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val selected = settings.themeMode == mode
                        FilterChip(
                            selected = selected,
                            onClick  = { viewModel.setThemeMode(mode) },
                            label    = {
                                Text(text = mode.getLabel())
                            },
                            modifier = Modifier.weight(1f),
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            // ── Accent Color ────────────────────────────────────────────
            SettingsGroup(title = stringResource(R.string.accent_color)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppAccentColor.entries.forEach { accent ->
                        val selected = settings.accentColor == accent && !settings.useDynamicColor
                    item{
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accent.seed)
                                .then(
                                    if (selected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable {
                                    viewModel.setAccentColor(accent)
                                    viewModel.setDynamicColor(false)
                                }
                        ) {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Color name label
                Text(
                    text  = if (settings.useDynamicColor) "Dynamic"
                    else settings.accentColor.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SettingsGroup(title = stringResource(R.string.language)) {

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.entries.forEach { language ->
                        val selected = selectedLanguage == language

                        item {
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onboardingViewModel.saveLanguage(language)

                                    LocaleHelper.saveLanguage(context, language.code)

                                    //instantly apply
                                    (context as android.app.Activity).recreate()
                                },
                                label = {
                                    Text("${language.flag} ${language.nativeName}")
                                }
                            )
                        }
                    }
                }
            }

            //Sim Preferences
            SettingsGroup(title = stringResource(R.string.sim_settings)) {
                SettingsClickableRow(
                    title = stringResource(R.string.sim_settings),
                    subtitle = stringResource(R.string.sim_settings_desc)
                ) {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                    } else {
                        Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                    }
                    context.startActivity(intent)
                }
            }


            // ── Material You ────────────────────────────────────────────
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                SettingsGroup(title = "Material You") {
//                    SettingsToggleRow(
//                        title    = "Dynamic color",
//                        subtitle = "Pull colors from your wallpaper (Android 12+)",
//                        checked  = settings.useDynamicColor,
//                        onCheckedChange = { viewModel.setDynamicColor(it) }
//                    )
//                }
//            }
//
//            // ── Display ─────────────────────────────────────────────────
//            SettingsGroup(title = "Display") {
//                SettingsToggleRow(
//                    title    = "AMOLED black",
//                    subtitle = "Pure black background in dark mode",
//                    checked  = settings.useAmoledBlack,
//                    onCheckedChange = { viewModel.setAmoledBlack(it) }
//                )
//            }
//
//            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row{
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null
            )
        }

    }
}

// ── Reusable composables ───────────────────────────────────────────────────

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
            color      = MaterialTheme.colorScheme.primary
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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