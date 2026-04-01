package com.stripe.android.uicore.image

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalStripeImageLoader = staticCompositionLocalOf<StripeImageLoader> {
    error("No ImageLoader provided")
}
