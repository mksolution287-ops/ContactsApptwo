package com.callerinfocom.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.callerinfocom.utils.AdManager

/**
 * Banner ad. The AdView is preloaded and cached by [AdManager], so it's
 * usually already loaded by the time a screen shows it. This composable only
 * attaches/detaches that cached view.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adsReady by AdManager.adsReady.collectAsState()
    val adsEnabled by AdManager.adsEnabled.collectAsState()
    val bannerEnabled by AdManager.bannerEnabled.collectAsState()

    // Not ready yet (SDK / Remote Config still starting) or switched off.
    // Recomposes automatically the moment adsReady flips to true.
    if (!adsReady || !adsEnabled || !bannerEnabled) return

    val adView = remember(adsReady, bannerEnabled) {
        AdManager.acquireBanner(context)
    } ?: return

    // Drop the Activity reference + pause when leaving; the AdView stays
    // cached in AdManager for the next screen.
    DisposableEffect(adView) {
        onDispose { AdManager.releaseBanner() }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView }
    )
}