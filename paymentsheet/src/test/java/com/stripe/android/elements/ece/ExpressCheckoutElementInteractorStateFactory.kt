@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.elements.ece

import com.stripe.android.elements.ExpressCheckoutElement.Configuration.GooglePayConfiguration
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.CheckoutSessionPreview

internal object ExpressCheckoutElementInteractorStateFactory {
    private val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
        availableWallets = emptyList(),
    )

    fun create(
        expressButtons: List<ExpressButton> = listOf(
            ExpressButton.Link.create(
                paymentMethodMetadata = paymentMethodMetadata,
                linkAccountInfo = LinkAccountUpdate.Value(null),
            ),
            ExpressButton.GooglePay.create(
                paymentMethodMetadata = paymentMethodMetadata,
                googlePayConfiguration = GooglePayConfiguration().build(),
                shippingAddressRequired = false,
            ),
        ),
    ): ExpressCheckoutElementInteractor.State {
        return ExpressCheckoutElementInteractor.State(
            expressButtons = expressButtons,
        )
    }
}
