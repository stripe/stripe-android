package com.stripe.android.paymentsheet

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.DimenRes
import androidx.annotation.FontRes
import androidx.annotation.RestrictTo
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.fragment.app.Fragment
import com.stripe.android.CollectMissingLinkBillingDetailsPreview
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.GooglePayJsonFactory
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.googlepaylauncher.GooglePayPaymentMethodLauncher
import com.stripe.android.link.account.LinkStore
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.AddressAutocompletePreview
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.AppearanceAPIAdditionsPreview
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
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerFactory
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.uicore.PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR
import com.stripe.android.uicore.StripeThemeDefaults
import com.stripe.android.uicore.getRawValueFromDimenResource
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

    @Parcelize
    data class Appearance
    @OptIn(AppearanceAPIAdditionsPreview::class)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    constructor(
        /**
         * Describes the colors used while the system is in light mode.
         */
        val colorsLight: Colors = Colors.defaultLight,

        /**
         * Describes the colors used while the system is in dark mode.
         */
        val colorsDark: Colors = Colors.defaultDark,

        /**
         * Describes the appearance of shapes.
         */
        val shapes: Shapes = Shapes.default,

        /**
         * Describes the typography used for text.
         */
        val typography: Typography = Typography.default,

        /**
         * Describes the appearance of the primary button (e.g., the "Pay" button).
         */
        val primaryButton: PrimaryButton = PrimaryButton(),

        /**
         * Describes the appearance of the Embedded Payment Element
         */
        internal val embeddedAppearance: Embedded = Embedded.default,

        /**
         * Describes the inset values used for all forms
         */
        internal val formInsetValues: Insets = Insets.defaultFormInsetValues,

        /**
         * Defines spacing between conceptual sections of a form. This does not control padding
         * between input fields. Negative values will also be ignored and default spacing will
         * be applied.
         */
        internal val sectionSpacing: Spacing = Spacing.defaultSectionSpacing,

        /**
         * Defines spacing inside the input fields of a form.
         */
        internal val textFieldInsets: Insets = Insets.defaultTextFieldInsets,

        /**
         * Defines the visual style of icons in Payment Element
         */
        internal val iconStyle: IconStyle = IconStyle.default,

        internal val verticalModeRowPadding: Float = StripeThemeDefaults.verticalModeRowPadding,
    ) : Parcelable {
        constructor() : this(
            colorsLight = Colors.defaultLight,
            colorsDark = Colors.defaultDark,
            shapes = Shapes.default,
            typography = Typography.default,
            primaryButton = PrimaryButton(),
        )

        constructor(
            colorsLight: Colors = Colors.defaultLight,
            colorsDark: Colors = Colors.defaultDark,
            shapes: Shapes = Shapes.default,
            typography: Typography = Typography.default,
            primaryButton: PrimaryButton = PrimaryButton(),
        ) : this(
            colorsLight = colorsLight,
            colorsDark = colorsDark,
            shapes = shapes,
            typography = typography,
            primaryButton = primaryButton,
            embeddedAppearance = Embedded.default
        )

        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            colorsLight: Colors = Colors.defaultLight,
            colorsDark: Colors = Colors.defaultDark,
            shapes: Shapes = Shapes.default,
            typography: Typography = Typography.default,
            primaryButton: PrimaryButton = PrimaryButton(),
            embeddedAppearance: Embedded = Embedded.default,
            formInsetValues: Insets = Insets.defaultFormInsetValues,
        ) : this(
            colorsLight = colorsLight,
            colorsDark = colorsDark,
            shapes = shapes,
            typography = typography,
            primaryButton = primaryButton,
            embeddedAppearance = embeddedAppearance,
            formInsetValues = formInsetValues,
            sectionSpacing = Spacing.defaultSectionSpacing,
        )

        fun getColors(isDark: Boolean): Colors {
            return if (isDark) colorsDark else colorsLight
        }

        @Parcelize
        @Poko
        class Embedded(
            internal val style: RowStyle
        ) : Parcelable {

            internal companion object {
                val default = Embedded(
                    style = RowStyle.FlatWithRadio.default
                )
            }

            @Parcelize
            sealed class RowStyle : Parcelable {

                internal abstract fun hasSeparators(): Boolean
                internal abstract fun startSeparatorHasDefaultInset(): Boolean

                @Parcelize
                @Poko
                class FlatWithRadio(
                    /**
                     * The thickness of the separator line between rows.
                     */
                    internal val separatorThicknessDp: Float,

                    /**
                     * The start inset of the separator line between rows.
                     */
                    internal val startSeparatorInsetDp: Float,

                    /**
                     * The end inset of the separator line between rows.
                     */
                    internal val endSeparatorInsetDp: Float,

                    /**
                     * Determines if the top separator is visible at the top of the Embedded Mobile Payment Element.
                     */
                    internal val topSeparatorEnabled: Boolean,

                    /**
                     * Determines if the bottom separator is visible at the bottom of the Embedded Mobile Payment
                     * Element.
                     */
                    internal val bottomSeparatorEnabled: Boolean,

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    internal val additionalVerticalInsetsDp: Float,

                    /**
                     * Horizontal insets applied to a payment method row.
                     */
                    internal val horizontalInsetsDp: Float,

                    /**
                     * Describes the colors used while the system is in light mode.
                     */
                    internal val colorsLight: Colors,

                    /**
                     * Describes the colors used while the system is in dark mode.
                     */
                    internal val colorsDark: Colors
                ) : RowStyle() {
                    constructor(
                        context: Context,
                        @DimenRes separatorThicknessRes: Int,
                        @DimenRes startSeparatorInsetRes: Int,
                        @DimenRes endSeparatorInsetRes: Int,
                        topSeparatorEnabled: Boolean,
                        bottomSeparatorEnabled: Boolean,
                        @DimenRes additionalVerticalInsetsRes: Int,
                        @DimenRes horizontalInsetsRes: Int,
                        colorsLight: Colors,
                        colorsDark: Colors
                    ) : this(
                        separatorThicknessDp = context.getRawValueFromDimenResource(separatorThicknessRes),
                        startSeparatorInsetDp = context.getRawValueFromDimenResource(startSeparatorInsetRes),
                        endSeparatorInsetDp = context.getRawValueFromDimenResource(endSeparatorInsetRes),
                        topSeparatorEnabled = topSeparatorEnabled,
                        bottomSeparatorEnabled = bottomSeparatorEnabled,
                        additionalVerticalInsetsDp = context.getRawValueFromDimenResource(additionalVerticalInsetsRes),
                        horizontalInsetsDp = context.getRawValueFromDimenResource(horizontalInsetsRes),
                        colorsLight = colorsLight,
                        colorsDark = colorsDark
                    )

                    override fun hasSeparators() = true
                    override fun startSeparatorHasDefaultInset() = true
                    internal fun getColors(isDark: Boolean): Colors = if (isDark) colorsDark else colorsLight

                    @Parcelize
                    @Poko
                    class Colors(
                        /**
                         * The color of the separator line between rows.
                         */
                        @ColorInt
                        internal val separatorColor: Int,

                        /**
                         * The color of the radio button when selected.
                         */
                        @ColorInt
                        internal val selectedColor: Int,

                        /**
                         * The color of the radio button when unselected.
                         */
                        @ColorInt
                        internal val unselectedColor: Int,
                    ) : Parcelable

                    internal companion object {
                        val default = FlatWithRadio(
                            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                            startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                            endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
                            horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
                            colorsLight = Colors(
                                separatorColor = StripeThemeDefaults.radioColorsLight.separatorColor.toArgb(),
                                selectedColor = StripeThemeDefaults.radioColorsLight.selectedColor.toArgb(),
                                unselectedColor = StripeThemeDefaults.radioColorsLight.unselectedColor.toArgb()
                            ),
                            colorsDark = Colors(
                                separatorColor = StripeThemeDefaults.radioColorsDark.separatorColor.toArgb(),
                                selectedColor = StripeThemeDefaults.radioColorsDark.selectedColor.toArgb(),
                                unselectedColor = StripeThemeDefaults.radioColorsDark.unselectedColor.toArgb()
                            ),
                        )
                    }
                }

                @Parcelize
                @Poko
                class FlatWithCheckmark(
                    /**
                     * The thickness of the separator line between rows.
                     */
                    internal val separatorThicknessDp: Float,

                    /**
                     * The start inset of the separator line between rows.
                     */
                    internal val startSeparatorInsetDp: Float,

                    /**
                     * The end inset of the separator line between rows.
                     */
                    internal val endSeparatorInsetDp: Float,

                    /**
                     * Determines if the top separator is visible at the top of the Embedded Mobile Payment Element.
                     */
                    internal val topSeparatorEnabled: Boolean,

                    /**
                     * Determines if the bottom separator is visible at the bottom of the Embedded Mobile Payment
                     * Element.
                     */
                    internal val bottomSeparatorEnabled: Boolean,

                    /**
                     * Inset of the checkmark from the end of the row
                     */
                    internal val checkmarkInsetDp: Float,

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    internal val additionalVerticalInsetsDp: Float,

                    /**
                     * Horizontal insets applied to a payment method row.
                     */
                    internal val horizontalInsetsDp: Float,

                    /**
                     * Describes the colors used while the system is in light mode.
                     */
                    internal val colorsLight: Colors,

                    /**
                     * Describes the colors used while the system is in dark mode.
                     */
                    internal val colorsDark: Colors
                ) : RowStyle() {
                    constructor(
                        context: Context,
                        @DimenRes separatorThicknessRes: Int,
                        @DimenRes startSeparatorInsetRes: Int,
                        @DimenRes endSeparatorInsetRes: Int,
                        topSeparatorEnabled: Boolean,
                        bottomSeparatorEnabled: Boolean,
                        @DimenRes checkmarkInsetRes: Int,
                        @DimenRes additionalVerticalInsetsRes: Int,
                        @DimenRes horizontalInsetsRes: Int,
                        colorsLight: Colors,
                        colorsDark: Colors
                    ) : this(
                        separatorThicknessDp = context.getRawValueFromDimenResource(separatorThicknessRes),
                        startSeparatorInsetDp = context.getRawValueFromDimenResource(startSeparatorInsetRes),
                        endSeparatorInsetDp = context.getRawValueFromDimenResource(endSeparatorInsetRes),
                        topSeparatorEnabled = topSeparatorEnabled,
                        bottomSeparatorEnabled = bottomSeparatorEnabled,
                        checkmarkInsetDp = context.getRawValueFromDimenResource(checkmarkInsetRes),
                        additionalVerticalInsetsDp = context.getRawValueFromDimenResource(additionalVerticalInsetsRes),
                        horizontalInsetsDp = context.getRawValueFromDimenResource(horizontalInsetsRes),
                        colorsLight = colorsLight,
                        colorsDark = colorsDark
                    )

                    @Parcelize
                    @Poko
                    class Colors(
                        /**
                         * The color of the separator line between rows.
                         */
                        @ColorInt
                        internal val separatorColor: Int,

                        /**
                         * The color of the checkmark.
                         */
                        @ColorInt
                        internal val checkmarkColor: Int,
                    ) : Parcelable

                    override fun hasSeparators() = true
                    override fun startSeparatorHasDefaultInset() = false
                    internal fun getColors(isDark: Boolean): Colors = if (isDark) colorsDark else colorsLight

                    internal companion object {
                        val default = FlatWithCheckmark(
                            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                            startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                            endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                            checkmarkInsetDp = StripeThemeDefaults.embeddedCommon.checkmarkInsetDp,
                            additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
                            horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
                            colorsLight = Colors(
                                separatorColor = StripeThemeDefaults.checkmarkColorsLight.separatorColor.toArgb(),
                                checkmarkColor = StripeThemeDefaults.checkmarkColorsLight.checkmarkColor.toArgb()
                            ),
                            colorsDark = Colors(
                                separatorColor = StripeThemeDefaults.checkmarkColorsDark.separatorColor.toArgb(),
                                checkmarkColor = StripeThemeDefaults.checkmarkColorsDark.checkmarkColor.toArgb()
                            )
                        )
                    }
                }

                @Parcelize
                @Poko
                class FloatingButton(
                    /**
                     * The spacing between payment method rows
                     */
                    internal val spacingDp: Float,
                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    internal val additionalInsetsDp: Float,
                ) : RowStyle() {
                    constructor(
                        context: Context,
                        @DimenRes spacingRes: Int,
                        @DimenRes additionalInsetsRes: Int
                    ) : this(
                        spacingDp = context.getRawValueFromDimenResource(spacingRes),
                        additionalInsetsDp = context.getRawValueFromDimenResource(additionalInsetsRes)
                    )

                    override fun hasSeparators() = false
                    override fun startSeparatorHasDefaultInset() = false

                    internal companion object {
                        val default = FloatingButton(
                            spacingDp = StripeThemeDefaults.floating.spacing,
                            additionalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp
                        )
                    }
                }

                @Parcelize
                @Poko
                class FlatWithChevron(
                    /**
                     * The thickness of the separator line between rows.
                     */
                    internal val separatorThicknessDp: Float,

                    /**
                     * The start inset of the separator line between rows.
                     */
                    internal val startSeparatorInsetDp: Float,

                    /**
                     * The end inset of the separator line between rows.
                     */
                    internal val endSeparatorInsetDp: Float,

                    /**
                     * Determines if the top separator is visible at the top of the Embedded Mobile Payment Element.
                     */
                    internal val topSeparatorEnabled: Boolean,

                    /**
                     * Additional vertical insets applied to a payment method row.
                     * - Note: Increasing this value increases the height of each row.
                     */
                    internal val additionalVerticalInsetsDp: Float,

                    /**
                     * Determines if the bottom separator is visible at the bottom of the Embedded Mobile Payment
                     * Element.
                     */
                    internal val bottomSeparatorEnabled: Boolean,

                    /**
                     * Horizontal insets applied to a payment method row.
                     */
                    internal val horizontalInsetsDp: Float,

                    /**
                     * Describes the colors used while the system is in light mode.
                     */
                    internal val colorsLight: Colors,

                    /**
                     * Describes the colors used while the system is in dark mode.
                     */
                    internal val colorsDark: Colors
                ) : RowStyle() {
                    constructor(
                        context: Context,
                        @DimenRes separatorThicknessRes: Int,
                        @DimenRes startSeparatorInsetRes: Int,
                        @DimenRes endSeparatorInsetRes: Int,
                        topSeparatorEnabled: Boolean,
                        bottomSeparatorEnabled: Boolean,
                        @DimenRes additionalVerticalInsetsRes: Int,
                        @DimenRes horizontalInsetsRes: Int,
                        colorsLight: Colors,
                        colorsDark: Colors
                    ) : this(
                        separatorThicknessDp = context.getRawValueFromDimenResource(separatorThicknessRes),
                        startSeparatorInsetDp = context.getRawValueFromDimenResource(startSeparatorInsetRes),
                        endSeparatorInsetDp = context.getRawValueFromDimenResource(endSeparatorInsetRes),
                        topSeparatorEnabled = topSeparatorEnabled,
                        bottomSeparatorEnabled = bottomSeparatorEnabled,
                        additionalVerticalInsetsDp = context.getRawValueFromDimenResource(additionalVerticalInsetsRes),
                        horizontalInsetsDp = context.getRawValueFromDimenResource(horizontalInsetsRes),
                        colorsLight = colorsLight,
                        colorsDark = colorsDark
                    )

                    @Parcelize
                    @Poko
                    class Colors(
                        /**
                         * The color of the separator line between rows.
                         */
                        @ColorInt
                        internal val separatorColor: Int,

                        /**
                         * The color of the chevron.
                         */
                        @ColorInt
                        internal val chevronColor: Int,
                    ) : Parcelable

                    override fun hasSeparators() = true
                    override fun startSeparatorHasDefaultInset() = false
                    internal fun getColors(isDark: Boolean): Colors = if (isDark) colorsDark else colorsLight

                    internal companion object {
                        val default = FlatWithChevron(
                            separatorThicknessDp = StripeThemeDefaults.flat.separatorThickness,
                            startSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                            endSeparatorInsetDp = StripeThemeDefaults.flat.separatorInsets,
                            topSeparatorEnabled = StripeThemeDefaults.flat.topSeparatorEnabled,
                            bottomSeparatorEnabled = StripeThemeDefaults.flat.bottomSeparatorEnabled,
                            additionalVerticalInsetsDp = StripeThemeDefaults.embeddedCommon.additionalVerticalInsetsDp,
                            horizontalInsetsDp = StripeThemeDefaults.embeddedCommon.horizontalInsetsDp,
                            colorsLight = Colors(
                                separatorColor = StripeThemeDefaults.chevronColorsLight.separatorColor.toArgb(),
                                chevronColor = StripeThemeDefaults.chevronColorsLight.chevronColor.toArgb()
                            ),
                            colorsDark = Colors(
                                separatorColor = StripeThemeDefaults.chevronColorsDark.separatorColor.toArgb(),
                                chevronColor = StripeThemeDefaults.chevronColorsDark.chevronColor.toArgb()
                            )
                        )
                    }
                }
            }
        }

        class Builder {
            private var colorsLight = Colors.defaultLight
            private var colorsDark = Colors.defaultDark
            private var shapes = Shapes.default
            private var typography = Typography.default
            private var primaryButton: PrimaryButton = PrimaryButton()
            private var formInsetValues: Insets = Insets.defaultFormInsetValues

            @OptIn(AppearanceAPIAdditionsPreview::class)
            private var sectionSpacing: Spacing = Spacing.defaultSectionSpacing

            private var textFieldInsets: Insets = Insets.defaultTextFieldInsets

            @OptIn(AppearanceAPIAdditionsPreview::class)
            private var iconStyle: IconStyle = IconStyle.default

            private var verticalModeRowPadding: Float = StripeThemeDefaults.verticalModeRowPadding

            private var embeddedAppearance: Embedded =
                Embedded.default

            fun colorsLight(colors: Colors) = apply {
                this.colorsLight = colors
            }

            fun colorsDark(colors: Colors) = apply {
                this.colorsDark = colors
            }

            fun shapes(shapes: Shapes) = apply {
                this.shapes = shapes
            }

            fun typography(typography: Typography) = apply {
                this.typography = typography
            }

            fun primaryButton(primaryButton: PrimaryButton) = apply {
                this.primaryButton = primaryButton
            }

            fun embeddedAppearance(embeddedAppearance: Embedded) = apply {
                this.embeddedAppearance = embeddedAppearance
            }

            fun formInsetValues(insets: Insets) = apply {
                this.formInsetValues = insets
            }

            @AppearanceAPIAdditionsPreview
            fun sectionSpacing(sectionSpacing: Spacing) = apply {
                this.sectionSpacing = sectionSpacing
            }

            @AppearanceAPIAdditionsPreview
            fun textFieldInsets(textFieldInsets: Insets) = apply {
                this.textFieldInsets = textFieldInsets
            }

            @AppearanceAPIAdditionsPreview
            fun iconStyle(iconStyle: IconStyle) = apply {
                this.iconStyle = iconStyle
            }

            @AppearanceAPIAdditionsPreview
            fun verticalModeRowPadding(verticalModeRowPaddingDp: Float) = apply {
                this.verticalModeRowPadding = verticalModeRowPaddingDp
            }

            @OptIn(AppearanceAPIAdditionsPreview::class)
            fun build(): Appearance {
                return Appearance(
                    colorsLight = colorsLight,
                    colorsDark = colorsDark,
                    shapes = shapes,
                    typography = typography,
                    primaryButton = primaryButton,
                    embeddedAppearance = embeddedAppearance,
                    formInsetValues = formInsetValues,
                    sectionSpacing = sectionSpacing,
                    textFieldInsets = textFieldInsets,
                    iconStyle = iconStyle,
                    verticalModeRowPadding = verticalModeRowPadding,
                )
            }
        }
    }

    @Parcelize
    data class Colors(
        /**
         * A primary color used throughout PaymentSheet.
         */
        @ColorInt
        val primary: Int,

        /**
         * The color used for the surfaces (backgrounds) of PaymentSheet.
         */
        @ColorInt
        val surface: Int,

        /**
         * The color used for the background of inputs, tabs, and other components.
         */
        @ColorInt
        val component: Int,

        /**
         * The color used for borders of inputs, tabs, and other components.
         */
        @ColorInt
        val componentBorder: Int,

        /**
         * The color of the divider lines used inside inputs, tabs, and other components.
         */
        @ColorInt
        val componentDivider: Int,

        /**
         * The default color used for text and on other elements that live on components.
         */
        @ColorInt
        val onComponent: Int,

        /**
         * The color used for items appearing over the background in Payment Sheet.
         */
        @ColorInt
        val onSurface: Int,

        /**
         * The color used for text of secondary importance.
         * For example, this color is used for the label above input fields.
         */
        @ColorInt
        val subtitle: Int,

        /**
         * The color used for input placeholder text.
         */
        @ColorInt
        val placeholderText: Int,

        /**
         * The color used for icons in PaymentSheet, such as the close or back icons.
         */
        @ColorInt
        val appBarIcon: Int,

        /**
         * A color used to indicate errors or destructive actions in PaymentSheet.
         */
        @ColorInt
        val error: Int
    ) : Parcelable {
        constructor(
            primary: Color,
            surface: Color,
            component: Color,
            componentBorder: Color,
            componentDivider: Color,
            onComponent: Color,
            subtitle: Color,
            placeholderText: Color,
            onSurface: Color,
            appBarIcon: Color,
            error: Color
        ) : this(
            primary = primary.toArgb(),
            surface = surface.toArgb(),
            component = component.toArgb(),
            componentBorder = componentBorder.toArgb(),
            componentDivider = componentDivider.toArgb(),
            onComponent = onComponent.toArgb(),
            subtitle = subtitle.toArgb(),
            placeholderText = placeholderText.toArgb(),
            onSurface = onSurface.toArgb(),
            appBarIcon = appBarIcon.toArgb(),
            error = error.toArgb()
        )

        companion object {
            internal fun configureDefaultLight(
                primary: Color = StripeThemeDefaults.colorsLight.materialColors.primary,
                surface: Color = StripeThemeDefaults.colorsLight.materialColors.surface,
            ) = Colors(
                primary = primary,
                surface = surface,
                component = StripeThemeDefaults.colorsLight.component,
                componentBorder = StripeThemeDefaults.colorsLight.componentBorder,
                componentDivider = StripeThemeDefaults.colorsLight.componentDivider,
                onComponent = StripeThemeDefaults.colorsLight.onComponent,
                subtitle = StripeThemeDefaults.colorsLight.subtitle,
                placeholderText = StripeThemeDefaults.colorsLight.placeholderText,
                onSurface = StripeThemeDefaults.colorsLight.materialColors.onSurface,
                appBarIcon = StripeThemeDefaults.colorsLight.appBarIcon,
                error = StripeThemeDefaults.colorsLight.materialColors.error
            )

            val defaultLight = configureDefaultLight()

            internal fun configureDefaultDark(
                primary: Color = StripeThemeDefaults.colorsDark.materialColors.primary,
                surface: Color = StripeThemeDefaults.colorsDark.materialColors.surface,
            ) = Colors(
                primary = primary,
                surface = surface,
                component = StripeThemeDefaults.colorsDark.component,
                componentBorder = StripeThemeDefaults.colorsDark.componentBorder,
                componentDivider = StripeThemeDefaults.colorsDark.componentDivider,
                onComponent = StripeThemeDefaults.colorsDark.onComponent,
                subtitle = StripeThemeDefaults.colorsDark.subtitle,
                placeholderText = StripeThemeDefaults.colorsDark.placeholderText,
                onSurface = StripeThemeDefaults.colorsDark.materialColors.onSurface,
                appBarIcon = StripeThemeDefaults.colorsDark.appBarIcon,
                error = StripeThemeDefaults.colorsDark.materialColors.error
            )

            val defaultDark = configureDefaultDark()
        }
    }

    @Parcelize
    data class Shapes @AppearanceAPIAdditionsPreview constructor(
        /**
         * The corner radius used for tabs, inputs, buttons, and other components in PaymentSheet.
         */
        val cornerRadiusDp: Float,

        /**
         * The border used for inputs, tabs, and other components in PaymentSheet.
         */
        val borderStrokeWidthDp: Float,

        /**
         * The corner radius used for specifically for the sheets displayed by Payment Element. Be default, this is
         * set to the same value as [cornerRadiusDp].
         */
        val bottomSheetCornerRadiusDp: Float = cornerRadiusDp,
    ) : Parcelable {
        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            /**
             * The corner radius used for tabs, inputs, buttons, and other components in PaymentSheet.
             */
            cornerRadiusDp: Float,

            /**
             * The border used for inputs, tabs, and other components in PaymentSheet.
             */
            borderStrokeWidthDp: Float,
        ) : this(
            cornerRadiusDp = cornerRadiusDp,
            borderStrokeWidthDp = borderStrokeWidthDp,
            bottomSheetCornerRadiusDp = cornerRadiusDp,
        )

        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            context: Context,
            cornerRadiusDp: Int,
            borderStrokeWidthDp: Int,
        ) : this(
            cornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusDp),
            borderStrokeWidthDp = context.getRawValueFromDimenResource(borderStrokeWidthDp),
            bottomSheetCornerRadiusDp = context.getRawValueFromDimenResource(cornerRadiusDp),
        )

        companion object {
            val default = Shapes(
                cornerRadiusDp = StripeThemeDefaults.shapes.cornerRadius,
                borderStrokeWidthDp = StripeThemeDefaults.shapes.borderStrokeWidth
            )
        }
    }

    @Parcelize
    data class Typography @AppearanceAPIAdditionsPreview constructor(
        /**
         * The scale factor for all fonts in PaymentSheet, the default value is 1.0.
         * When this value increases fonts will increase in size and decrease when this value is lowered.
         */
        val sizeScaleFactor: Float,

        /**
         * The font used in text. This should be a resource ID value.
         */
        @FontRes
        val fontResId: Int?,

        /**
         * Custom font configuration for specific text styles
         * Note: When set, these fonts override the default font calculations for their respective text styles
         */
        val custom: Custom,
    ) : Parcelable {
        @OptIn(AppearanceAPIAdditionsPreview::class)
        constructor(
            /**
             * The scale factor for all fonts in PaymentSheet, the default value is 1.0.
             * When this value increases fonts will increase in size and decrease when this value is lowered.
             */
            sizeScaleFactor: Float,
            /**
             * The font used in text. This should be a resource ID value.
             */
            @FontRes
            fontResId: Int?
        ) : this(
            sizeScaleFactor = sizeScaleFactor,
            fontResId = fontResId,
            custom = Custom(),
        )

        @AppearanceAPIAdditionsPreview
        @Parcelize
        data class Custom(
            /**
             * The font used for headlines (e.g., "Add your payment information")
             *
             * Note: If `null`, uses the calculated font based on `base` and `sizeScaleFactor`
             */
            val h1: Font? = null,
        ) : Parcelable

        @AppearanceAPIAdditionsPreview
        @Parcelize
        data class Font(
            /**
             * The font used in text. This should be a resource ID value.
             */
            @FontRes
            val fontFamily: Int? = null,
            /**
             * The font size used for the text. This should represent a sp value.
             */
            val fontSizeSp: Float? = null,
            /**
             * The font weight used for the text.
             */
            val fontWeight: Int? = null,
            /**
             * The letter spacing used for the text. This should represent a sp value.
             */
            val letterSpacingSp: Float? = null,
        ) : Parcelable

        companion object {
            val default = Typography(
                sizeScaleFactor = StripeThemeDefaults.typography.fontSizeMultiplier,
                fontResId = StripeThemeDefaults.typography.fontFamily
            )
        }
    }

    @AppearanceAPIAdditionsPreview
    @Poko
    @Parcelize
    class Spacing(internal val spacingDp: Float) : Parcelable {
        internal companion object {
            val defaultSectionSpacing = Spacing(spacingDp = -1f)
        }
    }

    /**
     * Defines the visual style of icons in Payment Element
     */
    @AppearanceAPIAdditionsPreview
    enum class IconStyle {
        /**
         * Display icons with a filled appearance
         */
        Filled,

        /**
         * Display icons with an outlined appearance
         */
        Outlined;

        internal companion object {
            val default = Filled
        }
    }

    @Parcelize
    data class PrimaryButton(
        /**
         * Describes the colors used while the system is in light mode.
         */
        val colorsLight: PrimaryButtonColors = PrimaryButtonColors.defaultLight,
        /**
         * Describes the colors used while the system is in dark mode.
         */
        val colorsDark: PrimaryButtonColors = PrimaryButtonColors.defaultDark,
        /**
         * Describes the shape of the primary button.
         */
        val shape: PrimaryButtonShape = PrimaryButtonShape(),
        /**
         * Describes the typography of the primary button.
         */
        val typography: PrimaryButtonTypography = PrimaryButtonTypography()
    ) : Parcelable

    @Parcelize
    data class PrimaryButtonColors(
        /**
         * The background color of the primary button.
         * Note: If 'null', {@link Colors#primary} is used.
         */
        @ColorInt
        val background: Int?,
        /**
         * The color of the text and icon in the primary button.
         */
        @ColorInt
        val onBackground: Int,
        /**
         * The border color of the primary button.
         */
        @ColorInt
        val border: Int,
        /**
         * The background color for the primary button when in a success state. Defaults
         * to base green background color.
         */
        @ColorInt
        val successBackgroundColor: Int = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR.toArgb(),
        /**
         * The success color for the primary button text when in a success state. Defaults
         * to `onBackground`.
         */
        @ColorInt
        val onSuccessBackgroundColor: Int = onBackground,
    ) : Parcelable {
        constructor(
            background: Int?,
            onBackground: Int,
            border: Int
        ) : this(
            background = background,
            onBackground = onBackground,
            border = border,
            successBackgroundColor = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR.toArgb(),
            onSuccessBackgroundColor = onBackground,
        )

        constructor(
            background: Color?,
            onBackground: Color,
            border: Color
        ) : this(
            background = background?.toArgb(),
            onBackground = onBackground.toArgb(),
            border = border.toArgb(),
        )

        constructor(
            background: Color?,
            onBackground: Color,
            border: Color,
            successBackgroundColor: Color = PRIMARY_BUTTON_SUCCESS_BACKGROUND_COLOR,
            onSuccessBackgroundColor: Color = onBackground,
        ) : this(
            background = background?.toArgb(),
            onBackground = onBackground.toArgb(),
            border = border.toArgb(),
            successBackgroundColor = successBackgroundColor.toArgb(),
            onSuccessBackgroundColor = onSuccessBackgroundColor.toArgb(),
        )

        companion object {
            val defaultLight = PrimaryButtonColors(
                background = null,
                onBackground = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
                border = StripeThemeDefaults.primaryButtonStyle.colorsLight.border.toArgb(),
                successBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsLight.successBackground.toArgb(),
                onSuccessBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsLight.onBackground.toArgb(),
            )
            val defaultDark = PrimaryButtonColors(
                background = null,
                onBackground = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
                border = StripeThemeDefaults.primaryButtonStyle.colorsDark.border.toArgb(),
                successBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsDark.successBackground.toArgb(),
                onSuccessBackgroundColor = StripeThemeDefaults.primaryButtonStyle.colorsDark.onBackground.toArgb(),
            )
        }
    }

    @Parcelize
    data class PrimaryButtonShape(
        /**
         * The corner radius of the primary button.
         * Note: If 'null', {@link Shapes#cornerRadiusDp} is used.
         */
        val cornerRadiusDp: Float? = null,
        /**
         * The border width of the primary button.
         * Note: If 'null', {@link Shapes#borderStrokeWidthDp} is used.
         */
        val borderStrokeWidthDp: Float? = null,
        /**
         * The height of the primary button.
         * Note: If 'null', the default height is 48dp.
         */
        val heightDp: Float? = null
    ) : Parcelable {
        constructor(
            context: Context,
            @DimenRes cornerRadiusRes: Int? = null,
            @DimenRes borderStrokeWidthRes: Int? = null,
            @DimenRes heightRes: Int? = null
        ) : this(
            cornerRadiusDp = cornerRadiusRes?.let {
                context.getRawValueFromDimenResource(it)
            },
            borderStrokeWidthDp = borderStrokeWidthRes?.let {
                context.getRawValueFromDimenResource(it)
            },
            heightDp = heightRes?.let {
                context.getRawValueFromDimenResource(it)
            }
        )
    }

    @Parcelize
    data class PrimaryButtonTypography(
        /**
         * The font used in the primary button.
         * Note: If 'null', Appearance.Typography.fontResId is used.
         */
        @FontRes
        val fontResId: Int? = null,

        /**
         * The font size in the primary button.
         * Note: If 'null', {@link Typography#sizeScaleFactor} is used.
         */
        val fontSizeSp: Float? = null
    ) : Parcelable {
        constructor(
            context: Context,
            fontResId: Int? = null,
            fontSizeSp: Int
        ) : this(
            fontResId = fontResId,
            fontSizeSp = context.getRawValueFromDimenResource(fontSizeSp)
        )
    }

    @Parcelize
    @Poko
    class Insets(
        val startDp: Float,
        val topDp: Float,
        val endDp: Float,
        val bottomDp: Float
    ) : Parcelable {
        constructor(
            context: Context,
            @DimenRes startRes: Int,
            @DimenRes topRes: Int,
            @DimenRes endRes: Int,
            @DimenRes bottomRes: Int
        ) : this(
            startDp = context.getRawValueFromDimenResource(startRes),
            topDp = context.getRawValueFromDimenResource(topRes),
            endDp = context.getRawValueFromDimenResource(endRes),
            bottomDp = context.getRawValueFromDimenResource(bottomRes)
        )

        constructor(
            horizontalDp: Float,
            verticalDp: Float
        ) : this(
            startDp = horizontalDp,
            topDp = verticalDp,
            endDp = horizontalDp,
            bottomDp = verticalDp
        )

        constructor(
            context: Context,
            @DimenRes horizontalRes: Int,
            @DimenRes verticalRes: Int
        ) : this(
            startDp = context.getRawValueFromDimenResource(horizontalRes),
            topDp = context.getRawValueFromDimenResource(verticalRes),
            endDp = context.getRawValueFromDimenResource(horizontalRes),
            bottomDp = context.getRawValueFromDimenResource(verticalRes)
        )

        companion object {
            internal val defaultFormInsetValues = Insets(
                startDp = 20f,
                topDp = 0f,
                endDp = 20f,
                bottomDp = 40f,
            )

            internal val defaultTextFieldInsets = Insets(
                startDp = StripeThemeDefaults.textFieldInsets.start,
                topDp = StripeThemeDefaults.textFieldInsets.top,
                endDp = StripeThemeDefaults.textFieldInsets.end,
                bottomDp = StripeThemeDefaults.textFieldInsets.bottom,
            )
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
     * Options to block certain card brands on the client
     */
    sealed class CardBrandAcceptance : Parcelable {

        /**
         * Card brand categories that can be allowed or disallowed
         */
        @Parcelize
        enum class BrandCategory : Parcelable {
            /**
             * Visa branded cards
             */
            Visa,

            /**
             * Mastercard branded cards
             */
            Mastercard,

            /**
             * Amex branded cards
             */
            Amex,

            /**
             * Discover branded cards
             * **Note**: Encompasses all of Discover Global Network (Discover, Diners, JCB, UnionPay, Elo).
             */
            Discover
        }

        companion object {
            /**
             * Accept all card brands supported by Stripe
             */
            @JvmStatic
            fun all(): CardBrandAcceptance = All

            /**
             * Accept only the card brands specified in `brands`.
             * **Note**: Any card brands that do not map to a `BrandCategory` will be blocked when using an allow list.
             */
            @JvmStatic
            fun allowed(brands: List<BrandCategory>): CardBrandAcceptance =
                Allowed(brands)

            /**
             * Accept all card brands supported by Stripe except for those specified in `brands`.
             * **Note**: Any card brands that do not map to a `BrandCategory` will be accepted
             * when using a disallow list.
             */
            @JvmStatic
            fun disallowed(brands: List<BrandCategory>): CardBrandAcceptance =
                Disallowed(brands)
        }

        @Parcelize
        internal data object All : CardBrandAcceptance()

        @Parcelize
        internal data class Allowed(
            val brands: List<BrandCategory>
        ) : CardBrandAcceptance()

        @Parcelize
        internal data class Disallowed(
            val brands: List<BrandCategory>
        ) : CardBrandAcceptance()
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

    @Parcelize
    data class CustomerConfiguration internal constructor(
        /**
         * The identifier of the Stripe Customer object.
         * See [Stripe's documentation](https://stripe.com/docs/api/customers/object#customer_object-id).
         */
        val id: String,

        /**
         * A short-lived token that allows the SDK to access a Customer's payment methods.
         */
        val ephemeralKeySecret: String,

        internal val accessType: CustomerAccessType,
    ) : Parcelable {
        constructor(
            id: String,
            ephemeralKeySecret: String,
        ) : this(
            id = id,
            ephemeralKeySecret = ephemeralKeySecret,
            accessType = CustomerAccessType.LegacyCustomerEphemeralKey(ephemeralKeySecret)
        )

        companion object {
            @ExperimentalCustomerSessionApi
            fun createWithCustomerSession(
                id: String,
                clientSecret: String
            ): CustomerConfiguration {
                return CustomerConfiguration(
                    id = id,
                    ephemeralKeySecret = "",
                    accessType = CustomerAccessType.CustomerSession(clientSecret)
                )
            }
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
        internal val collectMissingBillingDetailsForExistingPaymentMethods: Boolean
    ) : Parcelable {

        @JvmOverloads
        constructor(
            display: Display = Display.Automatic
        ) : this(display, true)

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
                collectMissingBillingDetailsForExistingPaymentMethods
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
