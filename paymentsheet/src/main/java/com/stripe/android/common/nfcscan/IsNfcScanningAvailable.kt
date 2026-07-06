package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.hardware.NfcHardwareDelegate
import com.stripe.android.common.nfcscan.security.IsDeviceSecureForNfc
import com.stripe.android.core.utils.FeatureFlags.enableNfcScanning
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.paymentsheet.state.TapToAddAvailabilityFactory
import javax.inject.Inject

internal interface IsNfcScanningAvailable {
    fun get(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?
    ): Boolean
}

internal class DefaultIsNfcScanningAvailable @Inject constructor(
    val isDeviceSecureForNfc: IsDeviceSecureForNfc,
    val tapToAddAvailabilityFactory: TapToAddAvailabilityFactory,
    val nfcHardwareDelegate: NfcHardwareDelegate,
) : IsNfcScanningAvailable {
    override fun get(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?
    ): Boolean {
        return enableNfcScanning.isEnabled &&
            !tapToAddAvailabilityFactory.isAvailable(elementsSession, customerMetadata) &&
            isDeviceSecureForNfc.get() &&
            nfcHardwareDelegate.isAvailable()
    }
}
