package com.callerinfocom.ui.uninstall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.callerinfocom.R

private const val DISPLAY_DURATION_MS = 3000L  // 3 seconds visible
private const val FADE_DURATION_MS    = 400

@Composable
fun UninstallThankYouScreen(
    onTimeout: () -> Unit
) {
    // Fade-in on entry, fade-out just before navigating away
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(FADE_DURATION_MS),
        label         = "thankYouAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true                          // trigger fade-in
        delay(DISPLAY_DURATION_MS.toLong())
        visible = false                         // trigger fade-out
        delay(FADE_DURATION_MS.toLong())
        onTimeout()                             // then open system settings
    }

    Box(
        modifier          = Modifier
            .fillMaxSize()
            .alpha(alpha),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier.padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.thankyou),
                contentDescription = "Thankyou Image"
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text       = stringResource(R.string.thankyou_title),
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text      = stringResource(R.string.thankyou_subtitle),
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}