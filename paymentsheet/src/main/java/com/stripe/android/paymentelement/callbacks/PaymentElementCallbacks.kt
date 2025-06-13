package com.stripe.android.paymentelement.callbacks

import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElement.RowSelectionBehavior.Companion.getInternalRowSelectionCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ShopPayHandlers

@OptIn(ExperimentalCustomPaymentMethodsApi::class, ExperimentalAnalyticEventCallbackApi::class)
internal data class PaymentElementCallbacks private constructor(
    val createIntentCallback: CreateIntentCallback?,
    val confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
    val analyticEventCallback: AnalyticEventCallback?,
    val rowSelectionCallback: InternalRowSelectionCallback?,
    val shopPayHandlers: ShopPayHandlers?
) {
    class Builder {
        private var createIntentCallback: CreateIntentCallback? = null
        private var confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback? = null
        private var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null
        private var analyticEventCallback: AnalyticEventCallback? = null
        private var rowSelectionCallback: InternalRowSelectionCallback? = null
        private var shopPayHandlers: ShopPayHandlers? = null

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

        @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
        fun rowSelectionImmediateActionCallback(
            rowSelectionBehavior: EmbeddedPaymentElement.RowSelectionBehavior,
            element: EmbeddedPaymentElement,
        ) = apply {
            this.rowSelectionCallback = getInternalRowSelectionCallback(
                rowSelectionBehavior = rowSelectionBehavior,
                embeddedPaymentElement = element
            )
        }

        fun shopPayHandlers(shopPayHandlers: ShopPayHandlers?) = apply {
            this.shopPayHandlers = shopPayHandlers
        }

        fun build(): PaymentElementCallbacks {
            return PaymentElementCallbacks(
                createIntentCallback = createIntentCallback,
                confirmCustomPaymentMethodCallback = confirmCustomPaymentMethodCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                analyticEventCallback = analyticEventCallback,
                rowSelectionCallback = rowSelectionCallback,
                shopPayHandlers = shopPayHandlers
            )
        }
    }
}
