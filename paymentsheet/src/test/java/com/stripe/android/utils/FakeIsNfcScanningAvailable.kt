package com.stripe.android.utils

import com.stripe.android.common.nfcscan.IsNfcScanningAvailable
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata

internal class FakeIsNfcScanningAvailable(
    private val result: Boolean,
) : IsNfcScanningAvailable {
    override fun get(metadata: PaymentMethodMetadata): Boolean = result
}
