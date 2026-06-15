package com.stripe.android.checkout

import android.content.Context
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory

@OptIn(CheckoutSessionPreview::class)
internal object CheckoutStateFactory {
    const val DEFAULT_KEY = "test_key"

    fun createCheckout(context: Context): Checkout {
        return Checkout.createWithState(
            context = context,
            state = create(),
        )
    }

    fun create(
        key: String = DEFAULT_KEY,
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
