package com.stripe.android.common.spms

import com.stripe.android.paymentsheet.model.PaymentSelection

internal fun PaymentSelection.Saved.withLinkState(
    state: SavedPaymentMethodLinkFormHelper.State,
): PaymentSelection.Saved {
    return when (state) {
        is SavedPaymentMethodLinkFormHelper.State.Unused,
        is SavedPaymentMethodLinkFormHelper.State.Incomplete -> copy(linkInput = null)
        is SavedPaymentMethodLinkFormHelper.State.Complete -> copy(linkInput = state.userInput)
    }
}
