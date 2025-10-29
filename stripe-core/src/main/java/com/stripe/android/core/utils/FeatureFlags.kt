package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

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
    val cardScanGooglePayMigration = FeatureFlag("Use Google Payment Card Recognition API for Card Scan")
    val enablePassiveCaptcha = FeatureFlag("Enable Passive Captcha")
    val forceLinkWebAuth = FeatureFlag("Link: Force web auth")
    val enablePromptPay = FeatureFlag("Enable PromptPay")
    val enableAttestationOnIntentConfirmation = FeatureFlag("Enable Attestation on Intent Confirmation")
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class FeatureFlag(
    val name: String,
) {

    private var overrideEnabledValue: Boolean? = null

    val isEnabled: Boolean
        get() =  overrideEnabledValue ?: false

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
