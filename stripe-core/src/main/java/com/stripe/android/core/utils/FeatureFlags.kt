package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    // Add any feature flags here
    val nativeLinkEnabled = FeatureFlag()
    val instantDebitsBillingDetails = FeatureFlag()
    val useNewUpdateCardScreen = FeatureFlag()
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag {

    private var overrideEnabledValue: Boolean? = null

    val isEnabled: Boolean
        get() = if (BuildConfig.DEBUG) {
            overrideEnabledValue ?: false
        } else {
            false
        }

    fun setEnabled(isEnabled: Boolean) {
        overrideEnabledValue = isEnabled
    }

    fun reset() {
        overrideEnabledValue = null
    }
}
