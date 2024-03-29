package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags

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
