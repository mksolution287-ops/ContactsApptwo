package com.contactsapptwomktech.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Swipe
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.contactsapptwomktech.R
import kotlinx.coroutines.launch


private data class OnboardingPage(
    val icon        : ImageVector,
    val accentIcon  : ImageVector? = null,
    val title       : Int,
    val subtitle    : Int,
    val description : Int
)

private val pages = listOf(
    OnboardingPage(
        icon        = Icons.Outlined.People,
        title       = R.string.onboarding_1_title,
        subtitle    = R.string.onboarding_1_subtitle,
        description = R.string.onboarding_1_desc
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Dialpad,
        accentIcon  = Icons.Outlined.Search,
        title       = R.string.onboarding_2_title,
        subtitle    = R.string.onboarding_2_subtitle,
        description = R.string.onboarding_2_desc
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Swipe,
        accentIcon  = Icons.Outlined.Search,
        title       = R.string.onboarding_4_title,
        subtitle    = R.string.onboarding_4_subtitle,
        description = R.string.onboarding_4_desc
    ),
    OnboardingPage(
        icon        = Icons.Outlined.Star,
        accentIcon  = Icons.Outlined.Call,
        title       = R.string.onboarding_3_title,
        subtitle    = R.string.onboarding_3_subtitle,
        description = R.string.onboarding_3_desc
    )
)

/**
 * Three-page onboarding pager shown once after language + permissions are done.
 *
 * @param onFinished Called when the user taps "Get Started" on the last page.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope      = rememberCoroutineScope()

    // Fade in the whole screen
    val screenAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        screenAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(screenAlpha.value)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Skip button (top-right) ────────────────────────────────────────
        item {
            Box(
                modifier         = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    onClick  = onFinished,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text  = stringResource(R.string.skip_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ── Pager ──────────────────────────────────────────────────────────
        item {
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.height(480.dp)
            ) { pageIndex ->
                OnboardingPage(page = pages[pageIndex])
            }
        }

        // ── Page indicator dots ────────────────────────────────────────────
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier              = Modifier.padding(vertical = 20.dp)
            ) {
                pages.indices.forEach { index ->
                    PageDot(isActive = index == pagerState.currentPage)
                }
            }
        }

        // ── Bottom action button ───────────────────────────────────────────
        item {
            val isLastPage = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                shape   = RoundedCornerShape(14.dp),
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(54.dp)
            ) {
                Text(
                    text       = if (isLastPage) stringResource(R.string.get_started) else stringResource(R.string.next),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Single onboarding page
// ---------------------------------------------------------------------------

@Composable
private fun OnboardingPage(page: OnboardingPage) {
    androidx.compose.foundation.layout.Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon illustration
        Box(
            modifier         = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = page.icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(64.dp)
            )
            // Optional accent icon in the corner
            if (page.accentIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = page.accentIcon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onPrimary,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text       = stringResource(page.title),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onBackground,
            textAlign  = TextAlign.Center
        )
        Text(
            text       = stringResource(page.subtitle),
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text      = stringResource(page.description),
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------
// Animated page indicator dot
// ---------------------------------------------------------------------------

@Composable
private fun PageDot(isActive: Boolean) {
    val width by animateDpAsState(
        targetValue   = if (isActive) 24.dp else 8.dp,
        animationSpec = tween(250),
        label         = "dot_width"
    )
    val color by animateColorAsState(
        targetValue   = if (isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        animationSpec = tween(250),
        label         = "dot_color"
    )
    Box(
        modifier = Modifier
            .height(8.dp)
            .width(width)
            .clip(CircleShape)
            .background(color)
    )
}