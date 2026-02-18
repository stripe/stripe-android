package com.stripe.android.uicore.image

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalStripeImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}
