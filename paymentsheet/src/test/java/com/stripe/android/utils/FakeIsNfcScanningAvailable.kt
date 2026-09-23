package com.stripe.android.utils

import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.common.nfcscan.NfcScanningAvailability
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeIsNfcScanningAvailable(
    private val result: NfcScanningAvailability,
) : IsNfcScanningAvailable {
    constructor(result: Boolean) : this(
        result = if (result) {
            NfcScanningAvailability.Available(shouldBePrimaryScanningOption = true)
        } else {
            NfcScanningAvailability.Unavailable
        }
    )

    override fun get(metadata: PaymentMethodMetadata): NfcScanningAvailability = result
}
