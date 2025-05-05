package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.RowSelectionImmediateActionCallback
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.analytics.PaymentSheetConfirmationError

@OptIn(ExperimentalCustomPaymentMethodsApi::class, ExperimentalAnalyticEventCallbackApi::class)
internal data class PaymentElementCallbacks private constructor(
    val createIntentCallback: CreateIntentCallback?,
    val confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
    val analyticEventCallback: AnalyticEventCallback?,
    val rowSelectionCallback: RowSelectionImmediateActionCallback?,
) {
    class Builder {
        private var createIntentCallback: CreateIntentCallback? = null
        private var confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback? = null
        private var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null
        private var analyticEventCallback: AnalyticEventCallback? = null
        private var rowSelectionCallback: RowSelectionImmediateActionCallback? = null

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

        fun analyticEventCallback(analyticEventCallback: AnalyticEventCallback?) = apply {
            this.analyticEventCallback = analyticEventCallback
        }

        fun rowSelectionImmediateActionCallback(rowSelectionCallback: RowSelectionImmediateActionCallback?) = apply {
            this.rowSelectionCallback = rowSelectionCallback
        }

        fun build(): PaymentElementCallbacks {
            return PaymentElementCallbacks(
                createIntentCallback = createIntentCallback,
                confirmCustomPaymentMethodCallback = confirmCustomPaymentMethodCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                analyticEventCallback = analyticEventCallback,
                rowSelectionCallback = rowSelectionCallback,
            )
        }
    }
}
