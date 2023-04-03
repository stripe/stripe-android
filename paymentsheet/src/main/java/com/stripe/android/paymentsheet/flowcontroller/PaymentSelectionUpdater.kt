package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentSheetState

internal object PaymentSelectionUpdater {

    fun process(
        currentSelection: PaymentSelection?,
        newState: PaymentSheetState.Full,
    ): PaymentSelection? {
        return currentSelection?.takeIf { it.canBeUsedIn(newState) } ?: newState.paymentSelection
    }
}

private fun PaymentSelection.canBeUsedIn(state: PaymentSheetState.Full): Boolean {
    val allowedTypes = state.stripeIntent.paymentMethodTypes

    return when (this) {
        is PaymentSelection.New -> {
            paymentMethodCreateParams.typeCode in allowedTypes
        }
        is PaymentSelection.Saved -> {
            paymentMethod.type?.code in allowedTypes && paymentMethod in state.customerPaymentMethods
        }
        is PaymentSelection.GooglePay -> {
            state.isGooglePayReady
        }
        is PaymentSelection.Link -> {
            state.linkState != null
        }
    }
}
