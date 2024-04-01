package com.stripe.android.link.ui

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LinkUi {
    /**
     * Enable 2024 brand.
     * Delete this after rollout. (~May 2024)
     */
    @Volatile
    var useNewBrand: Boolean = true
}
