package com.stripe.android.checkout

import android.app.Application
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.checkout.injection.CheckoutPresenterSubcomponent
import com.stripe.android.checkout.injection.DaggerCheckoutControllerComponent
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.validateShippingCountry
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

private val SERVER_UPDATE_TIMEOUT_MS = 20.seconds.inWholeMilliseconds

@Singleton
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions", "UnusedParameter")
class CheckoutController @Inject internal constructor(
    resultCallback: ResultCallback,
    @ViewModelScope private val viewModelScope: CoroutineScope,
    private val checkoutSessionRepository: CheckoutSessionRepository,
    private val checkoutStateLoader: CheckoutStateLoader,
    private val stateHolder: CheckoutControllerStateHolder,
    private val checkoutPresenterSubcomponentFactory: CheckoutPresenterSubcomponent.Factory,
) {
    val checkoutSession: StateFlow<CheckoutSession?>
        get() = stateHolder.checkoutSession

    private val mutex = Mutex()
    private val pendingMutations = AtomicInteger(0)

    private val _isLoading = MutableStateFlow(false)

    /**
     * Whether a mutation is currently in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    suspend fun configure(
        checkoutSessionClientSecret: String,
        configuration: Configuration = Configuration(),
    ): kotlin.Result<Unit> = runSerialized {
        val configurationState = configuration.build()
        val sessionId = checkoutSessionClientSecret.substringBefore("_secret_")

        checkoutSessionRepository.init(
            sessionId = sessionId,
            adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
        ).mapCatching { response ->
            checkoutStateLoader.loadInitial(
                configuration = configurationState,
                checkoutSessionResponse = response,
            )
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
     * Updates the quantity of a line item.
     *
     * @param lineItemId The ID of the line item to update.
     * @param quantity The new quantity.
     */
    suspend fun updateLineItemQuantity(
        lineItemId: String,
        quantity: Int,
    ): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        checkoutSessionRepository.updateLineItemQuantity(sessionId, lineItemId, quantity)
    }

    /**
     * Selects a shipping option.
     *
     * @param id The ID of the shipping option to select.
     */
    suspend fun selectShippingOption(
        id: String,
    ): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        checkoutSessionRepository.selectShippingRate(sessionId, id)
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
     * Updates the customer's tax ID.
     *
     * @param type The type of tax ID (e.g. "eu_vat"). Leading/trailing whitespace is trimmed.
     * @param value The tax ID value. Leading/trailing whitespace is trimmed.
     */
    suspend fun updateTaxId(
        type: String,
        value: String,
    ): kotlin.Result<Unit> = withCheckoutState { sessionId ->
        checkoutSessionRepository.updateTaxId(sessionId, type.trim(), value.trim())
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
     * then automatically refreshes [checkoutSession] with the latest session data.
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
     * Runs a mutation against the checkout session, serializing it behind [mutex] so mutations run
     * in sequence. [block] produces the updated [CheckoutSessionResponse]; the result is folded into
     * a new [CheckoutControllerState] (with any [additionalStateMutations] applied) and handed to
     * [checkoutStateLoader] to reload the payment element and atomically commit the new state.
     *
     * Returns [kotlin.Result.failure] if the session hasn't been configured yet or a payment flow is
     * currently presented.
     */
    private suspend fun withCheckoutState(
        additionalStateMutations: CheckoutControllerState.() -> CheckoutControllerState = { this },
        block: suspend CheckoutControllerState.(sessionId: String) -> kotlin.Result<CheckoutSessionResponse>,
    ): kotlin.Result<Unit> {
        val currentState = stateHolder.state
            ?: return kotlin.Result.failure(
                IllegalStateException("Cannot mutate checkout session before it is configured.")
            )
        if (currentState.integrationLaunched) {
            return kotlin.Result.failure(
                IllegalStateException("Cannot mutate checkout session while a payment flow is presented.")
            )
        }
        return runSerialized {
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
     * Serializes [block] behind [mutex] so configuration and mutations run in sequence, and toggles
     * [isLoading] while any serialized work is in flight (tracked via [pendingMutations] so
     * concurrent callers share a single loading window).
     */
    private suspend fun <T> runSerialized(
        block: suspend () -> kotlin.Result<T>,
    ): kotlin.Result<T> {
        if (pendingMutations.incrementAndGet() == 1) {
            _isLoading.value = true
        }
        return try {
            // Run network requests with a mutex to ensure events are processed in order.
            mutex.withLock {
                block()
            }
        } finally {
            if (pendingMutations.decrementAndGet() == 0) {
                _isLoading.value = false
            }
        }
    }

    internal suspend fun updateCurrency(currency: String): kotlin.Result<Unit> {
        return withCheckoutState { sessionId ->
            checkoutSessionRepository.updateCurrency(sessionId, currency)
        }
    }

    fun createPresenter(activity: ComponentActivity): CheckoutPresenter {
        return checkoutPresenterSubcomponentFactory.create().presenter
    }

    fun destroy() {
        viewModelScope.cancel()
        stateHolder.state = null
    }

    fun clearPaymentOption() {
        TODO("Not yet implemented")
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Builder(
        private val application: Application,
        private val savedStateHandle: SavedStateHandle,
    ) {
        private var resultCallback: ResultCallback = ResultCallback {}

        fun resultCallback(
            resultCallback: ResultCallback
        ): Builder = apply {
            this.resultCallback = resultCallback
        }

        fun build(): CheckoutController {
            val component = DaggerCheckoutControllerComponent.factory().create(
                application = application,
                savedStateHandle = savedStateHandle,
                resultCallback = resultCallback,
            )

            return component.checkoutController
        }
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false
        private var googlePayConfiguration: GooglePayConfiguration? = null
        private var paymentElementConfiguration: PaymentElement.Configuration = PaymentElement.Configuration()
        private var currencySelectorElementConfiguration: CurrencySelectorElement.Configuration =
            CurrencySelectorElement.Configuration()
        private var shippingAddressElementConfiguration: ShippingAddressElement.Configuration =
            ShippingAddressElement.Configuration()
        private var expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration =
            ExpressCheckoutElement.Configuration()

        fun adaptivePricingAllowed(
            adaptivePricingAllowed: Boolean
        ): Configuration = apply {
            this.adaptivePricingAllowed = adaptivePricingAllowed
        }

        fun paymentElement(
            configuration: PaymentElement.Configuration
        ): Configuration = apply {
            this.paymentElementConfiguration = configuration
        }

        fun currencySelectorElement(
            configuration: CurrencySelectorElement.Configuration
        ): Configuration = apply {
            this.currencySelectorElementConfiguration = configuration
        }

        fun shippingAddressElement(
            configuration: ShippingAddressElement.Configuration
        ): Configuration = apply {
            this.shippingAddressElementConfiguration = configuration
        }

        fun expressCheckoutElement(
            configuration: ExpressCheckoutElement.Configuration
        ): Configuration = apply {
            this.expressCheckoutElementConfiguration = configuration
        }

        fun googlePayConfiguration(
            configuration: GooglePayConfiguration,
        ): Configuration = apply {
            this.googlePayConfiguration = configuration
        }

        @Parcelize
        internal data class State(
            val adaptivePricingAllowed: Boolean,
            val googlePayConfiguration: GooglePayConfiguration.State?,
            val paymentElementConfiguration: PaymentElement.Configuration.State,
            val currencySelectorElementConfiguration: CurrencySelectorElement.Configuration.State,
            val shippingAddressElementConfiguration: ShippingAddressElement.Configuration.State,
            val expressCheckoutElementConfiguration: ExpressCheckoutElement.Configuration.State,
        ) : Parcelable

        internal fun build(): State = State(
            adaptivePricingAllowed = adaptivePricingAllowed,
            paymentElementConfiguration = paymentElementConfiguration.build(),
            currencySelectorElementConfiguration = currencySelectorElementConfiguration.build(),
            shippingAddressElementConfiguration = shippingAddressElementConfiguration.build(),
            expressCheckoutElementConfiguration = expressCheckoutElementConfiguration.build(),
            googlePayConfiguration = googlePayConfiguration?.build(),
        )
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed interface Result {
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Completed internal constructor() : Result

        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Canceled internal constructor() : Result

        @Poko
        @CheckoutSessionPreview
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        class Failed internal constructor(val error: Throwable) : Result
    }

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun interface ResultCallback {
        fun onResult(result: Result)
    }
}
