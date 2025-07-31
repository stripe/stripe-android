package com.stripe.android.paymentelement

import com.stripe.android.elements.payment.CustomPaymentMethod
import com.stripe.android.model.PaymentMethod

/**
 * Handler to be used to confirm payment with a custom payment method.
 *
 * To learn more about custom payment methods, see "docs_url"
 */
@ExperimentalCustomPaymentMethodsApi
fun interface ConfirmCustomPaymentMethodCallback {

    /**
     * Called when a user confirms payment or setup with a custom payment method.
     *
     * On completion, this should call [CustomPaymentMethodResultHandler.handleCustomPaymentMethodResult] with the
     * result of the custom payment method's confirmation.
     *
     * @param customPaymentMethod The custom payment method to confirm payment with
     * @param billingDetails Any billing details you've configured Payment Element to collect
     */
    fun onConfirmCustomPaymentMethod(
        customPaymentMethod: CustomPaymentMethod,
        billingDetails: PaymentMethod.BillingDetails,
    )
}
