package com.stripe.android.checkout

import android.app.Application
import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.checkout.Checkout.Companion.configure
import com.stripe.android.checkout.Checkout.Companion.createWithState
import com.stripe.android.checkout.injection.CheckoutComponent
import com.stripe.android.checkout.injection.DaggerCheckoutComponent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

private val SERVER_UPDATE_TIMEOUT_MS = 20.seconds.inWholeMilliseconds

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
            val application = context.applicationContext as Application
            val component = DaggerCheckoutComponent.factory().create(application)
            val configurationState = configuration.build()
            return component.checkoutSessionRepository.init(
                sessionId = checkoutSessionClientSecret.substringBefore("_secret_"),
                adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
            ).map { response ->
                val flagImages = component.flagImageResolver.resolve(response, cached = null)
                val key = UUID.randomUUID().toString()
                CheckoutInstances.getOrCreate(key) {
                    Checkout(
                        internalState = InternalState(
                            key = key,
                            configuration = configurationState,
                            checkoutSessionResponse = response,
                            flagImages = flagImages,
                        ),
                        component = component,
                    )
                }
            }
        }

        /**
         * Returns the existing [Checkout] if one is still alive, or creates a new one from [state].
         */
        fun createWithState(
            context: Context,
            state: State,
        ): Checkout {
            return CheckoutInstances.getOrCreate(state.internalState.key) {
                val application = context.applicationContext as Application
                val component = DaggerCheckoutComponent.factory().create(application)
                Checkout(
                    internalState = state.internalState,
                    component = component,
                )
            }
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
            _checkoutSession.value = internalState.asCheckoutSession()
        }

    private val mutex = Mutex()
    private val pendingMutations = AtomicInteger(0)

    private val _checkoutSession = MutableStateFlow(
        internalState.asCheckoutSession()
    )

    /**
     * The current [CheckoutSession], updated after each successful mutation.
     */
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    /**
     * Whether a mutation is currently in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
     * Runs an async function that calls your server to update the Checkout Session,
     * then automatically refreshes [checkoutSession] with the latest session data.
     *
     * A 20-second timeout is enforced. If [serverUpdate] doesn't complete within 20 seconds,
     * this method returns a [Result.failure] with a timeout exception.
     *
     * @param serverUpdate A suspend function that makes a request to your server to update
     * the Checkout Session.
     */
    suspend fun runServerUpdate(
        serverUpdate: suspend () -> Result<Unit>,
    ): Result<Unit> = withInternalState { sessionId ->
        withTimeout(SERVER_UPDATE_TIMEOUT_MS) { serverUpdate() }.fold(
            onSuccess = {
                component.checkoutSessionRepository.init(
                    sessionId = sessionId,
                    adaptivePricingAllowed = configuration.adaptivePricingAllowed,
                )
            },
            onFailure = { Result.failure(it) },
        )
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
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<Unit> {
        internalState.checkoutSessionResponse
            .validateShippingCountry(address.build().country)
            .onFailure { return Result.failure(it) }
        return updateAddress(CheckoutSessionResponse.TaxAddressSource.SHIPPING, address) {
            copy(shippingName = name, shippingPhoneNumber = phoneNumber, shippingAddress = it)
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
     * Sets the billing address for this checkout.
     *
     * The address is stored locally and used when presenting payment UI. If automatic tax is
     * enabled and the tax address source is billing, the address is also sent to the server
     * to compute updated tax amounts.
     *
     * @param name The billing name.
     * @param phoneNumber The billing phone number.
     * @param address The billing address.
     */
    suspend fun updateBillingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<Unit> = updateAddress(CheckoutSessionResponse.TaxAddressSource.BILLING, address) {
        copy(billingName = name, billingPhoneNumber = phoneNumber, billingAddress = it)
    }

    private suspend fun updateAddress(
        addressType: CheckoutSessionResponse.TaxAddressSource,
        address: Address,
        mutation: InternalState.(Address.State) -> InternalState,
    ): Result<Unit> {
        val built = address.build()
        val response = internalState.checkoutSessionResponse
        val shouldSendTaxRegion =
            response.automaticTaxEnabled && response.taxAddressSource == addressType
        return withInternalState(
            additionalStateMutations = { mutation(built) },
        ) { sessionId ->
            if (shouldSendTaxRegion) {
                component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
            } else {
                Result.success(checkoutSessionResponse)
            }
        }
    }

    internal suspend fun updateCurrency(currency: String): Result<Unit> = withInternalState { sessionId ->
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
        _checkoutSession.value = internalState.asCheckoutSession()
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
        if (pendingMutations.incrementAndGet() == 1) {
            _isLoading.value = true
        }
        return try {
            // Run network requests with a mutex to ensure events are processed in order.
            mutex.withLock {
                val result = runCatching {
                    internalState.block(internalState.checkoutSessionResponse.id).getOrThrow()
                }.map { response ->
                    val flagImages = component.flagImageResolver.resolve(
                        response = response,
                        cached = internalState.flagImages,
                    )
                    internalState = internalState
                        .copy(checkoutSessionResponse = response, flagImages = flagImages)
                        .additionalStateMutations()
                    _checkoutSession.value = internalState.asCheckoutSession()
                }
                result
            }
        } finally {
            if (pendingMutations.decrementAndGet() == 0) {
                _isLoading.value = false
            }
        }
    }
}
