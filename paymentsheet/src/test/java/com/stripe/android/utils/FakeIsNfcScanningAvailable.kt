package com.stripe.android.utils

import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.ElementsSession

internal class FakeIsNfcScanningAvailable(
    private val result: Boolean,
) : IsNfcScanningAvailable {
    override fun get(
        elementsSession: ElementsSession,
        customerMetadata: CustomerMetadata?,
    ): Boolean = result
}
