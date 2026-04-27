package com.stripe.android.checkout

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.stripe.android.checkout.Checkout.Companion.configure
import com.stripe.android.checkout.Checkout.Companion.createWithState
import com.stripe.android.checkout.injection.CheckoutComponent
import com.stripe.android.checkout.injection.DaggerCheckoutComponent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.verticalmode.CurrencySelectorToggle
import com.stripe.android.uicore.utils.collectAsState
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Manages a Checkout Session, providing methods to observe and mutate its state.
 *
 * Create a new instance with [configure], or restore a previously saved instance with [createWithState].
 * Observe session updates via [checkoutSession] and loading state via [isLoading].
 *
 * Mutation methods are queued and run in sequence. They return [Result.failure] if a
 * payment flow is currently presented.
 */
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class Checkout private constructor(
    internalState: InternalState,
    private val component: CheckoutComponent,
) {
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        /**
         * Creates and initializes a new [Checkout] by fetching the session from the server.
         *
         * @param checkoutSessionClientSecret The client secret for the checkout session.
         * @param configuration Optional configuration options.
         */
        suspend fun configure(
            context: Context,
            checkoutSessionClientSecret: String,
            configuration: Configuration = Configuration(),
        ): Result<Checkout> {
            val component = DaggerCheckoutComponent.factory().create(context.applicationContext)
            val configurationState = configuration.build()
            return component.checkoutSessionRepository.init(
                sessionId = checkoutSessionClientSecret.substringBefore("_secret_"),
                adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
            ).map { response ->
                Checkout(
                    internalState = InternalState(
                        key = UUID.randomUUID().toString(),
                        configuration = configurationState,
                        checkoutSessionResponse = response,
                    ),
                    component,
                )
            }
        }

        /**
         * Recreates a [Checkout] from a previously saved [State], such as after process death.
         */
        fun createWithState(
            context: Context,
            state: State,
        ): Checkout {
            val component = DaggerCheckoutComponent.factory().create(context.applicationContext)
            return Checkout(
                internalState = state.internalState,
                component = component,
            )
        }
    }

    /**
     * A serializable snapshot of a [Checkout] instance's state.
     * Save via the [state] property and restore with [createWithState].
     */
    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val internalState: InternalState,
    ) : Parcelable

    /**
     * Builder for configuring a [Checkout] instance.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false

        /**
         * Whether to allow adaptive pricing, which displays amounts in the customer's local currency.
         */
        fun adaptivePricingAllowed(adaptivePricingAllowed: Boolean) = apply {
            this.adaptivePricingAllowed = adaptivePricingAllowed
        }

        @Parcelize
        internal data class State(
            val adaptivePricingAllowed: Boolean,
        ) : Parcelable

        internal fun build() = State(
            adaptivePricingAllowed = adaptivePricingAllowed,
        )
    }

    @Volatile
    internal var internalState: InternalState = internalState
        private set

    /**
     * A serializable snapshot of this instance's current state. Can be saved and later passed to
     * [createWithState] to restore.
     *
     * @throws IllegalStateException if a mutation is in flight while trying to set the state.
     */
    var state: State
        get() = State(internalState)
        set(value) {
            ensureNoMutationInFlight()
            internalState = value.internalState
        }

    private val mutex = Mutex()

    private val _checkoutSession = MutableStateFlow(internalState.checkoutSessionResponse.asCheckoutSession())

    /**
     * The current [CheckoutSession], updated after each successful mutation.
     */
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /**
     * Whether a mutation is currently in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        CheckoutInstances.add(internalState.key, this)
    }

    /**
     * Applies a promotion code to the checkout session.
     *
     * @param promotionCode The promotion code to apply. Leading/trailing whitespace is trimmed.
     */
    suspend fun applyPromotionCode(
        promotionCode: String,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, promotionCode.trim())
    }

    /**
     * Updates the quantity of a line item.
     *
     * @param lineItemId The ID of the line item to update.
     * @param quantity The new quantity.
     */
    suspend fun updateLineItemQuantity(
        lineItemId: String,
        quantity: Int,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateLineItemQuantity(sessionId, lineItemId, quantity)
    }

    /**
     * Removes the currently applied promotion code from the checkout session.
     */
    suspend fun removePromotionCode(): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, "")
    }

    /**
     * Selects a shipping option.
     *
     * @param id The ID of the shipping option to select.
     */
    suspend fun selectShippingOption(
        id: String,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.selectShippingRate(sessionId, id)
    }

    /**
     * Updates the shipping address and recalculates taxes.
     *
     * @param name The recipient's name.
     * @param phoneNumber The recipient's phone number.
     * @param address The shipping address.
     */
    suspend fun updateShippingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<Unit> {
        val built = address.build()
        return withInternalState(
            additionalStateMutations = {
                copy(shippingName = name, shippingPhoneNumber = phoneNumber, shippingAddress = built)
            },
        ) { sessionId ->
            component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
        }
    }

    /**
     * Updates the customer's tax ID.
     *
     * @param type The type of tax ID (e.g. "eu_vat"). Leading/trailing whitespace is trimmed.
     * @param value The tax ID value. Leading/trailing whitespace is trimmed.
     */
    suspend fun updateTaxId(
        type: String,
        value: String,
    ): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateTaxId(sessionId, type.trim(), value.trim())
    }

    /**
     * Updates the billing address and recalculates taxes.
     *
     * @param name The billing name.
     * @param phoneNumber The billing phone number.
     * @param address The billing address.
     */
    suspend fun updateBillingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<Unit> {
        val built = address.build()
        return withInternalState(
            additionalStateMutations = {
                copy(billingName = name, billingPhoneNumber = phoneNumber, billingAddress = built)
            },
        ) { sessionId ->
            component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
        }
    }

    /**
     * Re-fetches the checkout session from the server, replacing the local state.
     */
    suspend fun refresh(): Result<Unit> = withInternalState { sessionId ->
        component.checkoutSessionRepository.init(
            sessionId = sessionId,
            adaptivePricingAllowed = configuration.adaptivePricingAllowed
        )
    }

    internal suspend fun updateCurrency(currency: String) = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateCurrency(sessionId, currency)
    }

    internal fun markIntegrationLaunched() {
        internalState = internalState.copy(integrationLaunched = true)
    }

    internal fun markIntegrationDismissed() {
        internalState = internalState.copy(integrationLaunched = false)
    }

    internal fun ensureNoMutationInFlight() {
        if (mutex.isLocked) {
            throw IllegalStateException(
                "Cannot launch while a checkout session mutation is in flight."
            )
        }
    }

    internal fun updateWithResponse(response: CheckoutSessionResponse) {
        internalState = internalState.copy(checkoutSessionResponse = response)
        _checkoutSession.value = response.asCheckoutSession()
    }

    private suspend fun withInternalState(
        additionalStateMutations: InternalState.() -> InternalState = { this },
        block: suspend InternalState.(sessionId: String) -> Result<CheckoutSessionResponse>,
    ): Result<Unit> {
        if (internalState.integrationLaunched) {
            return Result.failure(
                IllegalStateException(
                    "Cannot mutate checkout session while a payment flow is presented."
                )
            )
        }
        // Run network requests with a mutex to ensure events are processed in order.
        return mutex.withLock {
            _isLoading.value = true
            val result = internalState.block(internalState.checkoutSessionResponse.id).map { response ->
                internalState = internalState.copy(checkoutSessionResponse = response).additionalStateMutations()
                _checkoutSession.value = response.asCheckoutSession()
            }
            _isLoading.value = false
            result
        }
    }

    @Composable
    fun CurrencySelectorContent() {
        val scope = rememberCoroutineScope()
        val isLoading by isLoading.collectAsState()
        val checkoutSession by checkoutSession.collectAsState()
        val currencySelectorOptions = checkoutSession.currencySelectorOptions ?: return
        CurrencySelectorToggle(
            options = currencySelectorOptions,
            onCurrencySelected = { currencyOption ->
                scope.launch(Dispatchers.Main.immediate) { updateCurrency(currencyOption.code) }
            },
            isEnabled = !isLoading,
        )
    }
}
