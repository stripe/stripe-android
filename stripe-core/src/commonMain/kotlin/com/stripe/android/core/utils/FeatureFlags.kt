package com.stripe.android.core.utils

import androidx.annotation.RestrictTo

internal expect val isDebugBuild: Boolean

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    // Add any feature flags here
    val nativeLinkEnabled = FeatureFlag("Native Link")
    val nativeLinkAttestationEnabled = FeatureFlag("Native Link Attestation")
    val instantDebitsIncentives = FeatureFlag("Instant Bank Payments Incentives")
    val financialConnectionsFullSdkUnavailable = FeatureFlag("FC Full SDK Unavailable")
    val forceEnableNativeFinancialConnections = FeatureFlag("Force enable FC Native")
    val showInlineOtpInWalletButtons = FeatureFlag("Show Inline Signup in Wallet Buttons")
    val forceEnableLinkPaymentSelectionHint = FeatureFlag("Link: Force enable payment selection hint")
    val forceLinkWebAuth = FeatureFlag("Link: Force web auth")
    val enableAttestationOnIntentConfirmation = FeatureFlag("Enable Attestation on Intent Confirmation")
    val enableTapToAdd = FeatureFlag("Enable Tap to Add")
    val enableCardArt = FeatureFlag("Enable Card Art")
    val enableKlarnaFormRemoval = FeatureFlag("Remove forms from Klarna")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag(
    val name: String,
) {

    private var overrideEnabledValue: Boolean? = null

    val isEnabled: Boolean
        get() = if (isDebugBuild) {
            overrideEnabledValue ?: false
        } else {
            false
        }

    val value: Flag
        get() {
            if (!isDebugBuild) {
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
