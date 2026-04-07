package com.stripe.android.uicore.image

import androidx.annotation.RestrictTo
import androidx.compose.runtime.staticCompositionLocalOf

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
val LocalImageOptimizer = staticCompositionLocalOf<ImageOptimizer> {
    error("No ImageOptimizer provided")
}
