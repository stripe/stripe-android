package com.stripe.android.elements.payment

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.elements.AddressDetails
import com.stripe.android.model.PaymentMethod

@SharedPaymentTokenSessionPreview
fun interface PreparePaymentMethodHandler {
    /**
     * Prepares a payment method and shipping address to be passed through an external provider
     *
     * After the call to this method, the session will be completed in Payment Element successfully.
     *
     * @param paymentMethod The [PaymentMethod] representing the customer's payment details. If your
     * server needs the payment method, send [PaymentMethod.id] to your server and have it fetch the
     * [PaymentMethod] object. Otherwise, you can ignore this. Don't send other properties besides
     * the ID to your server.
     * @param shippingAddress The [AddressDetails] representing the customer's shipping details.
     */
    suspend fun onPreparePaymentMethod(
        paymentMethod: PaymentMethod,
        shippingAddress: AddressDetails?,
    )
}
