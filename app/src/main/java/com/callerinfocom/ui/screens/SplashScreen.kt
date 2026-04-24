package com.callerinfocom.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
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
import com.callerinfocom.R

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // ── Animation values ───────────────────────────────────────────────────
    val iconScale   = remember { Animatable(0.4f) }
    val iconAlpha   = remember { Animatable(0f) }
    val textAlpha   = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }
    val progress    = remember { Animatable(0f) }        // ← New: Progress animation

    LaunchedEffect(Unit) {
        // Icon pops in
        iconAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
        iconScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        delay(100)

        // Text fades + slides up
        textAlpha.animateTo(1f, tween(350))
        textOffsetY.animateTo(0f, tween(350, easing = FastOutSlowInEasing))

        // Start progress bar animation
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2200,           // Same duration as the final delay
                easing = LinearEasing
            )
        )

        // Hold and then finish
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
                text = stringResource(R.string.contacts_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .graphicsLayer { translationY = textOffsetY.value }
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = stringResource(R.string.contacts_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .graphicsLayer { translationY = textOffsetY.value }
            )
        }

        // Linear Progress Bar at the bottom
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 48.dp)
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    }
}