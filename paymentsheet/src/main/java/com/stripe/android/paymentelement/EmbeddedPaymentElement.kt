package com.stripe.android.paymentelement

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
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
import com.stripe.android.elements.payment.AnalyticEventCallback
import com.stripe.android.elements.payment.ConfirmCustomPaymentMethodCallback
import com.stripe.android.elements.payment.CreateIntentCallback
import com.stripe.android.elements.payment.CustomPaymentMethod
import com.stripe.android.elements.payment.GooglePayConfiguration
import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.elements.payment.LinkConfiguration
import com.stripe.android.elements.payment.PreparePaymentMethodHandler
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.SetupIntent
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfigurationCoordinator
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedContentHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementViewModel
import com.stripe.android.paymentelement.embedded.content.EmbeddedStateHelper
import com.stripe.android.paymentelement.embedded.content.PaymentOptionDisplayDataHolder
import com.stripe.android.paymentsheet.ExternalPaymentMethodConfirmHandler
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.utils.applicationIsTaskOwner
import com.stripe.android.uicore.image.rememberDrawablePainter
import com.stripe.android.uicore.utils.collectAsState
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@EmbeddedPaymentElementScope
class EmbeddedPaymentElement @Inject internal constructor(
    private val confirmationHelper: EmbeddedConfirmationHelper,
    private val contentHelper: EmbeddedContentHelper,
    private val selectionHolder: EmbeddedSelectionHolder,
    paymentOptionDisplayDataHolder: PaymentOptionDisplayDataHolder,
    private val configurationCoordinator: EmbeddedConfigurationCoordinator,
    stateHelper: EmbeddedStateHelper,
) {

    /**
     * Contains information about the customer's selected payment option.
     * Use this to display the payment option in your own UI.
     */
    val paymentOption: StateFlow<PaymentOptionDisplayData?> = paymentOptionDisplayDataHolder.paymentOption

    /**
     * The state of an already configured [EmbeddedPaymentElement].
     *
     * Use this to instantly configure an [EmbeddedPaymentElement], likely from the state of another Activity.
     */
    var state: State? by stateHelper::state

    /**
     * Call this method to configure [EmbeddedPaymentElement] or when the IntentConfiguration values you used to
     *  configure [EmbeddedPaymentElement] (amount, currency, etc.) change.
     *
     * This ensures the appropriate payment methods are displayed, collect the right fields, etc.
     * - Note: Upon completion, [paymentOption] may become null if it's no longer available.
     */
    suspend fun configure(
        intentConfiguration: IntentConfiguration,
        configuration: Configuration,
    ): ConfigureResult {
        return configurationCoordinator.configure(intentConfiguration, configuration)
    }

    /**
     * A composable function that displays a vertical list of wallet payment methods that can be used for express
     * checkout.
     */
    @WalletButtonsPreview
    @Composable
    fun WalletButtons() {
        val walletButtonsContent by contentHelper.walletButtonsContent.collectAsState()
        walletButtonsContent?.Content()
    }

    /**
     * A composable function that displays payment methods.
     *
     * It can present a sheet to collect more details or display saved payment methods.
     */
    @Composable
    fun Content() {
        val embeddedContent by contentHelper.embeddedContent.collectAsState()
        embeddedContent?.Content()
    }

    /**
     * Asynchronously confirms the currently selected payment option.
     *
     * Results will be delivered to the [ResultCallback] supplied during initialization of [EmbeddedPaymentElement].
     */
    fun confirm() {
        confirmationHelper.confirm()
    }

    /**
     * Sets the current [paymentOption] to `null`.
     */
    fun clearPaymentOption() {
        selectionHolder.set(null)
    }

    /**
     * Builder used in the creation of the [EmbeddedPaymentElement].
     *
     * Creation can be completed with [rememberEmbeddedPaymentElement].
     */
    class Builder internal constructor(
        internal val deferredHandler: DeferredHandler,
        internal val resultCallback: ResultCallback,
    ) {
        constructor(
            /**
             * Called when the customer confirms the payment or setup.
             */
            createIntentCallback: CreateIntentCallback,
            /**
             * Called with the result of the payment.
             */
            resultCallback: ResultCallback,
        ) : this(
            deferredHandler = DeferredHandler.Intent(createIntentCallback),
            resultCallback = resultCallback,
        )

        @SharedPaymentTokenSessionPreview
        constructor(
            /**
             * Called when a user calls confirm and their payment method
             * is being handed off to an external provider to handle payment/setup.
             */
            preparePaymentMethodHandler: PreparePaymentMethodHandler,
            /**
             * Called with the result of the payment.
             */
            resultCallback: ResultCallback,
        ) : this(
            deferredHandler = DeferredHandler.SharedPaymentToken(preparePaymentMethodHandler),
            resultCallback = resultCallback,
        )

        internal var externalPaymentMethodConfirmHandler: ExternalPaymentMethodConfirmHandler? = null
            private set

        @OptIn(ExperimentalCustomPaymentMethodsApi::class)
        internal var confirmCustomPaymentMethodCallback: ConfirmCustomPaymentMethodCallback? = null
            private set

        @OptIn(ExperimentalAnalyticEventCallbackApi::class)
        internal var analyticEventCallback: AnalyticEventCallback? = null
            private set

        internal var rowSelectionBehavior: RowSelectionBehavior = RowSelectionBehavior.default()

        /**
         * Called when a user confirms payment for an external payment method.
         */
        fun externalPaymentMethodConfirmHandler(handler: ExternalPaymentMethodConfirmHandler) = apply {
            this.externalPaymentMethodConfirmHandler = handler
        }

        /**
         * Called when a user confirms payment for a custom payment method.
         */
        @ExperimentalCustomPaymentMethodsApi
        fun confirmCustomPaymentMethodCallback(callback: ConfirmCustomPaymentMethodCallback) = apply {
            this.confirmCustomPaymentMethodCallback = callback
        }

        /**
         * Called when an analytic event is emitted.
         */
        @ExperimentalAnalyticEventCallbackApi
        fun analyticEventCallback(callback: AnalyticEventCallback) = apply {
            this.analyticEventCallback = callback
        }

        /**
         * Set the rowSelectionBehavior.
         * Default: payment method rows are selectable.
         * ImmediateAction: payment method rows are treated as buttons. Provide the custom logic you want to see when
         * rows are clicked
         */
        fun rowSelectionBehavior(rowSelectionBehavior: RowSelectionBehavior) = apply {
            this.rowSelectionBehavior = rowSelectionBehavior
        }

        @OptIn(SharedPaymentTokenSessionPreview::class)
        internal sealed interface DeferredHandler {
            class Intent(val createIntentCallback: CreateIntentCallback) : DeferredHandler

            class SharedPaymentToken constructor(
                val preparePaymentMethodHandler: PreparePaymentMethodHandler
            ) : DeferredHandler
        }
    }

    /** Configuration for [EmbeddedPaymentElement] **/
    @Parcelize
    @Poko
    class Configuration internal constructor(
        internal val merchantDisplayName: String,
        internal val customer: CustomerConfiguration?,
        internal val googlePay: GooglePayConfiguration?,
        internal val defaultBillingDetails: BillingDetails?,
        internal val shippingDetails: AddressDetails?,
        internal val allowsDelayedPaymentMethods: Boolean,
        internal val allowsPaymentMethodsRequiringShippingAddress: Boolean,
        internal val appearance: Appearance,
        internal val primaryButtonLabel: String?,
        internal val billingDetailsCollectionConfiguration: BillingDetailsCollectionConfiguration,
        internal val preferredNetworks: List<CardBrand>,
        internal val allowsRemovalOfLastSavedPaymentMethod: Boolean,
        internal val paymentMethodOrder: List<String>,
        internal val externalPaymentMethods: List<String>,
        internal val cardBrandAcceptance: CardBrandAcceptance,
        internal val customPaymentMethods: List<CustomPaymentMethod>,
        internal val embeddedViewDisplaysMandateText: Boolean,
        internal val link: LinkConfiguration,
        internal val formSheetAction: FormSheetAction,
    ) : Parcelable {
        @Suppress("TooManyFunctions")
        class Builder(
            /**
             * Your customer-facing business name.
             */
            private val merchantDisplayName: String
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
            private var cardBrandAcceptance: CardBrandAcceptance =
                ConfigurationDefaults.cardBrandAcceptance
            private var embeddedViewDisplaysMandateText: Boolean = ConfigurationDefaults.embeddedViewDisplaysMandateText
            private var customPaymentMethods: List<CustomPaymentMethod> =
                ConfigurationDefaults.customPaymentMethods
            private var link: LinkConfiguration = ConfigurationDefaults.link
            private var formSheetAction: FormSheetAction = FormSheetAction.Continue

            /**
             * If set, the customer can select a previously saved payment method.
             */
            fun customer(customer: CustomerConfiguration?) =
                apply { this.customer = customer }

            /**
             * Configuration related to the Stripe Customer making a payment.
             *
             * If set, Google Pay will be displayed as a payment option.
             */
            fun googlePay(googlePay: GooglePayConfiguration?) =
                apply { this.googlePay = googlePay }

            /**
             * The billing information for the customer.
             *
             * If set, form fields will be pre-populate with the values provided.
             * If `billingDetailsCollectionConfiguration.attachDefaultsToPaymentMethod` is `true`,
             * these values will be attached to the payment method even if they are not collected by
             * the UI.
             */
            fun defaultBillingDetails(defaultBillingDetails: BillingDetails?) =
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
            fun appearance(appearance: Appearance) =
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
            fun cardBrandAcceptance(
                cardBrandAcceptance: CardBrandAcceptance
            ) = apply {
                this.cardBrandAcceptance = cardBrandAcceptance
            }

            /**
             * Configuration related to custom payment methods.
             *
             * If set, Embedded Payment Element will display the defined list of custom payment methods in the UI.
             */
            @ExperimentalCustomPaymentMethodsApi
            fun customPaymentMethods(
                customPaymentMethods: List<CustomPaymentMethod>,
            ) = apply {
                this.customPaymentMethods = customPaymentMethods
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

            /**
             * Configuration related to Link.
             */
            fun link(link: LinkConfiguration): Builder = apply {
                this.link = link
            }

            /**
             * The view can display payment methods like "Card" that, when tapped, open a sheet where customers enter
             * their payment method details. The sheet has a button at the bottom. [formSheetAction] controls the action
             * the button performs.
             */
            fun formSheetAction(formSheetAction: FormSheetAction): Builder = apply {
                this.formSheetAction = formSheetAction
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
                customPaymentMethods = customPaymentMethods,
                embeddedViewDisplaysMandateText = embeddedViewDisplaysMandateText,
                link = link,
                formSheetAction = formSheetAction,
            )
        }
    }

    /**
     * The view can display payment methods like "Card" that, when tapped, open a form sheet where customers enter their
     * payment method details. The sheet has a button at the bottom. [FormSheetAction] enumerates the actions the button
     * can perform.
     */
    enum class FormSheetAction {

        /**
         * The button says "Continue". When tapped, the form sheet closes. The customer can confirm payment or setup
         * back in your app.
         */
        Continue,

        /**
         * The button says "Pay" or "Setup". When tapped, we confirm the payment or setup in the form sheet.
         */
        Confirm
    }

    /**
     * The result of a [configure] call.
     */
    sealed interface ConfigureResult {
        /**
         * The configure succeeded.
         */
        class Succeeded internal constructor() : ConfigureResult

        /**
         * The configure call failed e.g. due to network failure or because of an invalid IntentConfiguration.
         *
         * Your integration should retry with exponential backoff.
         */
        @Poko
        class Failed internal constructor(val error: Throwable) : ConfigureResult
    }

    @Poko
    class PaymentOptionDisplayData internal constructor(
        private val imageLoader: suspend () -> Drawable,

        /**
         * A user facing string representing the payment method; e.g. "Google Pay" or "···· 4242" for a card.
         */
        val label: String,

        /**
         * The billing details associated with the customer's desired payment method.
         */
        val billingDetails: BillingDetails?,

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
         * If you set [Configuration.Builder.embeddedViewDisplaysMandateText] to `false`, this text must be displayed to
         * the customer near your "Buy" button to comply with regulations.
         */
        val mandateText: AnnotatedString?,
        private val _shippingDetails: AddressDetails?,
    ) {
        private val iconDrawable: Drawable by lazy {
            DelegateDrawable(imageLoader)
        }

        /**
         * A shipping address that the user provided during checkout.
         */
        @ShippingDetailsInPaymentOptionPreview
        val shippingDetails: AddressDetails?
            get() = _shippingDetails

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
        class Completed internal constructor() : Result

        /**
         * The customer canceled the payment or setup attempt.
         */
        class Canceled internal constructor() : Result

        /**
         * The payment or setup attempt failed.
         *
         * @param error The error encountered by the customer.
         */
        @Poko
        class Failed internal constructor(val error: Throwable) : Result
    }

    /**
     * Callback that is invoked when a [Result] is available.
     */
    fun interface ResultCallback {
        fun onResult(result: Result)
    }

    /**
     * Describes how you handle row selections in EmbeddedPaymentElement
     */
    abstract class RowSelectionBehavior internal constructor() {
        private object Default : RowSelectionBehavior()

        private class ImmediateAction(
            val didSelectPaymentOption: (EmbeddedPaymentElement) -> Unit
        ) : RowSelectionBehavior()

        companion object {
            /**
             * When a payment option is selected, the customer taps a button to continue or confirm payment.
             * This is the default recommended integration.
             */
            fun default(): RowSelectionBehavior {
                return Default
            }

            /**
             * When a payment option is selected, [didSelectPaymentOption] is triggered.
             * You can implement this method to immediately perform an action e.g. go back to the checkout screen
             * or confirm the payment.
             *
             * Note that certain payment options like Google Pay and saved payment methods are disabled in this mode if
             * you set [EmbeddedPaymentElement.Configuration.formSheetAction] to [FormSheetAction.Confirm].
             */
            fun immediateAction(didSelectPaymentOption: (EmbeddedPaymentElement) -> Unit): RowSelectionBehavior {
                return ImmediateAction(didSelectPaymentOption)
            }

            internal fun getInternalRowSelectionCallback(
                rowSelectionBehavior: RowSelectionBehavior,
                embeddedPaymentElement: EmbeddedPaymentElement
            ): (() -> Unit)? {
                return if (rowSelectionBehavior is ImmediateAction) {
                    { rowSelectionBehavior.didSelectPaymentOption(embeddedPaymentElement) }
                } else {
                    null
                }
            }
        }
    }

    /**
     * A [Parcelable] state used to reconfigure [EmbeddedPaymentElement] across activity boundaries.
     */
    @Poko
    @Parcelize
    class State internal constructor(
        internal val confirmationState: EmbeddedConfirmationStateHolder.State,
        internal val customer: CustomerState?,
        internal val previousNewSelections: Bundle,
    ) : Parcelable

    internal companion object {
        fun create(
            activity: Activity,
            activityResultCaller: ActivityResultCaller,
            viewModelStoreOwner: ViewModelStoreOwner,
            lifecycleOwner: LifecycleOwner,
            paymentElementCallbackIdentifier: String,
            resultCallback: ResultCallback,
        ): EmbeddedPaymentElement {
            val viewModel = ViewModelProvider(
                owner = viewModelStoreOwner,
                factory = EmbeddedPaymentElementViewModel.Factory(
                    paymentElementCallbackIdentifier,
                    activity.window?.statusBarColor,
                )
            ).get(
                key = "EmbeddedPaymentElementViewModel(instance = $paymentElementCallbackIdentifier)",
                modelClass = EmbeddedPaymentElementViewModel::class.java,
            )

            val embeddedPaymentElementSubcomponent = viewModel.embeddedPaymentElementSubcomponentFactory.build(
                activityResultCaller = activityResultCaller,
                lifecycleOwner = lifecycleOwner,
                resultCallback = resultCallback,
            )

            embeddedPaymentElementSubcomponent.initializer.initialize(activity.applicationIsTaskOwner())

            return embeddedPaymentElementSubcomponent.embeddedPaymentElement
        }
    }
}
