package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod

/**
 * Handler to be used to confirm payment with an external payment method.
 *
 * To learn more about external payment methods, see
 * https://docs.stripe.com/payments/external-payment-methods?platform=android.
 */
fun interface ExternalPaymentMethodConfirmHandler {

    /**
     * Called when a user confirms payment or setup with an external payment method.
     *
     * On completion, this should call [ExternalPaymentMethodResultHandler.onExternalPaymentMethodResult] with the
     * result of the external payment method's confirmation.
     *
     * @param externalPaymentMethodType The external payment method to confirm payment with
     * @param billingDetails Any billing details you've configured PaymentSheet to collect
     */
    fun confirmExternalPaymentMethod(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails,
    )
}
