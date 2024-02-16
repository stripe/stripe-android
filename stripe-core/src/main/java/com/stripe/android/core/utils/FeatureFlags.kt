package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    val customerSheetACHv2 = FeatureFlag(enabledInDebugMode = true)
    val forceAuthFlowV3 = FeatureFlag(enabledInDebugMode = false)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag(
    private val enabledInDebugMode: Boolean,
) {

    private var overrideInTest: Boolean? = null

    val isEnabled: Boolean
        get() = if (BuildConfig.DEBUG) {
            overrideInTest ?: enabledInDebugMode
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
