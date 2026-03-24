package com.stripe.android.checkout

import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutStateFactory {
    fun create(
        key: String = "test_key",
        configuration: Checkout.Configuration.State = Checkout.Configuration().build(),
        checkoutSessionResponse: CheckoutSessionResponse = CheckoutSessionResponseFactory.create(),
        shippingName: String? = null,
        billingName: String? = null,
        shippingPhoneNumber: String? = null,
        billingPhoneNumber: String? = null,
        shippingAddress: Address.State? = null,
        billingAddress: Address.State? = null,
        integrationLaunched: Boolean = false,
    ): Checkout.State {
        return Checkout.State(
            InternalState(
                key = key,
                configuration = configuration,
                checkoutSessionResponse = checkoutSessionResponse,
                shippingName = shippingName,
                billingName = billingName,
                shippingPhoneNumber = shippingPhoneNumber,
                billingPhoneNumber = billingPhoneNumber,
                shippingAddress = shippingAddress,
                billingAddress = billingAddress,
                integrationLaunched = integrationLaunched,
            ),
        )
    }
}
