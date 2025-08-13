package com.stripe.android.paymentsheet

import com.stripe.android.ui.core.IsStripeCardScanAvailable

class FakeIsStripeCardScanAvailable(
    private val value: Boolean = true
) : IsStripeCardScanAvailable {
    override fun invoke() = value
}