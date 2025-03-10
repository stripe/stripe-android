package com.stripe.android.paymentelement

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Handler to be used to confirm payment with a custom payment method.
 *
 * To learn more about custom payment methods, see "docs_url"
 */
@ExperimentalCustomPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CustomPaymentMethodConfirmHandler {

    /**
     * Called when a user confirms payment or setup with a custom payment method.
     *
     * On completion, this should call [CustomPaymentMethodResultHandler.onCustomPaymentMethodResult] with the
     * result of the custom payment method's confirmation.
     *
     * @param customPaymentMethodType The custom payment method to confirm payment with
     * @param billingDetails Any billing details you've configured Payment Element to collect
     */
    fun confirmCustomPaymentMethod(
        customPaymentMethodType: PaymentSheet.CustomPaymentMethodConfiguration.CustomPaymentMethodType,
        billingDetails: PaymentMethod.BillingDetails,
    )
}
