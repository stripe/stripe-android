package com.stripe.android.uicore.image

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface ImageOptimizer {
    fun optimize(url: String, width: Int): String
}
