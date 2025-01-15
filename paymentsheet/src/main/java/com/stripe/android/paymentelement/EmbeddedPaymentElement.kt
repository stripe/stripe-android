package com.stripe.android.paymentelement

import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.ExperimentalCardBrandFilteringApi
import com.stripe.android.common.configuration.ConfigurationDefaults
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.embedded.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.SharedPaymentElementViewModel
import com.stripe.android.paymentsheet.CreateIntentCallback
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.ExternalPaymentMethodInterceptor
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.uicore.image.rememberDrawablePainter
import com.stripe.android.uicore.utils.collectAsState
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalEmbeddedPaymentElementApi
class EmbeddedPaymentElement private constructor(
    private val embeddedConfirmationHelper: EmbeddedConfirmationHelper,
    private val sharedViewModel: SharedPaymentElementViewModel,
    private val activityResultCaller: ActivityResultCaller,
    private val lifecycleOwner: LifecycleOwner
) {

    init {
        sharedViewModel.initEmbeddedActivityLauncher(activityResultCaller, lifecycleOwner)
    }

    /**
     * Contains information about the customer's selected payment option.
     * Use this to display the payment option in your own UI.
     */
    val paymentOption: StateFlow<PaymentOptionDisplayData?> = sharedViewModel.paymentOption

    /**
     * Call this method to initialize [EmbeddedPaymentElement] or when the IntentConfiguration values you used to
     *  initialize [EmbeddedPaymentElement] (amount, currency, etc.) change.
     *
     * This ensures the appropriate payment methods are displayed, collect the right fields, etc.
     * - Note: Upon completion, [paymentOption] may become null if it's no longer available.
     */
    suspend fun configure(
        intentConfiguration: PaymentSheet.IntentConfiguration,
        configuration: Configuration,
    ): ConfigureResult {
        return sharedViewModel.configure(intentConfiguration, configuration)
    }

    /**
     * A composable function that displays payment methods.
     *
     * It can present a sheet to collect more details or display saved payment methods.
     */
    @Composable
    fun Content() {
        val embeddedContent by sharedViewModel.embeddedContent.collectAsState()
        embeddedContent?.Content()
    }

    /**
     * Asynchronously confirms the currently selected payment options.
     *
     * Results will be delivered to the [ResultCallback] supplied during initialization of [EmbeddedPaymentElement].
     */
    fun confirm() {
        embeddedConfirmationHelper.confirm()
    }

    /**
     * Sets the current [paymentOption] to `null`.
     */
    fun clearPaymentOption() {
        sharedViewModel.clearPaymentOption()
    }

    /**
     * Builder used in the creation of the [EmbeddedPaymentElement].
     *
     * Creation can be completed with [rememberEmbeddedPaymentElement].
     */
    @ExperimentalEmbeddedPaymentElementApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder(
        /**
         * Called when the customer confirms the payment or setup.
         */
        internal val createIntentCallback: CreateIntentCallback,
        /**
         * Called with the result of the payment.
         */
        internal val resultCallback: ResultCallback,
    ) {
        internal var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null
            private set

        /**
         * Called when a user confirms payment for an external payment method.
         */
        fun externalPaymentMethodConfirmHandler(handler: ExternalPaymentMethodConfirmHandler) = apply {
            this.externalPaymentMethodConfirmHandler = handler
        }
    }

    /** Configuration for [EmbeddedPaymentElement] **/
    @Parcelize
    @Poko
    @ExperimentalEmbeddedPaymentElementApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration internal constructor(
        internal val merchantDisplayName: String,
        internal val customer: PaymentSheet.CustomerConfiguration?,
        internal val googlePay: PaymentSheet.GooglePayConfiguration?,
        internal val defaultBillingDetails: PaymentSheet.BillingDetails?,
        internal val shippingDetails: AddressDetails?,
        internal val allowsDelayedPaymentMethods: Boolean,
        internal val allowsPaymentMethodsRequiringShippingAddress: Boolean,
        internal val appearance: PaymentSheet.Appearance,
        internal val primaryButtonLabel: String?,
        internal val billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration,
        internal val preferredNetworks: List<CardBrand>,
        internal val allowsRemovalOfLastSavedPaymentMethod: Boolean,
        internal val paymentMethodOrder: List<String>,
        internal val externalPaymentMethods: List<String>,
        internal val cardBrandAcceptance: PaymentSheet.CardBrandAcceptance,
        internal val embeddedViewDisplaysMandateText: Boolean,
    ) : Parcelable {
        @Suppress("TooManyFunctions")
        @ExperimentalEmbeddedPaymentElementApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Builder(
            /**
             * Your customer-facing business name.
             */
            private val merchantDisplayName: String
        ) {
            private var customer: PaymentSheet.CustomerConfiguration? = ConfigurationDefaults.customer
            private var googlePay: PaymentSheet.GooglePayConfiguration? = ConfigurationDefaults.googlePay
            private var defaultBillingDetails: PaymentSheet.BillingDetails? = ConfigurationDefaults.billingDetails
            private var shippingDetails: AddressDetails? = ConfigurationDefaults.shippingDetails
            private var allowsDelayedPaymentMethods: Boolean = ConfigurationDefaults.allowsDelayedPaymentMethods
            private var allowsPaymentMethodsRequiringShippingAddress: Boolean =
                ConfigurationDefaults.allowsPaymentMethodsRequiringShippingAddress
            private var appearance: PaymentSheet.Appearance = ConfigurationDefaults.appearance
            private var primaryButtonLabel: String? = ConfigurationDefaults.primaryButtonLabel
            private var billingDetailsCollectionConfiguration =
                ConfigurationDefaults.billingDetailsCollectionConfiguration
            private var preferredNetworks: List<CardBrand> = ConfigurationDefaults.preferredNetworks
            private var allowsRemovalOfLastSavedPaymentMethod: Boolean =
                ConfigurationDefaults.allowsRemovalOfLastSavedPaymentMethod
            private var paymentMethodOrder: List<String> = ConfigurationDefaults.paymentMethodOrder
            private var externalPaymentMethods: List<String> = ConfigurationDefaults.externalPaymentMethods
            private var cardBrandAcceptance: PaymentSheet.CardBrandAcceptance =
                ConfigurationDefaults.cardBrandAcceptance
            private var embeddedViewDisplaysMandateText: Boolean = ConfigurationDefaults.embeddedViewDisplaysMandateText

            /**
             * If set, the customer can select a previously saved payment method.
             */
            fun customer(customer: PaymentSheet.CustomerConfiguration?) =
                apply { this.customer = customer }

            /**
             * Configuration related to the Stripe Customer making a payment.
             *
             * If set, Google Pay will be displayed as a payment option.
             */
            fun googlePay(googlePay: PaymentSheet.GooglePayConfiguration?) =
                apply { this.googlePay = googlePay }

            /**
             * The billing information for the customer.
             *
             * If set, form fields will be pre-populate with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the UI.
             */
            fun defaultBillingDetails(defaultBillingDetails: PaymentSheet.BillingDetails?) =
                apply { this.defaultBillingDetails = defaultBillingDetails }

            /**
             * The shipping information for the customer.
             * If set, form fields will be pre populated with the values provided.
             * This is used to display a "Billing address is same as shipping" checkbox if `defaultBillingDetails` is
             *  not provided.
             * If `name` and `line1` are populated, it's also
             *  [attached to the PaymentIntent](https://stripe.com/docs/api/payment_intents/object#payment_intent_object-shipping)
             *  during payment.
             */
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
             * **Note**: The payment element considers this property `true` if `shipping` details are present
             * on the PaymentIntent/SetupIntent when loaded.
             */
            fun allowsPaymentMethodsRequiringShippingAddress(
                allowsPaymentMethodsRequiringShippingAddress: Boolean,
            ) = apply {
                this.allowsPaymentMethodsRequiringShippingAddress =
                    allowsPaymentMethodsRequiringShippingAddress
            }

            /**
             * Describes the appearance of the embedded payment element.
             */
            fun appearance(appearance: PaymentSheet.Appearance) =
                apply { this.appearance = appearance }

            /**
             * The label to use for the primary button.
             *
             * If not set, Stripe will display suitable default labels for payment and setup intents.
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
                billingDetailsCollectionConfiguration: PaymentSheet.BillingDetailsCollectionConfiguration
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

            @ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
            fun allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod: Boolean) = apply {
                this.allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod
            }

            /**
             * By default, Stripe will use a dynamic ordering that optimizes payment method display for the
             * customer. You can override the default order in which payment methods are displayed with
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
             * External payment methods to display.
             *
             * If you specify any external payment methods here, you must also pass an
             * [ExternalPaymentMethodConfirmHandler] when creating the embedded payment element.
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
             * By default, the payment element will accept all supported cards by Stripe.
             * You can specify card brands that should block or allow
             * payments for by providing a list of those card brands.
             * **Note**: This is only a client-side solution.
             * **Note**: Card brand filtering is not currently supported in Link.
             */
            @ExperimentalCardBrandFilteringApi
            fun cardBrandAcceptance(
                cardBrandAcceptance: PaymentSheet.CardBrandAcceptance
            ) = apply {
                this.cardBrandAcceptance = cardBrandAcceptance
            }

            /**
             * Controls whether the view displays mandate text at the bottom for payment methods that require it.
             *
             * If set to `false`, your integration must display `PaymentOptionDisplayData.mandateText` to the customer
             *  near your “Buy” button to comply with regulations.
             *
             * - Note: This doesn't affect mandates displayed in the form sheet.
             */
            fun embeddedViewDisplaysMandateText(
                embeddedViewDisplaysMandateText: Boolean
            ) = apply {
                this.embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText
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
                cardBrandAcceptance = cardBrandAcceptance,
                embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
            )
        }
    }

    /**
     * The result of an [configure] call.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalEmbeddedPaymentElementApi
    sealed interface ConfigureResult {
        /**
         * The configure succeeded.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Succeeded internal constructor() : ConfigureResult

        /**
         * The configure call failed e.g. due to network failure or because of an invalid IntentConfiguration.
         *
         * Your integration should retry with exponential backoff.
         */
        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Failed internal constructor(val error: Throwable) : ConfigureResult
    }

    @Poko
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalEmbeddedPaymentElementApi
    class PaymentOptionDisplayData internal constructor(
        private val imageLoader: suspend () -> Drawable,

        /**
         * A user facing string representing the payment method; e.g. "Google Pay" or "···· 4242" for a card.
         */
        val label: String,

        /**
         * The billing details associated with the customer's desired payment method.
         */
        val billingDetails: PaymentSheet.BillingDetails?,

        /**
         * A string representation of the customer's desired payment method:
         * - If this is a Stripe payment method, see
         *      https://stripe.com/docs/api/payment_methods/object#payment_method_object-type for possible values.
         * - If this is an external payment method, see
         *      https://stripe.com/docs/payments/external-payment-methods?platform=ios#available-external-payment-methods
         *      for possible values.
         * - If this is Google Pay, the value is "google_pay".
         */
        val paymentMethodType: String,

        /**
         * If you set [Configuration.Builder.embeddedViewDisplaysMandateText] to `false`, this text must be displayed to
         * the customer near your "Buy" button to comply with regulations.
         */
        val mandateText: AnnotatedString?,
    ) {
        private val iconDrawable: Drawable by lazy {
            DelegateDrawable(imageLoader)
        }

        /**
         * An image representing a payment method; e.g. the Google Pay logo or a VISA logo.
         */
        val iconPainter: Painter
            @Composable
            get() = rememberDrawablePainter(iconDrawable)
    }

    /**
     * The result of an attempt to confirm a [PaymentIntent] or [SetupIntent].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalEmbeddedPaymentElementApi
    sealed interface Result {
        /**
         * The customer completed the payment or setup.
         * The payment may still be processing at this point; don't assume money has successfully moved.
         *
         * Your app should transition to a generic receipt view (e.g. a screen that displays "Your order
         * is confirmed!"), and fulfill the order (e.g. ship the product to the customer) after
         * receiving a successful payment event from Stripe.
         *
         * See [Stripe's documentation](https://stripe.com/docs/payments/handling-payment-events).
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Completed internal constructor() : Result

        /**
         * The customer canceled the payment or setup attempt.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Canceled internal constructor() : Result

        /**
         * The payment or setup attempt failed.
         *
         * @param error The error encountered by the customer.
         */
        @Poko
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @ExperimentalEmbeddedPaymentElementApi
        class Failed internal constructor(val error: Throwable) : Result
    }

    /**
     * Callback that is invoked when a [Result] is available.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @ExperimentalEmbeddedPaymentElementApi
    fun interface ResultCallback {
        fun onResult(result: Result)
    }

    internal companion object {
        @ExperimentalEmbeddedPaymentElementApi
        fun create(
            statusBarColor: Int?,
            activityResultCaller: ActivityResultCaller,
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            resultCallback: ResultCallback,
        ): EmbeddedPaymentElement {
            val sharedViewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = SharedPaymentElementViewModel.Factory(statusBarColor)
            )[SharedPaymentElementViewModel::class.java]
            lifecycleOwner.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        IntentConfirmationInterceptor.createIntentCallback = null
                        ExternalPaymentMethodInterceptor.externalPaymentMethodConfirmHandler = null
                    }
                }
            )
            return EmbeddedPaymentElement(
                embeddedConfirmationHelper = EmbeddedConfirmationHelper(
                    confirmationHandler = sharedViewModel.confirmationHandler,
                    resultCallback = resultCallback,
                    activityResultCaller = activityResultCaller,
                    lifecycleOwner = lifecycleOwner,
                    confirmationStateSupplier = { sharedViewModel.confirmationStateHolder.state },
                ),
                sharedViewModel = sharedViewModel,
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner
            )
        }
    }
}
