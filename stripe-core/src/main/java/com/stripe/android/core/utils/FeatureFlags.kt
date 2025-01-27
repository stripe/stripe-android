package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    // Add any feature flags here
    val nativeLinkEnabled = FeatureFlag("Native Link")
    val nativeLinkAttestationEnabled = FeatureFlag("Native Link Attestation")
    val instantDebitsIncentives = FeatureFlag("Instant Bank Payments Incentives")
    val enableDefaultPaymentMethods = FeatureFlag("Enable Default Payment Methods")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag(
    val name: String,
) {

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
