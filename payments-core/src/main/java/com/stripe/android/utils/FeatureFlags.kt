package com.stripe.android.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    val cardBrandChoice = FeatureFlag()
    val customerSheetACHv2 = FeatureFlag()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag {

    private var overrideInTest: Boolean? = null

    val isEnabled: Boolean
        get() = overrideInTest ?: true

    fun setEnabled(isEnabled: Boolean) {
        overrideInTest = isEnabled
    }

    fun reset() {
        overrideInTest = null
    }
}
