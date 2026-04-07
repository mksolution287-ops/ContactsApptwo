package com.contactsapptwomktech.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.contactsapptwomktech.R

/**
 * Manual splash screen shown only on the very first launch (before language is chosen).
 * Once the user has already selected a language, this screen is bypassed entirely.
 *
 * @param onFinished Called when the animation completes and we should navigate forward.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── Animation values ───────────────────────────────────────────────────
    val iconScale   = remember { Animatable(0.4f) }
    val iconAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        // Icon pops in
        iconAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        iconScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(100)
        // Text fades + slides up
        textAlpha.animateTo(1f, tween(350))
        textOffsetY.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        // Hold — long enough for the bottom to fully load
        delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // App icon circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.contacts2logo),
                    contentDescription = "App logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(28.dp))

            // App name
            Text(
                text       = stringResource(R.string.contacts_title),
                style      = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .alpha(textAlpha.value)
                    .graphicsLayer { translationY = textOffsetY.value }  // ✅ fixed: use translationY lambda
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = stringResource(R.string.contacts_subtitle),
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .alpha(textAlpha.value)
                    .graphicsLayer { translationY = textOffsetY.value }  // ✅ fixed: use translationY lambda
            )
        }
    }
}