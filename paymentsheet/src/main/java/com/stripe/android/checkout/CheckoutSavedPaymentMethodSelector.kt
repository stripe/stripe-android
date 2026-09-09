package com.stripe.android.checkout

import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal interface CheckoutSavedPaymentMethodSelector {
    suspend fun select(selection: PaymentSelection.Saved): Result<Unit>
}

@OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)
internal class DefaultCheckoutSavedPaymentMethodSelector @Inject constructor(
    private val checkoutController: CheckoutController,
) : CheckoutSavedPaymentMethodSelector {
    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        return checkoutController.selectSavedPaymentMethod(selection)
    }
}
