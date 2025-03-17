package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentelement.CustomPaymentMethodConfirmHandler
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal data class PaymentElementCallbacks private constructor(
    val createIntentCallback: CreateIntentCallback?,
    val customPaymentMethodConfirmHandler: CustomPaymentMethodConfirmHandler?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
) {
    class Builder {
        private var createIntentCallback: CreateIntentCallback? = null
        private var customPaymentMethodConfirmHandler: CustomPaymentMethodConfirmHandler? = null
        private var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null

        fun createIntentCallback(createIntentCallback: CreateIntentCallback?) = apply {
            this.createIntentCallback = createIntentCallback
        }

        fun customPaymentMethodConfirmHandler(
            customPaymentMethodConfirmHandler: CustomPaymentMethodConfirmHandler?
        ) = apply {
            this.customPaymentMethodConfirmHandler = customPaymentMethodConfirmHandler
        }

        fun externalPaymentMethodConfirmHandler(
            externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?
        ) = apply {
            this.externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
        }

        fun build(): PaymentElementCallbacks {
            return PaymentElementCallbacks(
                createIntentCallback = createIntentCallback,
                customPaymentMethodConfirmHandler = customPaymentMethodConfirmHandler,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
            )
        }
    }
}
