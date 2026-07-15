package com.stripe.android.checkout.ece

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory

internal object ExpressCheckoutElementInteractorStateFactory {
    private val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
        availableWallets = emptyList(),
    )

    fun create(
        expressButtons: List<ExpressButton> = listOf(
            ExpressButton.Link.create(
                paymentMethodMetadata = paymentMethodMetadata,
            ),
            ExpressButton.GooglePay.create(
                paymentMethodMetadata = paymentMethodMetadata,
            ),
        ),
    ): ExpressCheckoutElementInteractor.State {
        return ExpressCheckoutElementInteractor.State(
            expressButtons = expressButtons,
        )
    }
}
