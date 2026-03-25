package com.stripe.android.checkout

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.checkout.injection.CheckoutComponent
import com.stripe.android.checkout.injection.DaggerCheckoutComponent
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.parcelize.Parcelize
import java.util.UUID

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Suppress("TooManyFunctions")
class Checkout private constructor(
    private val component: CheckoutComponent,
    val key: String,
    initialInternalState: InternalState?,
) {
    constructor(context: Context) : this(
        component = DaggerCheckoutComponent.factory().create(context.applicationContext),
        key = UUID.randomUUID().toString(),
        initialInternalState = null,
    )

    constructor(context: Context, state: State) : this(
        component = DaggerCheckoutComponent.factory().create(context.applicationContext),
        key = state.internalState.key,
        initialInternalState = state.internalState,
    )

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val internalState: InternalState,
    ) : Parcelable

    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class Configuration {
        private var adaptivePricingAllowed: Boolean = false

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
    internal var internalState: InternalState? = initialInternalState
        private set

    var state: State
        get() = State(requireConfigured())
        set(value) {
            ensureNoMutationInFlight()
            internalState = value.internalState
        }

    private val mutex = Mutex()

    private val _checkoutSession = MutableStateFlow(initialInternalState?.checkoutSessionResponse?.asCheckoutSession())
    val checkoutSession: StateFlow<CheckoutSession?> = _checkoutSession.asStateFlow()

    init {
        CheckoutInstances.add(key, this)
    }

    suspend fun configure(
        checkoutSessionClientSecret: String,
        configuration: Configuration = Configuration(),
    ): Result<CheckoutSession> {
        if (mutex.isLocked) {
            return Result.failure(
                IllegalStateException("Cannot launch while a checkout session mutation is in flight.")
            )
        }
        val currentState = internalState
        if (currentState != null && currentState.integrationLaunched) {
            return Result.failure(
                IllegalStateException("Cannot configure while a payment flow is presented.")
            )
        }
        val configurationState = configuration.build()
        return component.checkoutSessionRepository.init(
            sessionId = checkoutSessionClientSecret.substringBefore("_secret_"),
            adaptivePricingAllowed = configurationState.adaptivePricingAllowed,
        ).map { response ->
            internalState = InternalState(
                key = key,
                configuration = configurationState,
                checkoutSessionResponse = response,
            )
            val checkoutSession = response.asCheckoutSession()
            _checkoutSession.value = checkoutSession
            checkoutSession
        }
    }

    suspend fun applyPromotionCode(
        promotionCode: String,
    ): Result<CheckoutSession> = withInternalState { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, promotionCode.trim())
    }

    suspend fun updateLineItemQuantity(
        lineItemId: String,
        quantity: Int,
    ): Result<CheckoutSession> = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateLineItemQuantity(sessionId, lineItemId, quantity)
    }

    suspend fun removePromotionCode(): Result<CheckoutSession> = withInternalState { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, "")
    }

    suspend fun selectShippingOption(
        id: String,
    ): Result<CheckoutSession> = withInternalState { sessionId ->
        component.checkoutSessionRepository.selectShippingRate(sessionId, id)
    }

    suspend fun updateShippingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<CheckoutSession> {
        val built = address.build()
        return withInternalState(
            additionalStateMutations = {
                copy(shippingName = name, shippingPhoneNumber = phoneNumber, shippingAddress = built)
            },
        ) { sessionId ->
            component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
        }
    }

    suspend fun updateTaxId(
        type: String,
        value: String,
    ): Result<CheckoutSession> = withInternalState { sessionId ->
        component.checkoutSessionRepository.updateTaxId(sessionId, type.trim(), value.trim())
    }

    suspend fun updateBillingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<CheckoutSession> {
        val built = address.build()
        return withInternalState(
            additionalStateMutations = {
                copy(billingName = name, billingPhoneNumber = phoneNumber, billingAddress = built)
            },
        ) { sessionId ->
            component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
        }
    }

    suspend fun refresh(): Result<CheckoutSession> = withInternalState { sessionId ->
        component.checkoutSessionRepository.init(
            sessionId = sessionId,
            adaptivePricingAllowed = configuration.adaptivePricingAllowed
        )
    }

    internal fun markIntegrationLaunched() {
        internalState = requireConfigured().copy(integrationLaunched = true)
    }

    internal fun markIntegrationDismissed() {
        internalState = requireConfigured().copy(integrationLaunched = false)
    }

    internal fun ensureNoMutationInFlight() {
        if (mutex.isLocked) {
            throw IllegalStateException(
                "Cannot launch while a checkout session mutation is in flight."
            )
        }
    }

    internal fun updateWithResponse(response: CheckoutSessionResponse) {
        internalState = requireConfigured().copy(checkoutSessionResponse = response)
        _checkoutSession.value = response.asCheckoutSession()
    }

    private fun requireConfigured(): InternalState {
        return internalState ?: throw IllegalStateException("Checkout has not been configured.")
    }

    private suspend fun withInternalState(
        additionalStateMutations: InternalState.() -> InternalState = { this },
        block: suspend InternalState.(sessionId: String) -> Result<CheckoutSessionResponse>,
    ): Result<CheckoutSession> {
        val currentState = internalState ?: return Result.failure(
            IllegalStateException("Checkout has not been configured.")
        )
        if (currentState.integrationLaunched) {
            return Result.failure(
                IllegalStateException(
                    "Cannot mutate checkout session while a payment flow is presented."
                )
            )
        }
        return mutex.withLock {
            val lockedState = requireConfigured()
            lockedState.block(lockedState.checkoutSessionResponse.id).map { response ->
                internalState = lockedState.copy(checkoutSessionResponse = response).additionalStateMutations()
                val checkoutSession = response.asCheckoutSession()
                _checkoutSession.value = checkoutSession
                checkoutSession
            }
        }
    }
}
