package com.stripe.android.elements.payment

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.fragment.app.Fragment
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.AllowsRemovalOfLastSavedPaymentMethodPreview
import com.stripe.android.elements.Appearance
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.ExtendedLabelsInPaymentOptionPreview
import com.stripe.android.paymentelement.ShippingDetailsInPaymentOptionPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.internalRememberPaymentSheetFlowController
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

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
     * @param configuration optional [FlowController] settings.
     * @param callback called with the result of configuring the FlowController.
     */
    fun configureWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: Configuration? = null,
        callback: ConfigCallback
    )

    /**
     * Configure the FlowController to process a [SetupIntent].
     *
     * @param setupIntentClientSecret the client secret for the [SetupIntent].
     * @param configuration optional [FlowController] settings.
     * @param callback called with the result of configuring the FlowController.
     */
    fun configureWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: FlowController.Configuration? = null,
        callback: ConfigCallback
    )

    /**
     * Configure the FlowController with an [IntentConfiguration].
     *
     * @param intentConfiguration The [IntentConfiguration] to use.
     * @param configuration An optional [FlowController] configuration.
     * @param callback called with the result of configuring the FlowController.
     */
    fun configureWithIntentConfiguration(
        intentConfiguration: IntentConfiguration,
        configuration: FlowController.Configuration? = null,
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
     * @param resultCallback Called when a [FlowController.Result] is available.
     * @param paymentOptionCallback Called when the customer's desired payment method changes.
     */
    class Builder(
        internal val resultCallback: ResultCallback,
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

    /** Configuration for [FlowController] **/
    @Parcelize
    @Poko
    class Configuration internal constructor(
        internal val merchantDisplayName: String,

        internal val customer: CustomerConfiguration? = ConfigurationDefaults.customer,

        internal val googlePay: GooglePayConfiguration? = ConfigurationDefaults.googlePay,

        internal val defaultBillingDetails: BillingDetails? = ConfigurationDefaults.billingDetails,

        internal val shippingDetails: AddressDetails? = ConfigurationDefaults.shippingDetails,

        internal val allowsDelayedPaymentMethods: Boolean = ConfigurationDefaults.allowsDelayedPaymentMethods,

        internal val allowsPaymentMethodsRequiringShippingAddress: Boolean =
            ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,

        internal val appearance: Appearance = ConfigurationDefaults.appearance,

        internal val primaryButtonLabel: String? = ConfigurationDefaults.primaryButtonLabel,

        internal val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
            ConfigurationDefaults.billingDetailsCollectionConfiguration,

        internal val preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks,

        internal val allowsRemovalOfLastSavedPaymentMethod: Boolean =
            ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,

        internal val paymentMethodOrder: List<String> = ConfigurationDefaults.paymentMethodOrder,

        internal val externalPaymentMethods: List<String> = ConfigurationDefaults.externalPaymentMethods,

        internal val paymentMethodLayout: PaymentMethodLayout = ConfigurationDefaults.paymentMethodLayout,

        internal val cardBrandAcceptance: CardBrandAcceptance = ConfigurationDefaults.cardBrandAcceptance,

        internal val customPaymentMethods: List<CustomPaymentMethod> =
            ConfigurationDefaults.customPaymentMethods,

        internal val link: LinkConfiguration = ConfigurationDefaults.link,

        internal val walletButtons: WalletButtonsConfiguration = ConfigurationDefaults.walletButtons,

        internal val shopPayConfiguration: ShopPayConfiguration? = ConfigurationDefaults.shopPayConfiguration,

        internal val googlePlacesApiKey: String? = ConfigurationDefaults.googlePlacesApiKey,
    ) : Parcelable {

        @JvmOverloads
        constructor(
            /**
             * Your customer-facing business name.
             *
             * The default value is the name of your app.
             */
            merchantDisplayName: String,

            /**
             * If set, the customer can select a previously saved payment method within FlowController.
             */
            customer: CustomerConfiguration? = ConfigurationDefaults.customer,

            /**
             * Configuration related to the Stripe Customer making a payment.
             *
             * If set, FlowController displays Google Pay as a payment option.
             */
            googlePay: GooglePayConfiguration? = ConfigurationDefaults.googlePay,

            /**
             * The billing information for the customer.
             *
             * If set, FlowController will pre-populate the form fields with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the FlowController UI.
             */
            defaultBillingDetails: BillingDetails? = ConfigurationDefaults.billingDetails,

            /**
             * The shipping information for the customer.
             * If set, FlowController will pre-populate the form fields with the values provided.
             * This is used to display a "Billing address is same as shipping" checkbox if `defaultBillingDetails`
             * is not provided.
             * If `name` and `line1` are populated, it's also
             * [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping)
             * during payment.
             */
            shippingDetails: AddressDetails? = ConfigurationDefaults.shippingDetails,

            /**
             * If true, allows payment methods that do not move money at the end of the checkout.
             * Defaults to true.
             *
             * Some payment methods can't guarantee you will receive funds from your customer at the end
             * of the checkout because they take time to settle (eg. most bank debits, like SEPA or ACH)
             * or require customer action to complete (e.g. OXXO, Konbini, Boleto). If this is set to
             * true, make sure your integration listens to webhooks for notifications on whether a
             * payment has succeeded or not.
             *
             * See [payment-notification](https://stripe.com/docs/payments/payment-methods#payment-notification).
             */
            allowsDelayedPaymentMethods: Boolean = ConfigurationDefaults.allowsDelayedPaymentMethods,

            /**
             * If `true`, allows payment methods that require a shipping address, like Afterpay and
             * Affirm. Defaults to `false`.
             *
             * Set this to `true` if you collect shipping addresses via [shippingDetails] or
             * [FlowController.shippingDetails].
             *
             * **Note**: FlowController considers this property `true` if `shipping` details are present
             * on the FlowController when FlowController loads.
             */
            allowsPaymentMethodsRequiringShippingAddress: Boolean =
                ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,

            /**
             * Describes the appearance of FlowController.
             */
            appearance: Appearance = ConfigurationDefaults.appearance,

            /**
             * The label to use for the primary button.
             *
             * If not set, FlowController will display suitable default labels for payment and setup
             * intents.
             */
            primaryButtonLabel: String? = ConfigurationDefaults.primaryButtonLabel,

            /**
             * Describes how billing details should be collected.
             * All values default to `automatic`.
             * If `never` is used for a required field for the Payment Method used during checkout,
             * you **must** provide an appropriate value as part of [defaultBillingDetails].
             */
            billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
                ConfigurationDefaults.billingDetailsCollectionConfiguration,

            /**
             * A list of preferred networks that should be used to process payments
             * made with a co-branded card if your user hasn't selected a network
             * themselves.
             *
             * The first preferred network that matches any available network will
             * be used. If no preferred network is applicable, Stripe will select
             * the network.
             */
            preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks,
        ) : this(
            merchantDisplayName = merchantDisplayName,
            customer = customer,
            googlePay = googlePay,
            defaultBillingDetails = defaultBillingDetails,
            shippingDetails = shippingDetails,
            allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
            allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
            appearance = appearance,
            primaryButtonLabel = primaryButtonLabel,
            billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
            preferredNetworks = preferredNetworks,
            allowsRemovalOfLastSavedPaymentMethod = ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod,
            externalPaymentMethods = ConfigurationDefaults.externalPaymentMethods,
            customPaymentMethods = ConfigurationDefaults.customPaymentMethods,
        )

        /**
         * [Configuration] builder for cleaner object creation from Java.
         */
        @Suppress("TooManyFunctions")
        class Builder(
            private var merchantDisplayName: String
        ) {
            private var customer: CustomerConfiguration? = ConfigurationDefaults.customer
            private var googlePay: GooglePayConfiguration? = ConfigurationDefaults.googlePay
            private var defaultBillingDetails: BillingDetails? = ConfigurationDefaults.billingDetails
            private var shippingDetails: AddressDetails? = ConfigurationDefaults.shippingDetails
            private var allowsDelayedPaymentMethods: Boolean = ConfigurationDefaults.allowsDelayedPaymentMethods
            private var allowsPaymentMethodsRequiringShippingAddress: Boolean =
                ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress
            private var appearance: Appearance = ConfigurationDefaults.appearance
            private var primaryButtonLabel: String? = ConfigurationDefaults.primaryButtonLabel
            private var billingDetailsCollectionConfiguration =
                ConfigurationDefaults.billingDetailsCollectionConfiguration
            private var preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks
            private var allowsRemovalOfLastSavedPaymentMethod: Boolean =
                ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod
            private var paymentMethodOrder: List<String> = ConfigurationDefaults.paymentMethodOrder
            private var externalPaymentMethods: List<String> = ConfigurationDefaults.externalPaymentMethods
            private var paymentMethodLayout: PaymentMethodLayout = ConfigurationDefaults.paymentMethodLayout
            private var cardBrandAcceptance: CardBrandAcceptance = ConfigurationDefaults.cardBrandAcceptance
            private var link: LinkConfiguration = ConfigurationDefaults.link
            private var walletButtons: WalletButtonsConfiguration = ConfigurationDefaults.walletButtons
            private var shopPayConfiguration: ShopPayConfiguration? = ConfigurationDefaults.shopPayConfiguration
            private var googlePlacesApiKey: String? = ConfigurationDefaults.googlePlacesApiKey

            private var customPaymentMethods: List<CustomPaymentMethod> =
                ConfigurationDefaults.customPaymentMethods

            /**
             * Your customer-facing business name.
             *
             * The default value is the name of your app.
             */
            fun merchantDisplayName(merchantDisplayName: String) =
                apply { this.merchantDisplayName = merchantDisplayName }

            /**
             * If set, the customer can select a previously saved payment method within FlowController.
             */
            fun customer(customer: CustomerConfiguration?) =
                apply { this.customer = customer }

            /**
             * Configuration related to the Stripe Customer making a payment.
             *
             * If set, FlowController displays Google Pay as a payment option.
             */
            fun googlePay(googlePay: GooglePayConfiguration?) =
                apply { this.googlePay = googlePay }

            /**
             * The billing information for the customer.
             *
             * If set, FlowController will pre-populate the form fields with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the FlowController UI.
             */
            fun defaultBillingDetails(defaultBillingDetails: BillingDetails?) =
                apply { this.defaultBillingDetails = defaultBillingDetails }

            /**
             * The shipping information for the customer.
             * If set, FlowController will pre-populate the form fields with the values provided.
             * This is used to display a "Billing address is same as shipping" checkbox
             * if `defaultBillingDetails` is not provided.
             * If `name` and `line1` are populated, it's also
             * [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping)
             * during payment.
             */
            @Suppress("MaxLineLength")
            fun shippingDetails(shippingDetails: AddressDetails?) =
                apply { this.shippingDetails = shippingDetails }

            /**
             * If true, allows payment methods that do not move money at the end of the checkout.
             * Defaults to false.
             *
             * Some payment methods can't guarantee you will receive funds from your customer at the end
             * of the checkout because they take time to settle (eg. most bank debits, like SEPA or ACH)
             * or require customer action to complete (e.g. OXXO, Konbini, Boleto). If this is set to
             * true, make sure your integration listens to webhooks for notifications on whether a
             * payment has succeeded or not.
             *
             * See [payment-notification](https://stripe.com/docs/payments/payment-methods#payment-notification).
             */
            fun allowsDelayedPaymentMethods(allowsDelayedPaymentMethods: Boolean) =
                apply { this.allowsDelayedPaymentMethods = allowsDelayedPaymentMethods }

            /**
             * If `true`, allows payment methods that require a shipping address, like Afterpay and
             * Affirm. Defaults to `false`.
             *
             * Set this to `true` if you collect shipping addresses via [shippingDetails]
             *
             * **Note**: FlowController considers this property `true` if `shipping` details are present
             * on the PaymentIntent when FlowController loads.
             */
            fun allowsPaymentMethodsRequiringShippingAddress(
                allowsPaymentMethodsRequiringShippingAddress: Boolean,
            ) = apply {
                this.allowsPaymentMethodsRequiringShippingAddress =
                    allowsPaymentMethodsRequiringShippingAddress
            }

            /**
             * Describes the appearance of FlowController.
             */
            fun appearance(appearance: Appearance) =
                apply { this.appearance = appearance }

            /**
             * The label to use for the primary button.
             *
             * If not set, Payment Sheet will display suitable default labels for payment and setup
             * intents.
             */
            fun primaryButtonLabel(primaryButtonLabel: String) =
                apply { this.primaryButtonLabel = primaryButtonLabel }

            /**
             * Describes how billing details should be collected.
             * All values default to `automatic`.
             * If `never` is used for a required field for the Payment Method used during checkout,
             * you **must** provide an appropriate value as part of [defaultBillingDetails].
             */
            fun billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
            ) = apply {
                this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
            }

            /**
             * A list of preferred networks that should be used to process payments
             * made with a co-branded card if your user hasn't selected a network
             * themselves.
             *
             * The first preferred network that matches any available network will
             * be used. If no preferred network is applicable, Stripe will select
             * the network.
             */
            fun preferredNetworks(
                preferredNetworks: List<CardBrand>
            ) = apply {
                this.preferredNetworks = preferredNetworks
            }

            @AllowsRemovalOfLastSavedPaymentMethodPreview
            fun allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod: Boolean) = apply {
                this.allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            }

            /**
             * By default, FlowController will use a dynamic ordering that optimizes payment method display for the
             * customer. You can override the default order in which payment methods are displayed in FlowController
             * with a list of payment method types.
             *
             * See https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for the list of valid
             *  types.
             * - Example: listOf("card", "klarna")
             * - Note: If you omit payment methods from this list, they’ll be automatically ordered by Stripe after the
             *  ones you provide. Invalid payment methods are ignored.
             */
            fun paymentMethodOrder(paymentMethodOrder: List<String>): Builder = apply {
                this.paymentMethodOrder = paymentMethodOrder
            }

            /**
             * External payment methods to display in FlowController.
             *
             * If you specify any external payment methods here, you must also pass an
             * [ExternalPaymentMethodConfirmHandler] to the FlowController constructor.
             *
             * To learn more about external payment methods, see
             * https://docs.stripe.com/payments/external-payment-methods?platform=android. For the full list of
             * supported payment methods, see
             * https://docs.stripe.com/payments/external-payment-methods?platform=android#available-external-payment-methods.
             */
            fun externalPaymentMethods(externalPaymentMethods: List<String>): Builder = apply {
                this.externalPaymentMethods = externalPaymentMethods
            }

            /**
             * The layout of payment methods in FlowController. Defaults to [PaymentMethodLayout.Horizontal].
             * @see [PaymentMethodLayout] for the list of available layouts.
             */
            fun paymentMethodLayout(paymentMethodLayout: PaymentMethodLayout): Builder = apply {
                this.paymentMethodLayout = paymentMethodLayout
            }

            /**
             * By default, FlowController will accept all supported cards by Stripe.
             * You can specify card brands FlowController should block or allow
             * payment for by providing a list of those card brands.
             * **Note**: This is only a client-side solution.
             * **Note**: Card brand filtering is not currently supported in Link.
             */
            fun cardBrandAcceptance(
                cardBrandAcceptance: CardBrandAcceptance
            ) = apply {
                this.cardBrandAcceptance = cardBrandAcceptance
            }

            /**
             * Configuration related to custom payment methods.
             *
             * If set, Payment Sheet will display the defined list of custom payment methods in the UI.
             */
            @ExperimentalCustomPaymentMethodsApi
            fun customPaymentMethods(
                customPaymentMethods: List<CustomPaymentMethod>,
            ) = apply {
                this.customPaymentMethods = customPaymentMethods
            }

            /**
             * Configuration related to Link.
             */
            fun link(link: LinkConfiguration): Builder = apply {
                this.link = link
            }

            /**
             * Configuration related to `WalletButtons`
             */
            @WalletButtonsPreview
            fun walletButtons(walletButtons: WalletButtonsConfiguration) = apply {
                this.walletButtons = walletButtons
            }

            /**
             * Configuration related to `ShopPay`
             */
            @ShopPayPreview
            fun shopPayConfiguration(shopPayConfiguration: ShopPayConfiguration) = apply {
                this.shopPayConfiguration = shopPayConfiguration
            }

            /**
             * Google Places API key to support autocomplete when collecting billing details
             */
            @AddressAutocompletePreview
            fun googlePlacesApiKey(googlePlacesApiKey: String) = apply {
                this.googlePlacesApiKey = googlePlacesApiKey
            }

            fun build() = Configuration(
                merchantDisplayName = merchantDisplayName,
                customer = customer,
                googlePay = googlePay,
                defaultBillingDetails = defaultBillingDetails,
                shippingDetails = shippingDetails,
                allowsDelayedPaymentMethods = allowsDelayedPaymentMethods,
                allowsPaymentMethodsRequiringShippingAddress = allowsPaymentMethodsRequiringShippingAddress,
                appearance = appearance,
                primaryButtonLabel = primaryButtonLabel,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration,
                preferredNetworks = preferredNetworks,
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
                paymentMethodOrder = paymentMethodOrder,
                externalPaymentMethods = externalPaymentMethods,
                paymentMethodLayout = paymentMethodLayout,
                cardBrandAcceptance = cardBrandAcceptance,
                customPaymentMethods = customPaymentMethods,
                link = link,
                walletButtons = walletButtons,
                shopPayConfiguration = shopPayConfiguration,
                googlePlacesApiKey = googlePlacesApiKey,
            )
        }

        internal companion object {
            fun default(context: Context): Configuration {
                val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
                return Configuration(appName)
            }
        }

        @OptIn(
            AllowsRemovalOfLastSavedPaymentMethodPreview::class,
            ExperimentalCustomPaymentMethodsApi::class,
            WalletButtonsPreview::class,
            ShopPayPreview::class
        )
        internal fun newBuilder(): Builder = Builder(merchantDisplayName)
            .customer(customer)
            .googlePay(googlePay)
            .defaultBillingDetails(defaultBillingDetails)
            .shippingDetails(shippingDetails)
            .allowsDelayedPaymentMethods(allowsDelayedPaymentMethods)
            .allowsPaymentMethodsRequiringShippingAddress(allowsPaymentMethodsRequiringShippingAddress)
            .appearance(appearance)
            .billingDetailsCollectionConfiguration(billingDetailsCollectionConfiguration)
            .preferredNetworks(preferredNetworks)
            .allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod)
            .paymentMethodOrder(paymentMethodOrder)
            .externalPaymentMethods(externalPaymentMethods)
            .paymentMethodLayout(paymentMethodLayout)
            .cardBrandAcceptance(cardBrandAcceptance)
            .customPaymentMethods(customPaymentMethods)
            .link(link)
            .walletButtons(walletButtons)
            .apply {
                primaryButtonLabel?.let { primaryButtonLabel(it) }
                shopPayConfiguration?.let { shopPayConfiguration(it) }
            }
    }

    /**
     * The result of an attempt to confirm a [PaymentIntent] or [SetupIntent].
     */
    sealed class Result : Parcelable {

        /**
         * The customer completed the payment or setup.
         * The payment may still be processing at this point; don't assume money has successfully moved.
         *
         * Your app should transition to a generic receipt view (e.g. a screen that displays "Your order
         * is confirmed!"), and fulfill the order (e.g. ship the product to the customer) after
         * receiving a successful payment event from Stripe.
         *
         * See [Stripe's documentation](https://stripe.com/docs/payments/handling-payment-events)
         */
        @Parcelize
        @Poko
        class Completed internal constructor(
            @Suppress("unused") private val irrelevantValueForGeneratedCode: Boolean = true
        ) : Result()

        /**
         * The customer canceled the payment or setup attempt.
         */
        @Parcelize
        @Poko
        class Canceled internal constructor(
            @Suppress("unused") private val irrelevantValueForGeneratedCode: Boolean = true
        ) : Result()

        /**
         * The payment or setup attempt failed.
         * @param error The error encountered by the customer.
         */
        @Parcelize
        @Poko
        class Failed internal constructor(
            val error: Throwable
        ) : Result()
    }

    /**
     * Callback that is invoked when a [Result] is available.
     */
    fun interface ResultCallback {
        fun onFlowControllerResult(flowControllerResult: Result)
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
