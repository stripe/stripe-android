package com.stripe.android.elements.payment

import android.graphics.drawable.Drawable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.fragment.app.Fragment
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.BillingDetails
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExtendedLabelsInPaymentOptionPreview
import com.stripe.android.paymentelement.ShippingDetailsInPaymentOptionPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER
import com.stripe.android.paymentsheet.PaymentSheetResultCallback
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.internalRememberPaymentSheetFlowController
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko

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
    fun getPaymentOption(): PaymentOptionDisplayData?

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
        internal val paymentOptionCallback: PaymentOptionDisplayData.Callback
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

    /**
     * The customer's selected payment option.
     */
    @Poko
    class PaymentOptionDisplayData internal constructor(
        /**
         * The drawable resource id of the icon that represents the payment option.
         */
        internal val drawableResourceId: Int,
        /**
         * A label that describes the payment option.
         *
         * For example, "···· 4242" for a Visa ending in 4242.
         */
        val label: String,
        /**
         * A string representation of the customer's desired payment method:
         * - If this is a Stripe payment method, see
         *      https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for possible values.
         * - If this is an external payment method, see
         *      https://docs.stripe.com/payments/mobile/external-payment-methods?platform=android
         *      for possible values.
         * - If this is Google Pay, the value is "google_pay".
         */
        val paymentMethodType: String,

        /**
         * The billing details associated with the customer's desired payment method.
         */
        val billingDetails: BillingDetails?,
        private val _shippingDetails: AddressDetails?,
        private val _labels: Labels,

        private val imageLoader: suspend () -> Drawable,
    ) {

        @Poko
        class Labels internal constructor(
            /**
             * Primary label for the payment option.
             * This will primarily describe the type of the payment option being used.
             * For cards, this could be `Mastercard`, 'Visa', or others.
             * For other payment methods, this is typically the payment method name.
             */
            val label: String,
            /**
             * Secondary optional label for the payment option.
             * This will primarily describe any expanded details about the payment option such as
             * the last four digits of a card or bank account.
             */
            val sublabel: String? = null,
        )

        /**
         * Labels containing additional information about the payment option.
         */
        @ExtendedLabelsInPaymentOptionPreview
        val labels: Labels
            get() = _labels

        /**
         * A shipping address that the user provided during checkout.
         */
        @ShippingDetailsInPaymentOptionPreview
        val shippingDetails: AddressDetails?
            get() = _shippingDetails

        /**
         * A [Painter] to draw the icon associated with this [PaymentOptionDisplayData].
         */
        val iconPainter: Painter
            @Composable
            get() = rememberDrawablePainter(icon())

        /**
         * Fetches the icon associated with this [PaymentOptionDisplayData].
         */
        fun icon(): Drawable {
            return DelegateDrawable(
                imageLoader = imageLoader,
            )
        }

        /**
         * Callback that is invoked when the customer's [PaymentOptionDisplayData] selection changes.
         */
        fun interface Callback {

            /**
             * @param paymentOption The new [PaymentOptionDisplayData] selection. If null, the customer has not yet
             * selected a [PaymentOptionDisplayData]. The customer can only complete the transaction if this value is
             * not null.
             */
            fun onPaymentOption(paymentOption: PaymentOptionDisplayData?)
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
