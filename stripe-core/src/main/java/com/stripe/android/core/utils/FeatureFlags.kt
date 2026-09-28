package com.stripe.android.core.utils

import androidx.annotation.RestrictTo
import com.stripe.android.core.BuildConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object FeatureFlags {
    private val allFlags = mutableListOf<FeatureFlag>()

    // Add any feature flags here
    val nativeLinkEnabled = create("Native Link")
    val nativeLinkAttestationEnabled = create("Native Link Attestation")
    val instantDebitsIncentives = create("Instant Bank Payments Incentives")
    val financialConnectionsFullSdkUnavailable = create("FC Full SDK Unavailable")
    val forceEnableNativeFinancialConnections = create("Force enable FC Native")
    val showInlineOtpInWalletButtons = create("Show Inline Signup in Wallet Buttons")
    val allowNoExistingPaymentMethodForGooglePay = create(
        "Allow no existing payment method required to use Google Pay"
    )
    val forceEnableLinkPaymentSelectionHint = create("Link: Force enable payment selection hint")
    val forceLinkWebAuth = create("Link: Force web auth")
    val forceOnelink = create("Link: Force Onelink brand")
    val forceOnelinkConsumer = create("Link: Force Onelink consumer")
    val enableKlarnaFormRemoval = create("Remove forms from Klarna")
    val disableNfcScanning = create("Disable NFC Scanning")
    val disableNfcScanningSecurity = create("Disable NFC Scanning Security")
    val disablePassiveCaptchaWarmup = create("Disable Passive Captcha Warm-Up")
    val forceTapToAddWithTerminal = create("Tap to Add: Force Terminal integration to be available")

    fun reset() {
        allFlags.forEach(FeatureFlag::reset)
    }

    private fun create(name: String): FeatureFlag {
        return FeatureFlag(name).also(allFlags::add)
    }
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
