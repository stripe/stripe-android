package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler

@OptIn(ExperimentalCustomPaymentMethodsApi::class)
internal data class PaymentElementCallbacks private constructor(
    val createIntentCallback: CreateIntentCallback?,
    val confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
) {
    class Builder {
        private var createIntentCallback: CreateIntentCallback? = null
        private var confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback? = null
        private var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null

        fun createIntentCallback(createIntentCallback: CreateIntentCallback?) = apply {
            this.createIntentCallback = createIntentCallback
        }

        fun confirmCustomPaymentMethodCallback(
            confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback?
        ) = apply {
            this.confirmCustomPaymentMethodCallback = confirmCustomPaymentMethodCallback
        }

        fun externalPaymentMethodConfirmHandler(
            externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?
        ) = apply {
            this.externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler
        }

        fun build(): PaymentElementCallbacks {
            return PaymentElementCallbacks(
                createIntentCallback = createIntentCallback,
                confirmCustomPaymentMethodCallback = confirmCustomPaymentMethodCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
            )
        }
    }
}
