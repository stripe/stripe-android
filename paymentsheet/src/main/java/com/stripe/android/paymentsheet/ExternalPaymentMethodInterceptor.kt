package com.stripe.android.paymentsheet

import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.paymentlauncher.PaymentResult
import java.lang.IllegalStateException

internal object ExternalPaymentMethodInterceptor {

    var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null

    fun intercept(
        externalPaymentMethodType: String,
        billingDetails: PaymentMethod.BillingDetails?,
        onPaymentResult: (PaymentResult) -> Unit,
        integrationType: ExternalPaymentMethodResultHandler.IntegrationType,
    ) {
        ExternalPaymentMethodResultHandler.integrationType = integrationType
        val externalPaymentMethodConfirmHandler = this.externalPaymentMethodConfirmHandler
        if (externalPaymentMethodConfirmHandler == null) {
            onPaymentResult(
                PaymentResult.Failed(
                    throwable = IllegalStateException(
                        "externalPaymentMethodConfirmHandler is null." +
                            " Cannot process payment for payment selection: $externalPaymentMethodType"
                    )
                )
            )
        } else {
            externalPaymentMethodConfirmHandler.confirmExternalPaymentMethod(
                externalPaymentMethodType = externalPaymentMethodType,
                billingDetails = billingDetails ?: PaymentMethod.BillingDetails()
            )
        }
    }
}
