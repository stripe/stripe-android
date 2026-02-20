package com.stripe.android.common.taptoadd

import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed interface TapToAddNextStep {
    data object Complete : TapToAddNextStep

    data class Continue(val paymentSelection: PaymentSelection.Saved) : TapToAddNextStep

    data class Canceled(
        val paymentSelection: PaymentSelection.Saved?
    ) : TapToAddNextStep
}
