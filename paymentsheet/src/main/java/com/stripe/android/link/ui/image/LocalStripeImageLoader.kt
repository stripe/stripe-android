package com.stripe.android.link.ui.image

import androidx.compose.runtime.staticCompositionLocalOf
import com.stripe.android.uicore.image.StripeImageLoader

internal val LocalStripeImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}
