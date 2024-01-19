package com.stripe.android.utils

import androidx.annotation.RestrictTo
import com.stripe.android.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    val customerSheetACHv2 = FeatureFlag()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag {

    private var overrideInTest: Boolean? = null

    val isEnabled: Boolean
        get() = if (BuildConfig.DEBUG) {
            overrideInTest ?: true
        } else {
            false
        }

    fun setEnabled(isEnabled: Boolean) {
        overrideInTest = isEnabled
    }

    fun reset() {
        overrideInTest = null
    }
}
