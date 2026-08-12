package com.stripe.android.checkout

import android.app.Application
import android.graphics.drawable.Drawable
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.ece.ExpressButtonType
import com.stripe.android.checkout.injection.CheckoutPresenterSubcomponent
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.common.ui.DelegateDrawable
import com.stripe.android.common.ui.PaymentElementActivityResultCaller
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.core.utils.StatusBarCompat
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.PaymentElement
import com.stripe.android.elements.ShippingAddressElement
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorOptions
import com.stripe.android.uicore.image.rememberDrawablePainter
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val SERVER_UPDATE_TIMEOUT_MS = 20.seconds.inWholeMilliseconds

/**
 * Controls a Stripe Checkout Session: load it with [configure], observe it through
 * [session], mutate it with the various `update`/`apply`/`select` methods, and present
 * payment UI with a [CheckoutPresenter] created via [createPresenter].
 *
 * Create instances with [Builder].
 */
@Singleton
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class CheckoutController @Inject internal constructor(
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val checkoutStateLoader: CheckoutStateLoader,
    private val stateHolder: CheckoutControllerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val checkoutPresenterSubcomponentFactory: CheckoutPresenterSubcomponent.Factory,
    @PaymentElementCallbackIdentifier internal val paymentElementCallbackIdentifier: String,
    private val savedState: CheckoutControllerSavedState,
) {
    /**
     * The latest [Session] data, or `null` until [configure] has completed successfully.
     */
    val session: StateFlow<Session?>
        get() = stateHolder.session

    /**
     * Whether a mutation or confirmation is currently in progress or queued.
     */
    val isUpdating: StateFlow<Boolean> = operationCoordinator.isUpdating

    init {
        viewModelScope.launch {
            operationCoordinator.observeConfirmationResults(::commitConfirmedSession)
        }
    }

    /**
     * Loads the Checkout Session identified by [checkoutSessionClientSecret] and prepares the
     * payment UI, populating [session] on success.
     *
     * @param checkoutSessionClientSecret The client secret of the Checkout Session to load.
     * @param configuration Options controlling how the checkout is configured and displayed.
     */
    suspend fun configure(
        checkoutSessionClientSecret: String,
        configuration: Configuration = Configuration(),
    ): kotlin.Result<Unit> {
        // A re-configure while a payment flow is presented would reload the session out from under
        // the open sheet. The first configure runs with null state, so this only blocks re-configures.
        if (sheetStateHolder.sheetIsOpen) {
            return integrationLaunchedFailure()
        }
        return operationCoordinator.runMutation {
            val configurationState = configuration.build()
            val sessionId = checkoutSessionClientSecret.substringBefore("_secret_")

            checkoutSessionRepository.init(
                sessionId = sessionId,
                adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
            ).mapCatching { response ->
                val defaultBillingAddress = configurationState.defaults.billingDetails?.address
                if (defaultBillingAddress != null &&
                    response.automaticTaxEnabled &&
                    response.taxAddressSource == CheckoutSessionResponse.TaxAddressSource.BILLING
                ) {
                    checkoutSessionRepository.updateTaxRegion(sessionId, defaultBillingAddress)
                        .getOrThrow()
                } else {
                    response
                }
            }.mapCatching { response ->
                val defaultEmail = configurationState.defaults.email
                if (defaultEmail != null) {
                    checkoutSessionRepository.updateEmail(sessionId, defaultEmail).getOrThrow()
                } else {
                    response
                }
            }.mapCatching { response ->
                checkoutStateLoader.loadInitial(
                    configuration = configurationState,
                    checkoutSessionResponse = response,
                )
            }
        }
    }

    /**
     * Applies a promotion code to the checkout session.
     *
     * @param promotionCode The promotion code to apply. Leading/trailing whitespace is trimmed.
     */
    suspend fun applyPromotionCode(
        promotionCode: String,
    ): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        checkoutSessionRepository.applyPromotionCode(sessionId, promotionCode.trim())
    }

    /**
     * Removes the currently applied promotion code from the checkout session.
     */
    suspend fun removePromotionCode(): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        checkoutSessionRepository.applyPromotionCode(sessionId, "")
    }

    /**
     * Sets the shipping address for this checkout.
     *
     * The address is stored locally and used when presenting payment UI. If automatic tax is
     * enabled and the tax address source is shipping, the address is also sent to the server
     * to compute updated tax amounts.
     *
     * @param name The recipient's name.
     * @param phoneNumber The recipient's phone number.
     * @param address The shipping address.
     */
    suspend fun updateShippingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> {
        stateHolder.state?.checkoutSessionResponse
            ?.validateShippingCountry(address.build().country)
            ?.onFailure { return kotlin.Result.failure(it) }
        return updateAddress(CheckoutSessionResponse.TaxAddressSource.SHIPPING, address) {
            copy(
                collectedDetails = collectedDetails.copy(
                    shippingName = name,
                    shippingPhoneNumber = phoneNumber,
                    shippingAddress = it,
                ),
            )
        }
    }

    /**
     * Updates the customer's email address.
     *
     * @param email The email address to set. Pass `null` to clear the customer's email.
     * Leading/trailing whitespace is trimmed.
     */
    suspend fun updateEmail(
        email: String?,
    ): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        checkoutSessionRepository.updateEmail(sessionId, email?.trim().orEmpty())
    }

    internal suspend fun updateBillingAddress(
        name: String?,
        phoneNumber: String?,
        address: Address,
    ): kotlin.Result<Unit> = updateAddress(CheckoutSessionResponse.TaxAddressSource.BILLING, address) {
        copy(
            collectedDetails = collectedDetails.copy(
                billingName = name,
                billingPhoneNumber = phoneNumber,
                billingAddress = it,
            ),
        )
    }

    /**
     * Runs an async function that calls your server to update the Checkout Session,
     * then automatically refreshes [session] with the latest session data.
     *
     * A 20-second timeout is enforced. If [serverUpdate] doesn't complete within 20 seconds,
     * this method returns a [kotlin.Result.failure] with a timeout exception.
     *
     * @param serverUpdate A suspend function that makes a request to your server to update
     * the Checkout Session.
     */
    suspend fun runServerUpdate(
        serverUpdate: suspend () -> kotlin.Result<Unit>,
    ): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        withTimeout(SERVER_UPDATE_TIMEOUT_MS) { serverUpdate() }.fold(
            onSuccess = {
                checkoutSessionRepository.init(
                    sessionId = sessionId,
                    adaptivePricingAllowed = configuration.adaptivePricingAllowed,
                )
            },
            onFailure = { kotlin.Result.failure(it) },
        )
    }

    private suspend fun updateAddress(
        addressType: CheckoutSessionResponse.TaxAddressSource,
        address: Address,
        mutation: CheckoutControllerState.(Address.State) -> CheckoutControllerState,
    ): kotlin.Result<Unit> {
        val built = address.build()
        return withCheckoutState(
            additionalStateMutations = { mutation(built) },
        ) { sessionId ->
            val shouldSendTaxRegion = checkoutSessionResponse.automaticTaxEnabled &&
                checkoutSessionResponse.taxAddressSource == addressType
            if (shouldSendTaxRegion) {
                checkoutSessionRepository.updateTaxRegion(sessionId, built)
            } else {
                kotlin.Result.success(checkoutSessionResponse)
            }
        }
    }

    /**
     * Runs a mutation against the checkout session, serializing it behind the operation coordinator
     * so mutations run in sequence. [block] produces the updated [CheckoutSessionResponse]; the
     * result is folded into a new [CheckoutControllerState] (with any [additionalStateMutations]
     * applied) and handed to [checkoutStateLoader] to reload the payment element and atomically
     * commit the new state.
     *
     * Returns [kotlin.Result.failure] if the session hasn't been configured yet or a payment flow is
     * currently presented.
     */
    private suspend fun withCheckoutState(
        additionalStateMutations: CheckoutControllerState.() -> CheckoutControllerState = { this },
        block: suspend CheckoutControllerState.(sessionId: String) -> kotlin.Result<CheckoutSessionResponse>,
    ): kotlin.Result<Unit> {
        stateHolder.state
            ?: return kotlin.Result.failure(
                IllegalStateException("Cannot mutate checkout session before it is configured.")
            )
        if (sheetStateHolder.sheetIsOpen) {
            return kotlin.Result.failure(
                IllegalStateException("Cannot mutate checkout session while a payment flow is presented.")
            )
        }
        return operationCoordinator.runMutation {
            runCatching {
                // Re-read the latest committed state inside the lock so serialized mutations
                // build on each other's results rather than a stale snapshot.
                val state = requireNotNull(stateHolder.state)
                val response = state.block(state.checkoutSessionResponse.id).getOrThrow()
                val newState = state
                    .copy(checkoutSessionResponse = response)
                    .additionalStateMutations()
                // reload resolves flag images (reusing newState's carried-over cache) and commits
                // the fully reloaded state to the holder.
                checkoutStateLoader.reload(newState)
            }
        }
    }

    /**
     * Commits the [CheckoutSessionResponse] a successful confirmation returned, mirroring what
     * [withCheckoutState] does with a mutation's response. Runs inside the operation gate, so the
     * state read here is the latest committed one.
     */
    internal suspend fun commitConfirmedSession(response: CheckoutSessionResponse) {
        val state = stateHolder.state ?: return
        checkoutStateLoader.reload(state.copy(checkoutSessionResponse = response))
    }

    private fun requireMutableState(): kotlin.Result<Unit> {
        stateHolder.state
            ?: return kotlin.Result.failure(
                IllegalStateException("Cannot mutate checkout session before it is configured.")
            )
        return if (sheetStateHolder.sheetIsOpen) {
            integrationLaunchedFailure()
        } else {
            kotlin.Result.success(Unit)
        }
    }

    private fun integrationLaunchedFailure(): kotlin.Result<Nothing> = kotlin.Result.failure(
        IllegalStateException("Cannot mutate checkout session while a payment flow is presented.")
    )

    internal suspend fun updateCurrency(currency: String): kotlin.Result<Unit> {
        return withCheckoutState { sessionId ->
            checkoutSessionRepository.updateCurrency(sessionId, currency)
        }
    }

    /**
     * Creates a [CheckoutPresenter] bound to [activity], used to present the payment UI for this
     * checkout session.
     *
     * @param activity The activity the payment UI will be presented from.
     */
    fun createPresenter(activity: ComponentActivity): CheckoutPresenter {
        val subcomponent = checkoutPresenterSubcomponentFactory.create(
            activityResultCaller = PaymentElementActivityResultCaller(
                key = "CheckoutController(instance = $paymentElementCallbackIdentifier)",
                registryOwner = activity,
            ),
            lifecycleOwner = activity,
            statusBarColor = StatusBarCompat.color(activity),
        )
        subcomponent.initializer.initialize()
        return subcomponent.presenter
    }

    /**
     * Releases resources held by this controller and clears its loaded state. Call this when the
     * controller is no longer needed.
     */
    fun destroy() {
        viewModelScope.cancel()
        checkoutStateLoader.clear()
        savedState.clear()
    }

    /**
     * Clears the customer's selected payment option, resetting it to `null`.
     *
     * Returns [kotlin.Result.failure] if the session hasn't been configured yet, a payment flow is
     * currently presented, or another mutation or confirmation is in progress.
     */
    fun clearPaymentOption(): kotlin.Result<Unit> {
        return requireMutableState().fold(
            onSuccess = {
                operationCoordinator.runSynchronousMutation {
                    stateHolder.clearSelection()
                    kotlin.Result.success(Unit)
                }
            },
            onFailure = { kotlin.Result.failure(it) },
        )
    }

    /**
     * A [Session] tracks the process of collecting a payment from your customer.
     *
     * - [Checkout Sessions Overview](https://docs.stripe.com/payments/checkout)
     * - [Checkout Sessions API Reference](https://docs.stripe.com/api/checkout/sessions)
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Session internal constructor(
        /**
         * The checkout session ID (e.g., "cs_xxx").
         */
        val id: String,
        /**
         * The status of the [Session] (open, complete, or expired).
         */
        val status: Status,
        /**
         * Whether this checkout session was created in live mode.
         */
        val liveMode: Boolean,
        /**
         * The three-letter ISO currency code (e.g., "usd").
         */
        val currency: String,
        /**
         * The customer's email address from the checkout session.
         */
        val email: String?,
        /**
         * The tax computation status for this checkout session.
         */
        val tax: Tax,
        /**
         * Summary of totals including subtotal, discounts, taxes, and shipping.
         */
        val totalSummary: TotalSummary?,
        /**
         * The products or services being purchased in this checkout session.
         */
        val lineItems: List<LineItem>,
        /**
         * Available shipping options for this checkout session.
         */
        val shippingOptions: List<ShippingRate>,
        /**
         * The customer's currently selected payment option, or `null` if none has been selected yet.
         */
        val paymentOptionDisplayData: PaymentOptionDisplayData?,
        internal val currencySelectorOptions: CurrencySelectorOptions?,
        internal val availableExpressButtonTypes: List<ExpressButtonType>,
    ) {

        /**
         * Whether Express Checkout Element has any payment methods to display for this checkout session.
         */
        val isExpressCheckoutElementAvailable: Boolean = availableExpressButtonTypes.isNotEmpty()

        /**
         * Whether Currency Selector Element has content to display for this checkout session.
         */
        val isCurrencySelectorAvailable: Boolean = currencySelectorOptions != null

        /**
         * The status of a checkout session.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        enum class Status {
            /**
             * The checkout session is still in progress. Payment processing has not started.
             */
            Open,

            /**
             * The checkout session is complete. Payment processing may still be in progress.
             */
            Complete,

            /**
             * The checkout session has expired. No further processing will occur.
             */
            Expired,

            /**
             * A status not recognized by this version of the SDK.
             */
            Unknown,
        }

        /**
         * Tax computation state for a checkout session.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Tax internal constructor(
            /**
             * The current tax computation status.
             */
            val status: Status,
        ) {
            /**
             * The status of tax computation.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            enum class Status {
                /**
                 * The final tax amount is computed, and the session is ready for confirmation.
                 */
                Ready,

                /**
                 * A shipping address must be provided to calculate tax.
                 */
                RequiresShippingAddress,

                /**
                 * A billing address must be provided to calculate tax.
                 */
                RequiresBillingAddress,

                /**
                 * A tax status not recognized by this version of the SDK.
                 */
                Unknown,
            }
        }

        /**
         * Summary of all totals for the checkout session.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class TotalSummary internal constructor(
            /**
             * The subtotal before discounts, taxes, and shipping.
             */
            val subtotal: Long,
            /**
             * The amount due today, accounting for applied balances.
             */
            val totalDueToday: Long,
            /**
             * The total amount due including all charges.
             */
            val totalAmountDue: Long,
            /**
             * Discounts applied to the checkout session.
             */
            val discountAmounts: List<DiscountAmount>,
            /**
             * Tax amounts applied to the checkout session.
             */
            val taxAmounts: List<TaxAmount>,
            /**
             * The selected shipping rate, if any.
             */
            val shippingRate: ShippingRate?,
            /**
             * The customer's account balance applied to this session, if any.
             */
            val appliedBalance: Long?,
        )

        /**
         * A discount applied to the checkout session.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class DiscountAmount internal constructor(
            /**
             * The discount amount in the smallest currency unit.
             */
            val amount: Long,
            /**
             * The display name of the discount.
             */
            val displayName: String,
        )

        /**
         * A tax amount applied to the checkout session.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class TaxAmount internal constructor(
            /**
             * The tax amount in the smallest currency unit.
             */
            val amount: Long,
            /**
             * Whether this tax is inclusive (already included in the price).
             */
            val inclusive: Boolean,
            /**
             * The display name of the tax (e.g., "Sales Tax").
             */
            val displayName: String,
            /**
             * The tax rate as a percentage (e.g., 8.25).
             */
            val percentage: Double,
        )

        /**
         * A shipping rate option for the checkout session.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class ShippingRate internal constructor(
            /**
             * The shipping rate ID.
             */
            val id: String,
            /**
             * The shipping amount in the smallest currency unit.
             */
            val amount: Long,
            /**
             * The display name of the shipping option (e.g., "Standard Shipping").
             */
            val displayName: String,
            /**
             * The estimated delivery time, if available (e.g., "3-5 business days").
             */
            val deliveryEstimate: String?,
        )

        /**
         * A line item in the checkout session.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class LineItem internal constructor(
            /**
             * The line item ID.
             */
            val id: String,
            /**
             * The display name of the item.
             */
            val name: String,
            /**
             * The quantity of this item.
             */
            val quantity: Int,
            /**
             * The unit price in the smallest currency unit, if available.
             */
            val unitAmount: Long?,
            /**
             * The subtotal before discounts and taxes.
             */
            val subtotal: Long,
            /**
             * The total after discounts and taxes.
             */
            val total: Long,
        )

        /**
         * Display data for the customer's currently selected payment option.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class PaymentOptionDisplayData internal constructor(
            /**
             * Loads the payment method image. Prefer [iconPainter] to render it in Compose.
             */
            @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            val imageLoader: suspend () -> Drawable,
            /**
             * A user facing string representing the payment method; e.g. "Google Pay" or "···· 4242" for a card.
             */
            val label: String,
            /**
             * The billing details associated with the customer's selected payment method, if any were collected.
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
             * If you set [PaymentElement.Configuration.embeddedViewDisplaysMandateText] to `false`, this text
             * must be displayed to the customer near your "Buy" button to comply with regulations.
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
    }

    /**
     * The billing details collected for a payment method.
     */
    @Poko
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class BillingDetails internal constructor(
        /**
         * The customer's billing address.
         */
        val address: Address?,
        /**
         * The customer's email address.
         */
        val email: String?,
        /**
         * The customer's full name.
         */
        val name: String?,
        /**
         * The customer's phone number, without formatting (e.g. 5551234567).
         */
        val phone: String?,
    ) {
        /**
         * A billing address.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Address internal constructor(
            /**
             * City, district, suburb, town, or village.
             */
            val city: String?,
            /**
             * Two-letter country code (ISO 3166-1 alpha-2).
             */
            val country: String?,
            /**
             * Address line 1 (e.g., street, PO Box, or company name).
             */
            val line1: String?,
            /**
             * Address line 2 (e.g., apartment, suite, unit, or building).
             */
            val line2: String?,
            /**
             * ZIP or postal code.
             */
            val postalCode: String?,
            /**
             * State, county, province, or region.
             */
            val state: String?,
        )
    }

    /**
     * Builds [CheckoutController] instances.
     *
     * @param application The application context.
     * @param savedStateHandle The [SavedStateHandle] used to persist and restore controller state
     * across process death.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder(
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
    ) {
        private var resultCallback: ResultCallback = ResultCallback {}
        private var integrationName: String = "stripe_checkout"

        /**
         * Sets the [ResultCallback] invoked when a payment flow finishes.
         */
        fun resultCallback(
            resultCallback: ResultCallback
        ): Builder = apply {
            this.resultCallback = resultCallback
        }

        /**
         * Sets a unique name identifying this integration, allowing multiple [CheckoutController]
         * instances to coexist.
         *
         * The name namespaces the controller's persisted state within [savedStateHandle] and its
         * entry in global callback state, so instances built from the same [savedStateHandle] stay
         * isolated from one another. Defaults to `"stripe_checkout"`; provide a distinct name for
         * each controller when building more than one.
         */
        fun integrationName(
            integrationName: String
        ): Builder = apply {
            this.integrationName = integrationName
        }

        /**
         * Builds a [CheckoutController] from the current configuration.
         */
        fun build(): CheckoutController {
            val checkoutControllerSavedState = CheckoutControllerSavedState(
                parentHandle = savedStateHandle,
                integrationName = integrationName,
            )
            val component = DaggerCheckoutControllerComponent.factory().create(
                application = application,
                paymentElementCallbackIdentifier = integrationName,
                resultCallback = resultCallback,
                checkoutControllerSavedState = checkoutControllerSavedState,
            )

            return component.checkoutController
        }
    }

    /**
     * Configuration options for a [CheckoutController], including per-element configuration and
     * feature toggles. Build with the fluent setters and pass to [CheckoutController.configure].
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricing: AdaptivePricing = AdaptivePricing()
        private var apiConfiguration: ApiConfiguration? = null
        private var merchantDisplayName: String? = null
        private var googlePayConfiguration: GooglePayConfiguration? = null
        private var defaults: Defaults = Defaults()
        private var paymentElementConfiguration: PaymentElement.Configuration = PaymentElement.Configuration()
        private var currencySelectorElementConfiguration: CurrencySelectorElement.Configuration =
            CurrencySelectorElement.Configuration()
        private var shippingAddressElementConfiguration: ShippingAddressElement.Configuration =
            ShippingAddressElement.Configuration()
        private var expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration =
            ExpressCheckoutElement.Configuration()

        /**
         * Sets the adaptive pricing configuration for this checkout session.
         */
        fun adaptivePricing(
            adaptivePricing: AdaptivePricing,
        ): Configuration = apply {
            this.adaptivePricing = adaptivePricing
        }

        /**
         * Sets the API configuration for this checkout session.
         */
        fun apiConfiguration(
            apiConfiguration: ApiConfiguration,
        ): Configuration = apply {
            this.apiConfiguration = apiConfiguration
        }

        /**
         * Sets the merchant display name shown to the customer during checkout.
         *
         * If not set, the business name from the checkout session is used, falling back to the
         * name of your app.
         */
        fun merchantDisplayName(
            merchantDisplayName: String,
        ): Configuration = apply {
            this.merchantDisplayName = merchantDisplayName
        }

        /**
         * Sets the configuration for the payment element.
         */
        fun paymentElement(
            configuration: PaymentElement.Configuration
        ): Configuration = apply {
            this.paymentElementConfiguration = configuration
        }

        /**
         * Sets the configuration for the currency selector element.
         */
        fun currencySelectorElement(
            configuration: CurrencySelectorElement.Configuration
        ): Configuration = apply {
            this.currencySelectorElementConfiguration = configuration
        }

        /**
         * Sets the configuration for the shipping address element.
         */
        fun shippingAddressElement(
            configuration: ShippingAddressElement.Configuration
        ): Configuration = apply {
            this.shippingAddressElementConfiguration = configuration
        }

        /**
         * Sets the configuration for the express checkout element.
         */
        fun expressCheckoutElement(
            configuration: ExpressCheckoutElement.Configuration
        ): Configuration = apply {
            this.expressCheckoutElementConfiguration = configuration
        }

        /**
         * Sets the Google Pay configuration for this checkout session.
         */
        fun googlePayConfiguration(
            configuration: GooglePayConfiguration,
        ): Configuration = apply {
            this.googlePayConfiguration = configuration
        }

        /**
         * Prefill values for the customer's details. If known up front, these prepopulate the
         * elements (and the Checkout Session) so the customer doesn't re-enter them.
         */
        fun defaults(
            defaults: Defaults
        ): Configuration = apply {
            this.defaults = defaults
        }

        @Parcelize
        internal data class State(
            val adaptivePricingAllowed: Boolean,
            val apiConfiguration: ApiConfiguration.State?,
            val merchantDisplayName: String?,
            val googlePayConfiguration: GooglePayConfiguration.State?,
            val defaults: Defaults.State,
            val paymentElementConfiguration: PaymentElement.Configuration.State,
            val currencySelectorElementConfiguration: CurrencySelectorElement.Configuration.State,
            val shippingAddressElementConfiguration: ShippingAddressElement.Configuration.State,
            val expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
        ) : Parcelable

        internal fun build(): State {
            val defaultsState = defaults.build()
            return State(
                adaptivePricingAllowed = adaptivePricing.build().allowed,
                apiConfiguration = apiConfiguration?.build(),
                merchantDisplayName = merchantDisplayName,
                paymentElementConfiguration = paymentElementConfiguration.build(),
                currencySelectorElementConfiguration = currencySelectorElementConfiguration.build(),
                shippingAddressElementConfiguration = shippingAddressElementConfiguration.build(),
                expressCheckoutElementConfiguration = expressCheckoutElementConfiguration.build(),
                googlePayConfiguration = googlePayConfiguration?.build(),
                defaults = defaultsState,
            )
        }

        /**
         * Configuration for adaptive pricing.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class AdaptivePricing {
            private var allowed: Boolean = false

            /**
             * Sets whether adaptive pricing is allowed for this checkout session.
             */
            fun allowed(allowed: Boolean): AdaptivePricing = apply {
                this.allowed = allowed
            }

            internal fun build(): State = State(
                allowed = allowed,
            )

            internal data class State(
                val allowed: Boolean,
            )
        }

        /**
         * Prefill values for the customer's billing/shipping details and email.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Defaults {
            private var billingDetails: ContactDetails? = null
            private var shippingDetails: ContactDetails? = null
            private var email: String? = null

            /**
             * The customer's known billing contact details.
             */
            fun billingDetails(billingDetails: ContactDetails): Defaults = apply {
                this.billingDetails = billingDetails
            }

            /**
             * The customer's known shipping contact details.
             */
            fun shippingDetails(shippingDetails: ContactDetails): Defaults = apply {
                this.shippingDetails = shippingDetails
            }

            /**
             * The customer's known email address.
             */
            fun email(email: String?): Defaults = apply {
                this.email = email
            }

            @Parcelize
            internal data class State(
                val billingDetails: ContactDetails.State?,
                val shippingDetails: ContactDetails.State?,
                val email: String?,
            ) : Parcelable

            internal fun build(): State = State(
                billingDetails = billingDetails?.build(),
                shippingDetails = shippingDetails?.build(),
                email = email?.trim()?.takeIf { it.isNotEmpty() },
            )

            /**
             * A name, phone number, and postal address for a customer.
             */
            @CheckoutSessionPreview
            @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
            class ContactDetails {
                private var name: String? = null
                private var phoneNumber: String? = null
                private var address: Address? = null

                fun name(name: String?): ContactDetails = apply { this.name = name }

                fun phoneNumber(phoneNumber: String?): ContactDetails = apply {
                    this.phoneNumber = phoneNumber
                }

                fun address(address: Address): ContactDetails = apply { this.address = address }

                @Parcelize
                internal data class State(
                    val name: String?,
                    val phoneNumber: String?,
                    val address: Address.State?,
                ) : Parcelable

                internal fun build(): State = State(
                    name = name,
                    phoneNumber = phoneNumber,
                    address = address?.build(),
                )
            }
        }
    }

    /**
     * Builder for an address passed to [updateShippingAddress] and [updateBillingAddress].
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Address {
        private var city: String? = null
        private var country: String? = null
        private var line1: String? = null
        private var line2: String? = null
        private var postalCode: String? = null
        private var state: String? = null

        /**
         * Sets the city, district, suburb, town, or village.
         */
        fun city(city: String?) = apply {
            this.city = city
        }

        /**
         * Sets the two-letter country code (ISO 3166-1 alpha-2). Required.
         */
        fun country(country: String) = apply {
            this.country = country
        }

        /**
         * Sets address line 1 (e.g., street, PO Box, or company name).
         */
        fun line1(line1: String?) = apply {
            this.line1 = line1
        }

        /**
         * Sets address line 2 (e.g., apartment, suite, unit, or building).
         */
        fun line2(line2: String?) = apply {
            this.line2 = line2
        }

        /**
         * Sets the ZIP or postal code.
         */
        fun postalCode(postalCode: String?) = apply {
            this.postalCode = postalCode
        }

        /**
         * Sets the state, county, province, or region.
         */
        fun state(state: String?) = apply {
            this.state = state
        }

        @Parcelize
        internal data class State(
            val city: String?,
            val country: String,
            val line1: String?,
            val line2: String?,
            val postalCode: String?,
            val state: String?,
        ) : Parcelable

        internal fun build(): State {
            return State(
                city = city?.trim(),
                country = requireNotNull(country?.trim()) {
                    "Country is required."
                },
                line1 = line1?.trim(),
                line2 = line2?.trim(),
                postalCode = postalCode?.trim(),
                state = state?.trim(),
            )
        }
    }

    /**
     * The result of a checkout payment flow, delivered to [ResultCallback].
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        /**
         * The customer completed the payment flow.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Completed internal constructor() : Result

        /**
         * The customer canceled the payment flow.
         */
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Canceled internal constructor() : Result

        /**
         * The payment flow failed with [error].
         *
         * @property error The error that caused the payment flow to fail.
         */
        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Failed internal constructor(val error: Throwable) : Result
    }

    /**
     * Callback invoked with the [Result] of a checkout payment flow.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ResultCallback {
        /**
         * Called when the payment flow finishes with [result].
         */
        fun onResult(result: Result)
    }
}
