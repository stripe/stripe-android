package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    // Add any feature flags here
    val nativeLinkEnabled = FeatureFlag("Native Link")
    val nativeLinkAttestationEnabled = FeatureFlag("Native Link Attestation")
    val instantDebitsIncentives = FeatureFlag("Instant Bank Payments Incentives")
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

    val value: Flag
        get() {
            if (BuildConfig.DEBUG.not()) {
                return Flag.NotSet
            }
            return when (overrideEnabledValue) {
                true -> Flag.Enabled
                false -> Flag.Disabled
                null -> Flag.NotSet
            }
        }

    fun setEnabled(isEnabled: Boolean) {
        overrideEnabledValue = isEnabled
    }

    fun reset() {
        overrideEnabledValue = null
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Flag {
        data object Enabled : Flag
        data object Disabled : Flag
        data object NotSet : Flag
    }
}
