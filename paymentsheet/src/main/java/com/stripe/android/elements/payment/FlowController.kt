package com.stripe.android.elements.payment

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.elements.AddressDetails
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER
import com.stripe.android.paymentsheet.PaymentOptionCallback
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.internalRememberPaymentSheetFlowController
import com.stripe.android.paymentsheet.model.PaymentOption

/**
 * A class that presents the individual steps of a payment sheet flow.
 */
interface FlowController {

    var shippingDetails: AddressDetails?

    /**
     * Displays a list of wallet buttons that can be used to checkout instantly
     */
    @WalletButtonsPreview
    @Composable
    fun WalletButtons()

    /**
     * Configure the FlowController to process a [PaymentIntent].
     *
     * @param paymentIntentClientSecret the client secret for the [PaymentIntent].
     * @param configuration optional [PaymentSheet] settings.
     * @param callback called with the result of configuring the FlowController.
     */
    fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: PaymentSheet.Configuration? = null,
        callback: ConfigCallback
    )

    /**
     * Configure the FlowController to process a [SetupIntent].
     *
     * @param setupIntentClientSecret the client secret for the [SetupIntent].
     * @param configuration optional [PaymentSheet] settings.
     * @param callback called with the result of configuring the FlowController.
     */
    fun configureWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: PaymentSheet.Configuration? = null,
        callback: ConfigCallback
    )

    /**
     * Configure the FlowController with an [IntentConfiguration].
     *
     * @param intentConfiguration The [IntentConfiguration] to use.
     * @param configuration An optional [PaymentSheet] configuration.
     * @param callback called with the result of configuring the FlowController.
     */
    fun configureWithIntentConfiguration(
        intentConfiguration: IntentConfiguration,
        configuration: PaymentSheet.Configuration? = null,
        callback: ConfigCallback
    )

    /**
     * Retrieve information about the customer's desired payment option.
     * You can use this to e.g. display the payment option in your UI.
     */
    fun getPaymentOption(): PaymentOption?

    /**
     * Present a sheet where the customer chooses how to pay, either by selecting an existing
     * payment method or adding a new one.
     * Call this when your "Select a payment method" button is tapped.
     */
    fun presentPaymentOptions()

    /**
     * Complete the payment or setup.
     */
    fun confirm()

    /**
     * Builder utility to set optional callbacks for [FlowController].
     *
     * @param resultCallback Called when a [PaymentSheetResult] is available.
     * @param paymentOptionCallback Called when the customer's desired payment method changes.
     */
    class Builder(
        internal val resultCallback: PaymentSheetResultCallback,
        internal val paymentOptionCallback: PaymentOptionCallback
    ) {
        private val callbacksBuilder = PaymentElementCallbacks.Builder()

        /**
         * @param handler Called when a user confirms payment for an external payment method.
         */
        fun externalPaymentMethodConfirmHandler(handler: ExternalPaymentMethodConfirmHandler) = apply {
            callbacksBuilder.externalPaymentMethodConfirmHandler(handler)
        }

        /**
         * @param callback Called when a user confirms payment for a custom payment method.
         */
        @ExperimentalCustomPaymentMethodsApi
        fun confirmCustomPaymentMethodCallback(callback: ConfirmCustomPaymentMethodCallback) = apply {
            callbacksBuilder.confirmCustomPaymentMethodCallback(callback)
        }

        /**
         * @param callback If specified, called when the customer confirms the payment or setup.
         */
        fun createIntentCallback(callback: CreateIntentCallback) = apply {
            callbacksBuilder.createIntentCallback(callback)
        }

        /**
         * @param callback If specified, called when an analytic event occurs.
         */
        @AnalyticEventCallbackPreview
        fun analyticEventCallback(callback: AnalyticEventCallback) = apply {
            callbacksBuilder.analyticEventCallback(callback)
        }

        /**
         * @param handlers Handlers for shop-pay specific events like shipping method and contact updates.
         */
        @ShopPayPreview
        fun shopPayHandlers(handlers: ShopPayHandlers) = apply {
            callbacksBuilder.shopPayHandlers(handlers)
        }

        /**
         * @param handler Called when a user calls confirm and their payment method is being handed off
         * to an external provider to handle payment/setup.
         */
        @SharedPaymentTokenSessionPreview
        fun preparePaymentMethodHandler(
            handler: PreparePaymentMethodHandler
        ) = apply {
            callbacksBuilder.preparePaymentMethodHandler(handler)
        }

        /**
         * Returns a [FlowController].
         *
         * @param activity The Activity that is presenting [FlowController].
         */
        fun build(activity: ComponentActivity): FlowController {
            initializeCallbacks()
            return FlowControllerFactory(activity, paymentOptionCallback, resultCallback).create()
        }

        /**
         * Returns a [FlowController].
         *
         * @param fragment The Fragment that is presenting [FlowController].
         */
        fun build(fragment: Fragment): FlowController {
            initializeCallbacks()
            return FlowControllerFactory(fragment, paymentOptionCallback, resultCallback).create()
        }

        /**
         * Returns a [FlowController] composable.
         */
        @Composable
        fun build(): FlowController {
            /*
             * Callbacks are initialized & updated internally by the internal composable function
             */
            return internalRememberPaymentSheetFlowController(
                callbacks = callbacksBuilder.build(),
                paymentOptionCallback = paymentOptionCallback,
                paymentResultCallback = resultCallback,
            )
        }

        private fun initializeCallbacks() {
            setFlowControllerCallbacks(callbacks = callbacksBuilder.build())
        }
    }

    sealed class Result {
        object Success : Result()

        class Failure(
            val error: Throwable
        ) : Result()
    }

    fun interface ConfigCallback {
        fun onConfigured(
            success: Boolean,
            error: Throwable?
        )
    }

    companion object {
        private fun setFlowControllerCallbacks(callbacks: PaymentElementCallbacks) {
            PaymentElementCallbackReferences[FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER] = callbacks
        }
    }
}
