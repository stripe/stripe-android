package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.model.PaymentMethod
import kotlinx.parcelize.Parcelize

/**
 * Handler to be used to confirm payment with an external payment method.
 *
 * To learn more about external payment methods, see https://docs.stripe.com/payments/external-payment-methods?platform=android.
 */
interface ExternalPaymentMethodConfirmHandler {

    /**
     * Called when a user confirms payment or setup with an external payment method.
     *
     * On completion, this should call [PaymentSheet.onExternalPaymentMethodResult] with the result of the attempt to confirm payment using the given external payment method.
     *
     * @param externalPaymentMethodType The external payment method to confirm payment with
     * @param billingDetails Any billing details you've configured PaymentSheet to collect
     */
    fun confirmExternalPaymentMethod(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails?,
    )
}
