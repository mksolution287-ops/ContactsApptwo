package com.callerinfocom.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.callerinfocom.R
import com.callerinfocom.utils.AdManager
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * App-install style native ad (landscape media, AdChoices top-right).
 *
 * The ad itself is preloaded by [AdManager] into [AdManager.NATIVE_KEY_APP_INSTALL]
 * at app start, so it normally shows instantly instead of loading when this
 * composable first appears.
 */
@Composable
fun AppInstallNativeAdCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adsEnabled by AdManager.adsEnabled.collectAsState()
    val nativeEnabled by AdManager.nativeEnabled.collectAsState()
    val adReady by AdManager.nativeAdReadyFlow(AdManager.NATIVE_KEY_APP_INSTALL).collectAsState()

    if (!adsEnabled || !nativeEnabled) return

    // No-op when already preloaded; queued automatically if ads aren't ready yet.
    LaunchedEffect(Unit) {
        AdManager.preloadNativeAd(context, AdManager.NATIVE_KEY_APP_INSTALL)
    }

    // When this card leaves the screen, replace the (now seen) ad with a fresh
    // preloaded one so the next visit shows a new ad instantly.
    DisposableEffect(Unit) {
        onDispose {
            AdManager.recycleNativeAd(context, AdManager.NATIVE_KEY_APP_INSTALL)
        }
    }

    AnimatedVisibility(
        visible = adReady,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { ctx ->
                val adView = LayoutInflater.from(ctx)
                    .inflate(R.layout.native_app_install_ad_view, null) as NativeAdView

                adView.mediaView        = adView.findViewById(R.id.ad_media)
                adView.headlineView     = adView.findViewById(R.id.ad_headline)
                adView.bodyView         = adView.findViewById(R.id.ad_body)
                adView.iconView         = adView.findViewById(R.id.ad_app_icon)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

//                adView.storeView        = adView.findViewById(R.id.ad_store)
//                adView.priceView        = adView.findViewById(R.id.ad_price)
//                adView.starRatingView   = adView.findViewById(R.id.ad_stars)

                adView
            },
            update = { adView ->
                val ad = AdManager.getNativeAd(AdManager.NATIVE_KEY_APP_INSTALL)
                    ?: return@AndroidView

                // Media
                adView.mediaView?.mediaContent = ad.mediaContent

                // Text fields + theme-aware colors
                val textColor = ContextCompat.getColor(context, R.color.native_ad_text_primary)

                (adView.headlineView as? TextView)?.apply {
                    text = ad.headline
                    setTextColor(textColor)
                }
                (adView.bodyView as? TextView)?.apply {
                    text = ad.body
                    setTextColor(textColor)
                }
                (adView.callToActionView as? Button)?.text = ad.callToAction

                // Attribution label — must always be visible per AdMob policy
                adView.findViewById<TextView>(R.id.ad_attribution)?.visibility = View.VISIBLE

                // Icon
                ad.icon?.drawable?.let {
                    (adView.iconView as? ImageView)?.setImageDrawable(it)
                }

                // Store
                val storeView = adView.storeView as? TextView
                if (ad.store != null) {
                    storeView?.text = ad.store
                    storeView?.visibility = View.VISIBLE
                } else {
                    storeView?.visibility = View.GONE
                }

                // Price
                val priceView = adView.priceView as? TextView
                if (ad.price != null) {
                    priceView?.text = ad.price
                    priceView?.visibility = View.VISIBLE
                } else {
                    priceView?.visibility = View.GONE
                }

                // Required — registers all views for click tracking
                adView.setNativeAd(ad)

                // Tell AdManager this ad has been rendered (so it gets recycled later)
                AdManager.markNativeAdShown(AdManager.NATIVE_KEY_APP_INSTALL)
            }
        )
    }
}