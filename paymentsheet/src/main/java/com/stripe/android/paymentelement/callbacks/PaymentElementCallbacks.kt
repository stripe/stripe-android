package com.stripe.android.paymentelement.callbacks

import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.CreateIntentWithConfirmationTokenCallback
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.EmbeddedPaymentElement.RowSelectionBehavior.Companion.getInternalRowSelectionCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.embedded.InternalRowSelectionCallback
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ShopPayHandlers

@OptIn(
    ExperimentalCustomPaymentMethodsApi::class,
    ExperimentalAnalyticEventCallbackApi::class,
    ShopPayPreview::class,
    SharedPaymentTokenSessionPreview::class
)
internal data class PaymentElementCallbacks private constructor(
    val createIntentCallback: CreateIntentCallback?,
    val createIntentWithConfirmationTokenCallback: CreateIntentWithConfirmationTokenCallback?,
    val confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback?,
    val externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler?,
    val analyticEventCallback: AnalyticEventCallback?,
    val rowSelectionCallback: InternalRowSelectionCallback?,
    val shopPayHandlers: ShopPayHandlers?,
    val preparePaymentMethodHandler: PreparePaymentMethodHandler?,
) {
    class Builder {
        private var createIntentCallback: CreateIntentCallback? = null
        private var createIntentWithConfirmationTokenCallback: CreateIntentWithConfirmationTokenCallback? = null
        private var confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback? = null
        private var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null
        private var analyticEventCallback: AnalyticEventCallback? = null
        private var rowSelectionCallback: InternalRowSelectionCallback? = null
        private var shopPayHandlers: ShopPayHandlers? = null
        private var preparePaymentMethodHandler: PreparePaymentMethodHandler? = null

        fun createIntentCallback(createIntentCallback: CreateIntentCallback?) = apply {
            this.createIntentCallback = createIntentCallback
        }

        fun createIntentCallback(
            createIntentWithConfirmationTokenCallback: CreateIntentWithConfirmationTokenCallback?
        ) = apply {
            this.createIntentWithConfirmationTokenCallback = createIntentWithConfirmationTokenCallback
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

        fun preparePaymentMethodHandler(handler: PreparePaymentMethodHandler?) = apply {
            this.preparePaymentMethodHandler = handler
        }

        fun rowSelectionImmediateActionCallback(
            rowSelectionBehavior: EmbeddedPaymentElement.RowSelectionBehavior,
            element: EmbeddedPaymentElement,
        ) = apply {
            this.rowSelectionCallback = getInternalRowSelectionCallback(
                rowSelectionBehavior = rowSelectionBehavior,
                embeddedPaymentElement = element
            )
        }

        @OptIn(ShopPayPreview::class)
        fun shopPayHandlers(shopPayHandlers: ShopPayHandlers?) = apply {
            this.shopPayHandlers = shopPayHandlers
        }

        fun build(): PaymentElementCallbacks {
            var mutualExclusiveCallbackCount = 0
            if (createIntentCallback != null) {
                mutualExclusiveCallbackCount++
            }
            if (createIntentWithConfirmationTokenCallback != null) {
                mutualExclusiveCallbackCount++
            }
            if (preparePaymentMethodHandler != null) {
                mutualExclusiveCallbackCount++
            }
            if (mutualExclusiveCallbackCount > 1) {
                throw IllegalArgumentException(
                    "Only one of createIntentCallback, " +
                        "createIntentWithConfirmationTokenCallback or " +
                        "preparePaymentMethodHandler can be set"
                )
            }

            return PaymentElementCallbacks(
                createIntentCallback = createIntentCallback,
                createIntentWithConfirmationTokenCallback = createIntentWithConfirmationTokenCallback,
                confirmCustomPaymentMethodCallback = confirmCustomPaymentMethodCallback,
                externalPaymentMethodConfirmHandler = externalPaymentMethodConfirmHandler,
                analyticEventCallback = analyticEventCallback,
                rowSelectionCallback = rowSelectionCallback,
                shopPayHandlers = shopPayHandlers,
                preparePaymentMethodHandler = preparePaymentMethodHandler,
            )
        }
    }
}
