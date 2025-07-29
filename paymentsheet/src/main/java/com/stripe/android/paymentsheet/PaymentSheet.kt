package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.Appearance
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.PaymentMethodLayout
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * A drop-in class that presents a bottom sheet to collect and process a customer's payment.
 */
class PaymentSheet internal constructor(
    private val paymentSheetLauncher: PaymentSheetLauncher
) {
    /**
     * Constructor to be used when launching [PaymentSheet] from a [ComponentActivity].
     *
     * @param activity The Activity that is presenting [PaymentSheet].
     * @param callback Called with the result of the payment after [PaymentSheet] is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith("PaymentSheet.Builder(callback).build(activity)")
    )
    constructor(
        activity: ComponentActivity,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    )

    /**
     * Constructor to be used when launching [PaymentSheet] from a [ComponentActivity] and external payment methods are
     * specified in your [Configuration].
     *
     * @param activity The Activity that is presenting [PaymentSheet].
     * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment method.
     * @param callback Called with the result of the payment after [PaymentSheet] is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith(
            "PaymentSheet.Builder(callback)" +
                ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                ".build(activity)"
        )
    )
    constructor(
        activity: ComponentActivity,
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
        callback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(activity, callback)
    ) {
        setPaymentSheetCallbacks(
            PaymentElementCallbacks.Builder()
                .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                .build()
        )
    }

    /**
     * Constructor to be used when launching [PaymentSheet] from a [ComponentActivity] and intending
     * to create and optionally confirm the [PaymentIntent] or [SetupIntent] on your server.
     *
     * @param activity The Activity that is presenting [PaymentSheet].
     * @param createIntentCallback Called when the customer confirms the payment or setup.
     * @param paymentResultCallback Called with the result of the payment or setup after
     * [PaymentSheet] is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith(
            "PaymentSheet.Builder(paymentResultCallback)" +
                ".createIntentCallback(createIntentCallback)" +
                ".build(activity)"
        )
    )
    constructor(
        activity: ComponentActivity,
        createIntentCallback: CreateIntentCallback,
        paymentResultCallback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(activity, paymentResultCallback)
    ) {
        setPaymentSheetCallbacks(
            PaymentElementCallbacks.Builder()
                .createIntentCallback(createIntentCallback)
                .build()
        )
    }

    /**
     * Constructor to be used when launching [PaymentSheet] from a [ComponentActivity] and intending
     * to create and optionally confirm the [PaymentIntent] or [SetupIntent] on your server and external payment methods
     * are specified in your [Configuration].
     *
     * @param activity The Activity that is presenting [PaymentSheet].
     * @param createIntentCallback Called when the customer confirms the payment or setup.
     * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment method.
     * @param paymentResultCallback Called with the result of the payment or setup after
     * [PaymentSheet] is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith(
            "PaymentSheet.Builder(paymentResultCallback)" +
                ".createIntentCallback(createIntentCallback)" +
                ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                ".build(activity)"
        )
    )
    constructor(
        activity: ComponentActivity,
        createIntentCallback: CreateIntentCallback,
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
        paymentResultCallback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(activity, paymentResultCallback)
    ) {
        setPaymentSheetCallbacks(
            PaymentElementCallbacks.Builder()
                .createIntentCallback(createIntentCallback)
                .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                .build()
        )
    }

    /**
     * Constructor to be used when launching the payment sheet from a [Fragment].
     *
     * @param fragment the Fragment that is presenting the payment sheet.
     * @param callback called with the result of the payment after the payment sheet is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith("PaymentSheet.Builder(callback).build(fragment)")
    )
    constructor(
        fragment: Fragment,
        callback: PaymentSheetResultCallback
    ) : this(
        DefaultPaymentSheetLauncher(fragment, callback)
    )

    /**
     * Constructor to be used when launching the payment sheet from a [Fragment] and external payment methods
     * are specified in your [Configuration].
     *
     * @param fragment the Fragment that is presenting the payment sheet.
     * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment method.
     * @param callback called with the result of the payment after the payment sheet is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith(
            "PaymentSheet.Builder(callback)" +
                ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                ".build(fragment)"
        )
    )
    constructor(
        fragment: Fragment,
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
        callback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(fragment, callback)
    ) {
        setPaymentSheetCallbacks(
            PaymentElementCallbacks.Builder()
                .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                .build()
        )
    }

    /**
     * Constructor to be used when launching [PaymentSheet] from a [Fragment] and intending to
     * create and optionally confirm the [PaymentIntent] or [SetupIntent] on your server.
     *
     * @param fragment The Fragment that is presenting [PaymentSheet].
     * @param createIntentCallback Called when the customer confirms the payment or setup.
     * @param paymentResultCallback Called with the result of the payment or setup after
     * [PaymentSheet] is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith(
            "PaymentSheet.Builder(paymentResultCallback)" +
                ".createIntentCallback(createIntentCallback)" +
                ".build(fragment)"
        )
    )
    constructor(
        fragment: Fragment,
        createIntentCallback: CreateIntentCallback,
        paymentResultCallback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(fragment, paymentResultCallback)
    ) {
        setPaymentSheetCallbacks(
            PaymentElementCallbacks.Builder()
                .createIntentCallback(createIntentCallback)
                .build()
        )
    }

    /**
     * Constructor to be used when launching [PaymentSheet] from a [Fragment] and intending to
     * create and optionally confirm the [PaymentIntent] or [SetupIntent] on your server and external payment methods
     * are specified in your [Configuration].
     *
     * @param fragment The Fragment that is presenting [PaymentSheet].
     * @param createIntentCallback Called when the customer confirms the payment or setup.
     * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment method.
     * @param paymentResultCallback Called with the result of the payment or setup after
     * [PaymentSheet] is dismissed.
     */
    @Deprecated(
        message = "This will be removed in a future release.",
        replaceWith = ReplaceWith(
            "PaymentSheet.Builder(paymentResultCallback)" +
                ".createIntentCallback(createIntentCallback)" +
                ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                ".build(fragment)"
        )
    )
    constructor(
        fragment: Fragment,
        createIntentCallback: CreateIntentCallback,
        externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
        paymentResultCallback: PaymentSheetResultCallback,
    ) : this(
        DefaultPaymentSheetLauncher(fragment, paymentResultCallback)
    ) {
        setPaymentSheetCallbacks(
            PaymentElementCallbacks.Builder()
                .createIntentCallback(createIntentCallback)
                .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                .build()
        )
    }

    /**
     * Builder to add optional callbacks to [PaymentSheet].
     *
     * @param resultCallback Called with the result of the payment after [PaymentSheet] is dismissed.
     */
    class Builder(internal val resultCallback: PaymentSheetResultCallback) {
        private val callbacksBuilder = PaymentElementCallbacks.Builder()

        /**
         * @param handler Called when a user confirms payment for an external payment method. Use with
         * [Configuration.Builder.externalPaymentMethods] to specify external payment methods.
         */
        fun externalPaymentMethodConfirmHandler(handler: ExternalPaymentMethodConfirmHandler) = apply {
            callbacksBuilder.externalPaymentMethodConfirmHandler(handler)
        }

        /**
         * @param callback Called when a user confirms payment for a custom payment method. Use with
         * [Configuration.Builder.customPaymentMethods] to specify custom payment methods.
         */
        @ExperimentalCustomPaymentMethodsApi
        fun confirmCustomPaymentMethodCallback(callback: ConfirmCustomPaymentMethodCallback) = apply {
            callbacksBuilder.confirmCustomPaymentMethodCallback(callback)
        }

        /**
         * @param callback Called when the customer confirms the payment or setup.
         * Only used when [presentWithIntentConfiguration] is called for a deferred flow.
         */
        fun createIntentCallback(callback: CreateIntentCallback) = apply {
            callbacksBuilder.createIntentCallback(callback)
        }

        /**
         * @param callback Called when an analytic event occurs.
         */
        @ExperimentalAnalyticEventCallbackApi
        fun analyticEventCallback(callback: AnalyticEventCallback) = apply {
            callbacksBuilder.analyticEventCallback(callback)
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
         * Returns a [PaymentSheet].
         *
         * @param activity The Activity that is presenting [PaymentSheet].
         */
        fun build(activity: ComponentActivity): PaymentSheet {
            initializeCallbacks()
            return PaymentSheet(DefaultPaymentSheetLauncher(activity, resultCallback))
        }

        /**
         * Returns a [PaymentSheet].
         *
         * @param fragment the Fragment that is presenting the payment sheet.
         */
        fun build(fragment: Fragment): PaymentSheet {
            initializeCallbacks()
            return PaymentSheet(DefaultPaymentSheetLauncher(fragment, resultCallback))
        }

        /**
         * Returns a [PaymentSheet] composable.
         */
        @Composable
        fun build(): PaymentSheet {
            /*
             * Callbacks are initialized & updated internally by the internal composable function
             */
            return internalRememberPaymentSheet(
                callbacks = callbacksBuilder.build(),
                paymentResultCallback = resultCallback,
            )
        }

        private fun initializeCallbacks() {
            setPaymentSheetCallbacks(callbacksBuilder.build())
        }
    }

    /**
     * Present [PaymentSheet] to process a [PaymentIntent].
     *
     * If the [PaymentIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param paymentIntentClientSecret The client secret of the [PaymentIntent].
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithPaymentIntent(
        paymentIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.present(
            mode = InitializationMode.PaymentIntent(paymentIntentClientSecret),
            configuration = configuration,
        )
    }

    /**
     * Present [PaymentSheet] to process a [SetupIntent].
     *
     * If the [SetupIntent] is already confirmed, [PaymentSheetResultCallback] will be invoked
     * with [PaymentSheetResult.Completed].
     *
     * @param setupIntentClientSecret The client secret of the [SetupIntent].
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithSetupIntent(
        setupIntentClientSecret: String,
        configuration: Configuration? = null
    ) {
        paymentSheetLauncher.present(
            mode = InitializationMode.SetupIntent(setupIntentClientSecret),
            configuration = configuration,
        )
    }

    /**
     * Present [PaymentSheet] with an [IntentConfiguration].
     *
     * @param intentConfiguration The [IntentConfiguration] to use.
     * @param configuration An optional [PaymentSheet] configuration.
     */
    @JvmOverloads
    fun presentWithIntentConfiguration(
        intentConfiguration: IntentConfiguration,
        configuration: Configuration? = null,
    ) {
        paymentSheetLauncher.present(
            mode = InitializationMode.DeferredIntent(intentConfiguration),
            configuration = configuration,
        )
    }

    /** Configuration for [PaymentSheet] **/
    @Parcelize
    data class Configuration internal constructor(
        /**
         * Your customer-facing business name.
         *
         * The default value is the name of your app.
         */
        val merchantDisplayName: String,

        /**
         * If set, the customer can select a previously saved payment method within PaymentSheet.
         */
        val customer: CustomerConfiguration? = ConfigurationDefaults.customer,

        /**
         * Configuration related to the Stripe Customer making a payment.
         *
         * If set, PaymentSheet displays Google Pay as a payment option.
         */
        val googlePay: GooglePayConfiguration? = ConfigurationDefaults.googlePay,

        /**
         * The color of the Pay or Add button. Keep in mind the text color is white.
         *
         * If set, PaymentSheet displays the button with this color.
         */
        @Deprecated(
            message = "Use Appearance parameter to customize primary button color",
            replaceWith = ReplaceWith(
                expression = "Appearance.colorsLight/colorsDark.primary " +
                    "or PrimaryButton.colorsLight/colorsDark.background"
            )
        )
        val primaryButtonColor: ColorStateList? = ConfigurationDefaults.primaryButtonColor,

        /**
         * The billing information for the customer.
         *
         * If set, PaymentSheet will pre-populate the form fields with the values provided.
         * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
         * these values will be attached to the payment method even if they are not collected by
         * the PaymentSheet UI.
         */
        val defaultBillingDetails: BillingDetails? = ConfigurationDefaults.billingDetails,

        /**
         * The shipping information for the customer.
         * If set, PaymentSheet will pre-populate the form fields with the values provided.
         * This is used to display a "Billing address is same as shipping" checkbox if `defaultBillingDetails` is not provided.
         * If `name` and `line1` are populated, it's also [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping) during payment.
         */
        val shippingDetails: AddressDetails? = ConfigurationDefaults.shippingDetails,

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
        val allowsDelayedPaymentMethods: Boolean = ConfigurationDefaults.allowsDelayedPaymentMethods,

        /**
         * If `true`, allows payment methods that require a shipping address, like Afterpay and
         * Affirm. Defaults to `false`.
         *
         * Set this to `true` if you collect shipping addresses via [shippingDetails] or
         * [FlowController.shippingDetails].
         *
         * **Note**: PaymentSheet considers this property `true` if `shipping` details are present
         * on the PaymentIntent when PaymentSheet loads.
         */
        val allowsPaymentMethodsRequiringShippingAddress: Boolean =
            ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,

        /**
         * Describes the appearance of Payment Sheet.
         */
        val appearance: Appearance = ConfigurationDefaults.appearance,

        /**
         * The label to use for the primary button.
         *
         * If not set, Payment Sheet will display suitable default labels for payment and setup
         * intents.
         */
        val primaryButtonLabel: String? = ConfigurationDefaults.primaryButtonLabel,

        /**
         * Describes how billing details should be collected.
         * All values default to `automatic`.
         * If `never` is used for a required field for the Payment Method used during checkout,
         * you **must** provide an appropriate value as part of [defaultBillingDetails].
         */
        val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration =
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
        val preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks,

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
             * If set, the customer can select a previously saved payment method within PaymentSheet.
             */
            customer: CustomerConfiguration? = ConfigurationDefaults.customer,

            /**
             * Configuration related to the Stripe Customer making a payment.
             *
             * If set, PaymentSheet displays Google Pay as a payment option.
             */
            googlePay: GooglePayConfiguration? = ConfigurationDefaults.googlePay,

            /**
             * The color of the Pay or Add button. Keep in mind the text color is white.
             *
             * If set, PaymentSheet displays the button with this color.
             */
            primaryButtonColor: ColorStateList? = ConfigurationDefaults.primaryButtonColor,

            /**
             * The billing information for the customer.
             *
             * If set, PaymentSheet will pre-populate the form fields with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the PaymentSheet UI.
             */
            defaultBillingDetails: BillingDetails? = ConfigurationDefaults.billingDetails,

            /**
             * The shipping information for the customer.
             * If set, PaymentSheet will pre-populate the form fields with the values provided.
             * This is used to display a "Billing address is same as shipping" checkbox if `defaultBillingDetails` is not provided.
             * If `name` and `line1` are populated, it's also [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping) during payment.
             */
            shippingDetails: AddressDetails? = ConfigurationDefaults.shippingDetails,

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
            allowsDelayedPaymentMethods: Boolean = ConfigurationDefaults.allowsDelayedPaymentMethods,

            /**
             * If `true`, allows payment methods that require a shipping address, like Afterpay and
             * Affirm. Defaults to `false`.
             *
             * Set this to `true` if you collect shipping addresses via [shippingDetails] or
             * [FlowController.shippingDetails].
             *
             * **Note**: PaymentSheet considers this property `true` if `shipping` details are present
             * on the PaymentIntent when PaymentSheet loads.
             */
            allowsPaymentMethodsRequiringShippingAddress: Boolean =
                ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress,

            /**
             * Describes the appearance of Payment Sheet.
             */
            appearance: Appearance = ConfigurationDefaults.appearance,

            /**
             * The label to use for the primary button.
             *
             * If not set, Payment Sheet will display suitable default labels for payment and setup
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
            primaryButtonColor = primaryButtonColor,
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
            private var primaryButtonColor: ColorStateList? = ConfigurationDefaults.primaryButtonColor
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
            private var link: PaymentSheet.LinkConfiguration = ConfigurationDefaults.link
            private var walletButtons: WalletButtonsConfiguration = ConfigurationDefaults.walletButtons
            private var shopPayConfiguration: ShopPayConfiguration? = ConfigurationDefaults.shopPayConfiguration
            private var googlePlacesApiKey: String? = ConfigurationDefaults.googlePlacesApiKey

            private var customPaymentMethods: List<CustomPaymentMethod> =
                ConfigurationDefaults.customPaymentMethods

            fun merchantDisplayName(merchantDisplayName: String) =
                apply { this.merchantDisplayName = merchantDisplayName }

            fun customer(customer: CustomerConfiguration?) =
                apply { this.customer = customer }

            fun googlePay(googlePay: GooglePayConfiguration?) =
                apply { this.googlePay = googlePay }

            @Deprecated(
                message = "Use Appearance parameter to customize primary button color",
                replaceWith = ReplaceWith(
                    expression = "Appearance.colorsLight/colorsDark.primary " +
                        "or PrimaryButton.colorsLight/colorsDark.background"
                )
            )
            fun primaryButtonColor(primaryButtonColor: ColorStateList?) =
                apply { this.primaryButtonColor = primaryButtonColor }

            fun defaultBillingDetails(defaultBillingDetails: BillingDetails?) =
                apply { this.defaultBillingDetails = defaultBillingDetails }

            fun shippingDetails(shippingDetails: AddressDetails?) =
                apply { this.shippingDetails = shippingDetails }

            fun allowsDelayedPaymentMethods(allowsDelayedPaymentMethods: Boolean) =
                apply { this.allowsDelayedPaymentMethods = allowsDelayedPaymentMethods }

            fun allowsPaymentMethodsRequiringShippingAddress(
                allowsPaymentMethodsRequiringShippingAddress: Boolean,
            ) = apply {
                this.allowsPaymentMethodsRequiringShippingAddress =
                    allowsPaymentMethodsRequiringShippingAddress
            }

            fun appearance(appearance: Appearance) =
                apply { this.appearance = appearance }

            fun primaryButtonLabel(primaryButtonLabel: String) =
                apply { this.primaryButtonLabel = primaryButtonLabel }

            fun billingDetailsCollectionConfiguration(
                billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration
            ) = apply {
                this.billingDetailsCollectionConfiguration = billingDetailsCollectionConfiguration
            }

            fun preferredNetworks(
                preferredNetworks: List<CardBrand>
            ) = apply {
                this.preferredNetworks = preferredNetworks
            }

            @ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
            fun allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod: Boolean) = apply {
                this.allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            }

            /**
             * By default, PaymentSheet will use a dynamic ordering that optimizes payment method display for the
             * customer. You can override the default order in which payment methods are displayed in PaymentSheet with
             * a list of payment method types.
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
             * External payment methods to display in PaymentSheet.
             *
             * If you specify any external payment methods here, you must also pass an
             * [ExternalPaymentMethodConfirmHandler] to the FlowController or PaymentSheet constructor.
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
             * The layout of payment methods in PaymentSheet. Defaults to [PaymentMethodLayout.Horizontal].
             * @see [PaymentMethodLayout] for the list of available layouts.
             */
            fun paymentMethodLayout(paymentMethodLayout: PaymentMethodLayout): Builder = apply {
                this.paymentMethodLayout = paymentMethodLayout
            }

            /**
             * By default, PaymentSheet will accept all supported cards by Stripe.
             * You can specify card brands PaymentSheet should block or allow
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
            fun link(link: PaymentSheet.LinkConfiguration): Builder = apply {
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
                primaryButtonColor = primaryButtonColor,
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
            ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class,
            ExperimentalCustomPaymentMethodsApi::class,
            WalletButtonsPreview::class,
            ShopPayPreview::class
        )
        @Suppress("DEPRECATION")
        internal fun newBuilder(): Builder = Builder(merchantDisplayName)
            .customer(customer)
            .googlePay(googlePay)
            .primaryButtonColor(primaryButtonColor)
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

    @Parcelize
    data class Address(
        /**
         * City, district, suburb, town, or village.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val city: String? = null,
        /**
         * Two-letter country code (ISO 3166-1 alpha-2).
         */
        val country: String? = null,
        /**
         * Address line 1 (e.g., street, PO Box, or company name).
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val line1: String? = null,
        /**
         * Address line 2 (e.g., apartment, suite, unit, or building).
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val line2: String? = null,
        /**
         * ZIP or postal code.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val postalCode: String? = null,
        /**
         * State, county, province, or region.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val state: String? = null
    ) : Parcelable {
        /**
         * [Address] builder for cleaner object creation from Java.
         */
        class Builder {
            private var city: String? = null
            private var country: String? = null
            private var line1: String? = null
            private var line2: String? = null
            private var postalCode: String? = null
            private var state: String? = null

            fun city(city: String?) = apply { this.city = city }
            fun country(country: String?) = apply { this.country = country }
            fun line1(line1: String?) = apply { this.line1 = line1 }
            fun line2(line2: String?) = apply { this.line2 = line2 }
            fun postalCode(postalCode: String?) = apply { this.postalCode = postalCode }
            fun state(state: String?) = apply { this.state = state }

            fun build() = Address(city, country, line1, line2, postalCode, state)
        }
    }

    @Parcelize
    data class BillingDetails(
        /**
         * The customer's billing address.
         */
        val address: Address? = null,
        /**
         * The customer's email.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val email: String? = null,
        /**
         * The customer's full name.
         * The value set is displayed in the payment sheet as-is. Depending on the payment method, the customer may be required to edit this value.
         */
        val name: String? = null,
        /**
         * The customer's phone number without formatting e.g. 5551234567
         */
        val phone: String? = null
    ) : Parcelable {
        internal fun isFilledOut(): Boolean {
            return address != null ||
                email != null ||
                name != null ||
                phone != null
        }

        /**
         * [BillingDetails] builder for cleaner object creation from Java.
         */
        class Builder {
            private var address: Address? = null
            private var email: String? = null
            private var name: String? = null
            private var phone: String? = null

            fun address(address: Address?) = apply { this.address = address }
            fun address(addressBuilder: Address.Builder) =
                apply { this.address = addressBuilder.build() }

            fun email(email: String?) = apply { this.email = email }
            fun name(name: String?) = apply { this.name = name }
            fun phone(phone: String?) = apply { this.phone = phone }

            fun build() = BillingDetails(address, email, name, phone)
        }
    }

    /**
     * Configuration for how billing details are collected during checkout.
     */
    @Parcelize
    data class BillingDetailsCollectionConfiguration(
        /**
         * How to collect the name field.
         */
        val name: CollectionMode = CollectionMode.Automatic,

        /**
         * How to collect the phone field.
         */
        val phone: CollectionMode = CollectionMode.Automatic,

        /**
         * How to collect the email field.
         */
        val email: CollectionMode = CollectionMode.Automatic,

        /**
         * How to collect the billing address.
         */
        val address: AddressCollectionMode = AddressCollectionMode.Automatic,

        /**
         * Whether the values included in [PaymentSheet.Configuration.defaultBillingDetails]
         * should be attached to the payment method, this includes fields that aren't displayed in the form.
         *
         * If `false` (the default), those values will only be used to prefill the corresponding fields in the form.
         */
        val attachDefaultsToPaymentMethod: Boolean = false,
    ) : Parcelable {

        internal val collectsName: Boolean
            get() = name == CollectionMode.Always

        internal val collectsEmail: Boolean
            get() = email == CollectionMode.Always

        internal val collectsPhone: Boolean
            get() = phone == CollectionMode.Always

        internal val collectsAnything: Boolean
            get() = name == CollectionMode.Always ||
                phone == CollectionMode.Always ||
                email == CollectionMode.Always ||
                address == AddressCollectionMode.Full

        private val collectsFullAddress: Boolean
            get() = address == AddressCollectionMode.Full

        internal fun toBillingAddressParameters(): GooglePayJsonFactory.BillingAddressParameters {
            val format = when (address) {
                AddressCollectionMode.Never,
                AddressCollectionMode.Automatic -> {
                    GooglePayJsonFactory.BillingAddressParameters.Format.Min
                }
                AddressCollectionMode.Full -> {
                    GooglePayJsonFactory.BillingAddressParameters.Format.Full
                }
            }

            return GooglePayJsonFactory.BillingAddressParameters(
                isRequired = collectsFullAddress || collectsPhone,
                format = format,
                isPhoneNumberRequired = collectsPhone,
            )
        }

        internal fun toBillingAddressConfig(): GooglePayPaymentMethodLauncher.BillingAddressConfig {
            val format = when (address) {
                AddressCollectionMode.Never,
                AddressCollectionMode.Automatic -> {
                    GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Min
                }
                AddressCollectionMode.Full -> {
                    GooglePayPaymentMethodLauncher.BillingAddressConfig.Format.Full
                }
            }

            return GooglePayPaymentMethodLauncher.BillingAddressConfig(
                isRequired = collectsFullAddress || collectsPhone,
                format = format,
                isPhoneNumberRequired = collectsPhone,
            )
        }

        /**
         * Billing details fields collection options.
         */
        enum class CollectionMode {
            /**
             * The field will be collected depending on the Payment Method's requirements.
             */
            Automatic,

            /**
             * The field will never be collected.
             * If this field is required by the Payment Method, you must provide it as part of `defaultBillingDetails`.
             */
            Never,

            /**
             * The field will always be collected, even if it isn't required for the Payment Method.
             */
            Always,
        }

        /**
         * Billing address collection options.
         */
        enum class AddressCollectionMode {
            /**
             * Only the fields required by the Payment Method will be collected, this may be none.
             */
            Automatic,

            /**
             * Address will never be collected.
             * If the Payment Method requires a billing address, you must provide it as part of `defaultBillingDetails`.
             */
            Never,

            /**
             * Collect the full billing address, regardless of the Payment Method requirements.
             */
            Full,
        }
    }

    /**
     * Defines a custom payment method type that can be displayed in Payment Element.
     */
    @Poko
    @Parcelize
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class CustomPaymentMethod internal constructor(
        val id: String,
        internal val subtitle: ResolvableString?,
        internal val disableBillingDetailCollection: Boolean,
    ) : Parcelable {
        @ExperimentalCustomPaymentMethodsApi
        constructor(
            /**
             * The unique identifier for this custom payment method type in the format of "cmpt_...".
             *
             * Obtained from the Stripe Dashboard at https://dashboard.stripe.com/settings/custom_payment_methods
             */
            id: String,

            /**
             * Optional subtitle text to be displayed below the custom payment method's display name.
             */
            @StringRes subtitle: Int?,

            /**
             * When true, Payment Element will not collect billing details for this custom payment method type
             * irregardless of the [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] settings.
             *
             * This has no effect if [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] is not
             * configured.
             */
            disableBillingDetailCollection: Boolean = true,
        ) : this(
            id = id,
            subtitle = subtitle?.resolvableString,
            disableBillingDetailCollection = disableBillingDetailCollection,
        )

        @ExperimentalCustomPaymentMethodsApi
        constructor(
            /**
             * The unique identifier for this custom payment method type in the format of "cmpt_...".
             *
             * Obtained from the Stripe Dashboard at https://dashboard.stripe.com/settings/custom_payment_methods
             */
            id: String,

            /**
             * Optional subtitle text string resource to be resolved and displayed below the custom payment method's
             * display name.
             */
            subtitle: String?,

            /**
             * When true, Payment Element will not collect billing details for this custom payment method type
             * irregardless of the [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] settings.
             *
             * This has no effect if [PaymentSheet.Configuration.billingDetailsCollectionConfiguration] is not
             * configured.
             */
            disableBillingDetailCollection: Boolean = true,
        ) : this(
            id = id,
            subtitle = subtitle?.resolvableString,
            disableBillingDetailCollection = disableBillingDetailCollection,
        )
    }

    internal sealed interface CustomerAccessType : Parcelable {
        val analyticsValue: String

        @Parcelize
        data class LegacyCustomerEphemeralKey(val ephemeralKeySecret: String) : CustomerAccessType {
            @IgnoredOnParcel
            override val analyticsValue: String = "legacy"
        }

        @Parcelize
        data class CustomerSession(val customerSessionClientSecret: String) : CustomerAccessType {
            @IgnoredOnParcel
            override val analyticsValue: String = "customer_session"
        }
    }

    /**
     * @param environment The Google Pay environment to use. See
     * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
     * for more information.
     * @param countryCode The two-letter ISO 3166 code of the country of your business, e.g. "US".
     * See your account's country value [here](https://dashboard.stripe.com/settings/account).
     * @param currencyCode The three-letter ISO 4217 alphabetic currency code, e.g. "USD" or "EUR".
     * Required in order to support Google Pay when processing a Setup Intent.
     * @param amount An optional amount to display for setup intents. Google Pay may or may not
     * display this amount depending on its own internal logic. Defaults to 0 if none is provided.
     * @param label An optional label to display with the amount. Google Pay may or may not display
     * this label depending on its own internal logic. Defaults to a generic label if none is
     * provided.
     * @param buttonType The Google Pay button type to use. Set to "Pay" by default. See
     * [Google's documentation](https://developers.google.com/android/reference/com/google/android/gms/wallet/Wallet.WalletOptions#environment)
     * for more information on button types.
     */
    @Parcelize
    data class GooglePayConfiguration @JvmOverloads constructor(
        val environment: Environment,
        val countryCode: String,
        val currencyCode: String? = null,
        val amount: Long? = null,
        val label: String? = null,
        val buttonType: ButtonType = ButtonType.Pay
    ) : Parcelable {

        enum class Environment {
            Production,
            Test,
        }

        @Suppress("MaxLineLength")
        /**
         * Google Pay button type options
         *
         * See [Google's documentation](https://developers.google.com/pay/api/android/reference/request-objects#ButtonOptions) for more information on button types.
         */
        enum class ButtonType {
            /**
             * Displays "Buy with" alongside the Google Pay logo.
             */
            Buy,

            /**
             * Displays "Book with" alongside the Google Pay logo.
             */
            Book,

            /**
             * Displays "Checkout with" alongside the Google Pay logo.
             */
            Checkout,

            /**
             * Displays "Donate with" alongside the Google Pay logo.
             */
            Donate,

            /**
             * Displays "Order with" alongside the Google Pay logo.
             */
            Order,

            /**
             * Displays "Pay with" alongside the Google Pay logo.
             */
            Pay,

            /**
             * Displays "Subscribe with" alongside the Google Pay logo.
             */
            Subscribe,

            /**
             * Displays only the Google Pay logo.
             */
            Plain
        }
    }

    /**
     * Configuration related to Link.
     */
    @Poko
    @Parcelize
    class LinkConfiguration internal constructor(
        internal val display: Display,
        internal val collectMissingBillingDetailsForExistingPaymentMethods: Boolean,
        internal val allowUserEmailEdits: Boolean,
    ) : Parcelable {

        @JvmOverloads
        constructor(
            display: Display = Display.Automatic
        ) : this(
            display = display,
            collectMissingBillingDetailsForExistingPaymentMethods = true,
            allowUserEmailEdits = true,
        )

        internal val shouldDisplay: Boolean
            get() = when (display) {
                Display.Automatic -> true
                Display.Never -> false
            }

        class Builder {
            private var display: Display = Display.Automatic
            private var collectMissingBillingDetailsForExistingPaymentMethods: Boolean = true

            fun display(display: Display) = apply {
                this.display = display
            }

            @CollectMissingLinkBillingDetailsPreview
            fun collectMissingBillingDetailsForExistingPaymentMethods(
                collectMissingBillingDetailsForExistingPaymentMethods: Boolean
            ) = apply {
                this.collectMissingBillingDetailsForExistingPaymentMethods =
                    collectMissingBillingDetailsForExistingPaymentMethods
            }

            fun build() = LinkConfiguration(
                display = display,
                collectMissingBillingDetailsForExistingPaymentMethods =
                collectMissingBillingDetailsForExistingPaymentMethods,
                allowUserEmailEdits = true,
            )
        }

        /**
         * Display configuration for Link
         */
        enum class Display {
            /**
             * Link will be displayed when available.
             */
            Automatic,

            /**
             * Link will never be displayed.
             */
            Never;

            internal val analyticsValue: String
                get() = when (this) {
                    Automatic -> "automatic"
                    Never -> "never"
                }
        }
    }

    /**
     * Configuration for wallet buttons
     */
    @Poko
    @Parcelize
    class WalletButtonsConfiguration(
        /**
         * Indicates the `WalletButtons` API will be used. This helps
         * ensure the Payment Element is not initialized with a displayed
         * wallet option as the default payment option.
         */
        val willDisplayExternally: Boolean = false,

        /**
         * Identifies the list of wallets that can be shown in `WalletButtons`. Wallets
         * are identified by their wallet identifier (google_pay, link, shop_pay). An
         * empty list means all wallets will be shown.
         */
        val walletsToShow: List<String> = emptyList(),
    ) : Parcelable

    /**
     * Configuration related to Shop Pay, which only applies when using wallet buttons.
     *
     * @param shopId The corresponding store's shopId.
     * @param billingAddressRequired Whether or not billing address is required. Defaults to `true`.
     * @param emailRequired Whether or not email is required. Defaults to `true`.
     * @param shippingAddressRequired Whether or not to collect the customer's shipping address.
     * @param lineItems An array of [LineItem] objects. These are shown as line items in the
     * payment interface, if line items are supported. You can represent discounts as negative
     * amount [LineItem]s.
     * @param shippingRates A list of [ShippingRate] objects. The first shipping rate listed
     * appears in the payment interface as the default option.
     */
    @Poko
    @Parcelize
    class ShopPayConfiguration(
        val shopId: String,
        val billingAddressRequired: Boolean = true,
        val emailRequired: Boolean = true,
        val shippingAddressRequired: Boolean,
        val allowedShippingCountries: List<String>,
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>
    ) : Parcelable {
        /**
         * A type used to describe a single item for in the Shop Pay wallet UI.
         */
        @Poko
        @Parcelize
        class LineItem(
            val name: String,
            val amount: Int
        ) : Parcelable

        /**
         * A shipping rate option.
         */
        @Poko
        @Parcelize
        class ShippingRate(
            val id: String,
            val amount: Int,
            val displayName: String,
            val deliveryEstimate: DeliveryEstimate?
        ) : Parcelable

        /**
         * Type used to describe DeliveryEstimates for shipping.
         * See https://docs.stripe.com/js/elements_object/create_express_checkout_element#express_checkout_element_create-options-shippingRates-deliveryEstimate
         */
        sealed interface DeliveryEstimate : Parcelable {
            @Poko
            @Parcelize
            class Range(
                val maximum: DeliveryEstimateUnit?,
                val minimum: DeliveryEstimateUnit?
            ) : DeliveryEstimate

            @Poko
            @Parcelize
            class Text(
                val value: String
            ) : DeliveryEstimate

            @Poko
            @Parcelize
            class DeliveryEstimateUnit(
                val unit: TimeUnit,
                val value: Int
            ) : Parcelable {

                enum class TimeUnit {
                    HOUR,
                    DAY,
                    BUSINESS_DAY,
                    WEEK,
                    MONTH
                }
            }
        }
    }

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
            configuration: Configuration? = null,
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
            configuration: Configuration? = null,
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
            configuration: Configuration? = null,
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
         * Builder utility to set optional callbacks for [PaymentSheet.FlowController].
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
            @ExperimentalAnalyticEventCallbackApi
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
             * Returns a [PaymentSheet.FlowController].
             *
             * @param activity The Activity that is presenting [PaymentSheet.FlowController].
             */
            fun build(activity: ComponentActivity): FlowController {
                initializeCallbacks()
                return FlowControllerFactory(activity, paymentOptionCallback, resultCallback).create()
            }

            /**
             * Returns a [PaymentSheet.FlowController].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet.FlowController].
             */
            fun build(fragment: Fragment): FlowController {
                initializeCallbacks()
                return FlowControllerFactory(fragment, paymentOptionCallback, resultCallback).create()
            }

            /**
             * Returns a [PaymentSheet.FlowController] composable.
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
            /**
             * Create a [FlowController] that you configure with a client secret by calling
             * [configureWithPaymentIntent] or [configureWithSetupIntent].
             *
             * @param activity The Activity that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback).build(activity)"
                )
            )
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with a client secret by calling
             * [configureWithPaymentIntent] or [configureWithSetupIntent].
             *
             * Use this creator if external payment methods are specified in your [Configuration].
             *
             * @param activity The Activity that is presenting [PaymentSheet].
             * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment
             * method.
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
                        ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                        ".build(activity)"
                )
            )
            fun create(
                activity: ComponentActivity,
                externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                setFlowControllerCallbacks(
                    PaymentElementCallbacks.Builder()
                        .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                        .build()
                )
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with an [IntentConfiguration] by calling
             * [configureWithIntentConfiguration].
             *
             * @param activity The Activity that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param createIntentCallback Called when the customer confirms the payment or setup.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
                        ".createIntentCallback(createIntentCallback)" +
                        ".build(activity)"
                )
            )
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                createIntentCallback: CreateIntentCallback,
                paymentResultCallback: PaymentSheetResultCallback,
            ): FlowController {
                setFlowControllerCallbacks(
                    PaymentElementCallbacks.Builder()
                        .createIntentCallback(createIntentCallback)
                        .build()
                )
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with an [IntentConfiguration] by calling
             * [configureWithIntentConfiguration].
             *
             * Use this creator if external payment methods are specified in your [Configuration].
             *
             * @param activity The Activity that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment
             * method.
             * @param createIntentCallback Called when the customer confirms the payment or setup.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
                        ".createIntentCallback(createIntentCallback)" +
                        ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                        ".build(activity)"
                )
            )
            fun create(
                activity: ComponentActivity,
                paymentOptionCallback: PaymentOptionCallback,
                externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
                createIntentCallback: CreateIntentCallback,
                paymentResultCallback: PaymentSheetResultCallback,
            ): FlowController {
                setFlowControllerCallbacks(
                    PaymentElementCallbacks.Builder()
                        .createIntentCallback(createIntentCallback)
                        .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                        .build()
                )
                return FlowControllerFactory(
                    activity,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with a client secret by calling
             * [configureWithPaymentIntent] or [configureWithSetupIntent].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet].
             * @param paymentOptionCallback called when the customer's [PaymentOption] selection changes.
             * @param paymentResultCallback called when a [PaymentSheetResult] is available.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback).build(fragment)"
                )
            )
            fun create(
                fragment: Fragment,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with a client secret by calling
             * [configureWithPaymentIntent] or [configureWithSetupIntent].
             *
             * Use this creator if external payment methods are specified in your [Configuration].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet].
             * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment
             * method.
             * @param paymentOptionCallback called when the customer's [PaymentOption] selection changes.
             * @param paymentResultCallback called when a [PaymentSheetResult] is available.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
                        ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                        ".build(fragment)"
                )
            )
            fun create(
                fragment: Fragment,
                externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
                paymentOptionCallback: PaymentOptionCallback,
                paymentResultCallback: PaymentSheetResultCallback
            ): FlowController {
                setFlowControllerCallbacks(
                    PaymentElementCallbacks.Builder()
                        .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                        .build()
                )
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with an [IntentConfiguration] by calling
             * [configureWithIntentConfiguration].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param createIntentCallback Called when the customer confirms the payment or setup.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
                        ".createIntentCallback(createIntentCallback)" +
                        ".build(fragment)"
                )
            )
            fun create(
                fragment: Fragment,
                paymentOptionCallback: PaymentOptionCallback,
                createIntentCallback: CreateIntentCallback,
                paymentResultCallback: PaymentSheetResultCallback,
            ): FlowController {
                setFlowControllerCallbacks(
                    PaymentElementCallbacks.Builder()
                        .createIntentCallback(createIntentCallback)
                        .build()
                )
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }

            /**
             * Create a [FlowController] that you configure with an [IntentConfiguration] by calling
             * [configureWithIntentConfiguration].
             *
             * Use this creator if external payment methods are specified in your [Configuration].
             *
             * @param fragment The Fragment that is presenting [PaymentSheet].
             * @param paymentOptionCallback Called when the customer's selected payment method
             * changes.
             * @param externalPaymentMethodConfirmHandler Called when a user confirms payment with an external payment
             * method.
             * @param createIntentCallback Called when the customer confirms the payment or setup.
             * @param paymentResultCallback Called with the result of the payment after
             * [PaymentSheet] is dismissed.
             */
            @JvmStatic
            @Deprecated(
                message = "This will be removed in a future release.",
                replaceWith = ReplaceWith(
                    "FlowController.Builder(paymentResultCallback, paymentOptionCallback)" +
                        ".createIntentCallback(createIntentCallback)" +
                        ".externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)" +
                        ".build(fragment)"
                )
            )
            fun create(
                fragment: Fragment,
                paymentOptionCallback: PaymentOptionCallback,
                createIntentCallback: CreateIntentCallback,
                externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler,
                paymentResultCallback: PaymentSheetResultCallback,
            ): FlowController {
                setFlowControllerCallbacks(
                    PaymentElementCallbacks.Builder()
                        .createIntentCallback(createIntentCallback)
                        .externalPaymentMethodConfirmHandler(externalPaymentMethodConfirmHandler)
                        .build()
                )
                return FlowControllerFactory(
                    fragment,
                    paymentOptionCallback,
                    paymentResultCallback
                ).create()
            }
        }
    }

    companion object {
        private fun setPaymentSheetCallbacks(callbacks: PaymentElementCallbacks) {
            PaymentElementCallbackReferences[PAYMENT_SHEET_DEFAULT_CALLBACK_IDENTIFIER] = callbacks
        }

        private fun setFlowControllerCallbacks(callbacks: PaymentElementCallbacks) {
            PaymentElementCallbackReferences[FLOW_CONTROLLER_DEFAULT_CALLBACK_IDENTIFIER] = callbacks
        }

        /**
         * Deletes all persisted authentication state associated with a customer.
         *
         * You must call this method when the user logs out from your app.
         * This will ensure that any persisted authentication state in PaymentSheet, such as
         * authentication cookies, is also cleared during logout.
         *
         * @param context the Application [Context].
         */
        fun resetCustomer(context: Context) {
            LinkStore(context).clear()
        }
    }
}
