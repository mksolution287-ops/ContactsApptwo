package com.callerinfocom.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.callerinfocom.data.model.AppLanguage
import com.callerinfocom.ui.components.AppInstallNativeAdCard
import com.callerinfocom.ui.components.NativeAdCard

@Composable
fun LanguageSelectScreen(
    initialLanguage: AppLanguage? = null,
    onLanguageChosen: (AppLanguage) -> Unit
) {
    var selected by rememberSaveable {
        mutableStateOf(initialLanguage ?: AppLanguage.SYSTEM_DEFAULT)
    }

    val languages = AppLanguage.entries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Header ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Select Language",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = { onLanguageChosen(selected) }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Language List ───────────────────────────────────────
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(languages) { lang ->

                val displayName = if (lang == AppLanguage.SYSTEM_DEFAULT) {
                    val systemLang = Locale.getDefault().displayLanguage
                    "🌐 System default ($systemLang)"
                } else {
                    "${lang.flag} ${lang.displayName}"
                }

                val isSelected = selected == lang

                LanguageTile(
                    displayName = displayName,
                    isSelected = isSelected,
                    onClick = {
                        selected = lang
                    }
                )
            }
        }
//        NativeAdCard(modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp).navigationBarsPadding())
        AppInstallNativeAdCard(modifier = Modifier.requiredWidth(LocalConfiguration.current.screenWidthDp.dp).navigationBarsPadding())
    }
}

// ─────────────────────────────────────────────────────────────
// Language Tile
// ─────────────────────────────────────────────────────────────
@Composable
private fun LanguageTile(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF2C2C2E),
        animationSpec = tween(150)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Radio button
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

        Spacer(Modifier.width(16.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}