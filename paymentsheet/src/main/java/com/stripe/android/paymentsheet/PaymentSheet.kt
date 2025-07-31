package com.stripe.android.paymentsheet

import android.content.Context
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.elements.AddressDetails
import com.stripe.android.elements.AllowsRemovalOfLastSavedPaymentMethodPreview
import com.stripe.android.elements.Appearance
import com.stripe.android.elements.BillingDetails
import com.stripe.android.elements.BillingDetailsCollectionConfiguration
import com.stripe.android.elements.CardBrandAcceptance
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.LinkConfiguration
import com.stripe.android.link.account.LinkStore
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ConfirmCustomPaymentMethodCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentelement.PreparePaymentMethodHandler
import com.stripe.android.paymentelement.ShopPayPreview
import com.stripe.android.paymentelement.WalletButtonsPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.LineItem
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.ShippingRate
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

    /**
     * Contains information needed to render [PaymentSheet]. The values are used to calculate
     * the payment methods displayed and influence the UI.
     *
     * **Note**: The [PaymentIntent] or [SetupIntent] you create on your server must have the same
     * values or the payment/setup will fail.
     *
     * @param mode Whether [PaymentSheet] should present a payment or setup flow.
     * @param paymentMethodTypes The payment methods types to display. If empty, we dynamically
     * determine the payment method types using your [Stripe Dashboard settings](https://dashboard.stripe.com/settings/payment_methods).
     * @param paymentMethodConfigurationId The configuration ID (if any) for the selected payment method configuration.
     * See https://stripe.com/docs/payments/multiple-payment-method-configs for more information.
     * @param onBehalfOf The account (if any) for which the funds of the intent are intended. See
     * [our docs](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-on_behalf_of) for more info.
     */
    @Poko
    @Parcelize
    class IntentConfiguration internal constructor(
        val mode: Mode,
        val paymentMethodTypes: List<String> = emptyList(),
        val paymentMethodConfigurationId: String? = null,
        val onBehalfOf: String? = null,
        internal val requireCvcRecollection: Boolean = false,
        internal val intentBehavior: IntentBehavior = IntentBehavior.Default,
    ) : Parcelable {
        @JvmOverloads
        constructor(
            mode: Mode,
            paymentMethodTypes: List<String> = emptyList(),
            paymentMethodConfigurationId: String? = null,
            onBehalfOf: String? = null,
            requireCvcRecollection: Boolean = false,
        ) : this(
            mode = mode,
            paymentMethodTypes = paymentMethodTypes,
            paymentMethodConfigurationId = paymentMethodConfigurationId,
            onBehalfOf = onBehalfOf,
            requireCvcRecollection = requireCvcRecollection,
            intentBehavior = IntentBehavior.Default,
        )

        @SharedPaymentTokenSessionPreview
        @JvmOverloads
        constructor(
            sharedPaymentTokenSessionWithMode: Mode,
            sellerDetails: SellerDetails?,
            paymentMethodTypes: List<String> = emptyList(),
            paymentMethodConfigurationId: String? = null,
            onBehalfOf: String? = null,
            requireCvcRecollection: Boolean = false,
        ) : this(
            mode = sharedPaymentTokenSessionWithMode,
            paymentMethodTypes = paymentMethodTypes,
            paymentMethodConfigurationId = paymentMethodConfigurationId,
            onBehalfOf = onBehalfOf,
            requireCvcRecollection = requireCvcRecollection,
            intentBehavior = IntentBehavior.SharedPaymentToken(sellerDetails),
        )

        /**
         * Contains information about the desired payment or setup flow.
         */
        sealed class Mode : Parcelable {

            internal abstract val setupFutureUse: SetupFutureUse?
            internal abstract val captureMethod: CaptureMethod?

            /**
             * Use this if your integration creates a [PaymentIntent].
             *
             * @param amount Amount intended to be collected in the smallest currency unit
             * (e.g. 100 cents to charge $1.00). Shown in Google Pay, Buy now pay later UIs, the Pay
             * button, and influences available payment methods. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-amount) for more info.
             * @param currency Three-letter ISO currency code. Filters out payment methods based on
             * supported currency. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-currency) for more info.
             * @param setupFutureUse Indicates that you intend to make future payments. See
             * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage) for more info.
             * @param captureMethod Controls when the funds will be captured from the customer's
             * account. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-capture_method) for more info.
             * @param paymentMethodOptions Additional payment method options params. See [our docs](https://docs.stripe.com/api/payment_intents/create#create_payment_intent-payment_method_options) for more info.
             */
            @Poko
            @Parcelize
            @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
            class Payment @JvmOverloads constructor(
                val amount: Long,
                val currency: String,
                override val setupFutureUse: SetupFutureUse? = null,
                override val captureMethod: CaptureMethod = CaptureMethod.Automatic,
                internal val paymentMethodOptions: PaymentMethodOptions?
            ) : Mode() {

                constructor(
                    amount: Long,
                    currency: String,
                    setupFutureUse: SetupFutureUse? = null,
                    captureMethod: CaptureMethod = CaptureMethod.Automatic,
                ) : this(
                    amount = amount,
                    currency = currency,
                    setupFutureUse = setupFutureUse,
                    captureMethod = captureMethod,
                    paymentMethodOptions = null
                )

                @Parcelize
                @Poko
                @PaymentMethodOptionsSetupFutureUsagePreview
                class PaymentMethodOptions(
                    internal val setupFutureUsageValues: Map<PaymentMethod.Type, SetupFutureUse>
                ) : Parcelable
            }

            /**
             * Use this if your integration creates a [SetupIntent].
             *
             * @param currency Three-letter ISO currency code. Filters out payment methods based on
             * supported currency. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-currency) for more info.
             * @param setupFutureUse Indicates that you intend to make future payments. See
             * [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage) for more info.
             */
            @Poko
            @Parcelize
            class Setup @JvmOverloads constructor(
                val currency: String? = null,
                override val setupFutureUse: SetupFutureUse = SetupFutureUse.OffSession,
            ) : Mode() {

                override val captureMethod: CaptureMethod?
                    get() = null
            }
        }

        /**
         * Indicates that you intend to make future payments with this [PaymentIntent]'s payment
         * method. See [our docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-setup_future_usage) for more info.
         */
        enum class SetupFutureUse {

            /**
             * Use this if you intend to only reuse the payment method when your customer is present
             * in your checkout flow.
             */
            OnSession,

            /**
             * Use this if your customer may or may not be present in your checkout flow.
             */
            OffSession,

            /**
             * Use none if you do not intend to reuse this payment method and want to override the top-level
             * setup_future_usage value for this payment method.
             */
            None
        }

        /**
         * Controls when the funds will be captured.
         *
         * See [docs](https://stripe.com/docs/api/payment_intents/create#create_payment_intent-capture_method).
         */
        enum class CaptureMethod {

            /**
             * Stripe automatically captures funds when the customer authorizes the payment.
             */
            Automatic,

            /**
             * Stripe asynchronously captures funds when the customer authorizes the payment.
             * Recommended over [CaptureMethod.Automatic] due to improved latency, but may require
             * additional integration changes.
             */
            AutomaticAsync,

            /**
             * Place a hold on the funds when the customer authorizes the payment, but don't capture
             * the funds until later.
             *
             * **Note**: Not all payment methods support this.
             */
            Manual,
        }

        @SharedPaymentTokenSessionPreview
        @Parcelize
        @Poko
        class SellerDetails(
            val networkId: String,
            val externalId: String,
        ) : Parcelable

        @OptIn(SharedPaymentTokenSessionPreview::class)
        internal sealed interface IntentBehavior : Parcelable {
            @Parcelize
            data object Default : IntentBehavior

            @Parcelize
            data class SharedPaymentToken(
                val sellerDetails: SellerDetails?,
            ) : IntentBehavior
        }

        companion object {

            /**
             * Pass this as the client secret into [CreateIntentResult.Success] to force
             * [PaymentSheet] to show success, dismiss the sheet without confirming the intent, and
             * return [PaymentSheetResult.Completed].
             *
             * **Note**: If provided, the SDK performs no action to complete the payment or setup.
             * It doesn't confirm a [PaymentIntent] or [SetupIntent] or handle next actions. You
             * should only use this if your integration can't create a [PaymentIntent] or
             * [SetupIntent]. It is your responsibility to ensure that you only pass this value if
             * the payment or setup is successful.
             */
            @DelicatePaymentSheetApi
            const val COMPLETE_WITHOUT_CONFIRMING_INTENT =
                IntentConfirmationInterceptor.COMPLETE_WITHOUT_CONFIRMING_INTENT
        }
    }

    /** Configuration for [PaymentSheet] **/
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
             * If set, the customer can select a previously saved payment method within PaymentSheet.
             */
            fun customer(customer: CustomerConfiguration?) =
                apply { this.customer = customer }

            /**
             * Configuration related to the Stripe Customer making a payment.
             *
             * If set, PaymentSheet displays Google Pay as a payment option.
             */
            fun googlePay(googlePay: GooglePayConfiguration?) =
                apply { this.googlePay = googlePay }

            /**
             * The billing information for the customer.
             *
             * If set, PaymentSheet will pre-populate the form fields with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the PaymentSheet UI.
             */
            fun defaultBillingDetails(defaultBillingDetails: BillingDetails?) =
                apply { this.defaultBillingDetails = defaultBillingDetails }

            /**
             * The shipping information for the customer.
             * If set, PaymentSheet will pre-populate the form fields with the values provided.
             * This is used to display a "Billing address is same as shipping" checkbox if `defaultBillingDetails` is not provided.
             * If `name` and `line1` are populated, it's also [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping) during payment.
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
             * Set this to `true` if you collect shipping addresses via [shippingDetails] or
             * [FlowController.shippingDetails].
             *
             * **Note**: PaymentSheet considers this property `true` if `shipping` details are present
             * on the PaymentIntent when PaymentSheet loads.
             */
            fun allowsPaymentMethodsRequiringShippingAddress(
                allowsPaymentMethodsRequiringShippingAddress: Boolean,
            ) = apply {
                this.allowsPaymentMethodsRequiringShippingAddress =
                    allowsPaymentMethodsRequiringShippingAddress
            }

            /**
             * Describes the appearance of Payment Sheet.
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
             * The layout of payment methods in PaymentSheet. Defaults to [PaymentSheet.PaymentMethodLayout.Horizontal].
             * @see [PaymentSheet.PaymentMethodLayout] for the list of available layouts.
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
     * Defines the layout orientations available for displaying payment methods in PaymentSheet.
     */
    enum class PaymentMethodLayout {
        /**
         * Payment methods are arranged horizontally.
         * Users can swipe left or right to navigate through different payment methods.
         */
        Horizontal,

        /**
         * Payment methods are arranged vertically.
         * Users can scroll up or down to navigate through different payment methods.
         */
        Vertical,

        /**
         * This lets Stripe choose the best layout for payment methods in the sheet.
         */
        Automatic
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
        internal val willDisplayExternally: Boolean = false,

        /**
         * Identifies the list of wallets that can be shown in `WalletButtons`. Wallets
         * are identified by their wallet identifier (google_pay, link, shop_pay). An
         * empty list means all wallets will be shown.
         */
        internal val walletsToShow: List<String> = emptyList(),
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
