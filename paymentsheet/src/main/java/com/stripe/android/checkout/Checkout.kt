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
    internalState: InternalState,
    private val component: CheckoutComponent,
) {
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        suspend fun configure(
            context: Context,
            checkoutSessionClientSecret: String,
        ): Result<Checkout> {
            val component = DaggerCheckoutComponent.factory().create(context.applicationContext)
            return component.checkoutSessionLoader.load(checkoutSessionClientSecret).map { response ->
                Checkout(
                    internalState = InternalState(
                        key = UUID.randomUUID().toString(),
                        checkoutSessionResponse = response,
                    ),
                    component,
                )
            }
        }

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

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val internalState: InternalState,
    ) : Parcelable

    @Volatile
    internal var internalState: InternalState = internalState
        private set

    var state: State
        get() = State(internalState)
        set(value) {
            ensureNoMutationInFlight()
            internalState = value.internalState
        }

    private val mutex = Mutex()

    private val _checkoutSession = MutableStateFlow(internalState.checkoutSessionResponse.asCheckoutSession())
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()

    init {
        CheckoutInstances.add(internalState.key, this)
    }

    suspend fun applyPromotionCode(
        promotionCode: String,
    ): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, promotionCode.trim())
    }

    suspend fun updateLineItemQuantity(
        lineItemId: String,
        quantity: Int,
    ): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.updateLineItemQuantity(sessionId, lineItemId, quantity)
    }

    suspend fun removePromotionCode(): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.applyPromotionCode(sessionId, "")
    }

    suspend fun selectShippingRate(
        shippingRateId: String,
    ): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.selectShippingRate(sessionId, shippingRateId)
    }

    suspend fun updateShippingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<CheckoutSession> {
        val built = address.build()
        return withSessionId(
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
    ): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.updateTaxId(sessionId, type.trim(), value.trim())
    }

    suspend fun updateBillingAddress(
        name: String? = null,
        phoneNumber: String? = null,
        address: Address,
    ): Result<CheckoutSession> {
        val built = address.build()
        return withSessionId(
            additionalStateMutations = {
                copy(billingName = name, billingPhoneNumber = phoneNumber, billingAddress = built)
            },
        ) { sessionId ->
            component.checkoutSessionRepository.updateTaxRegion(sessionId, built)
        }
    }

    suspend fun refresh(): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.init(sessionId)
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

    private suspend fun withSessionId(
        additionalStateMutations: InternalState.() -> InternalState = { this },
        block: suspend (sessionId: String) -> Result<CheckoutSessionResponse>,
    ): Result<CheckoutSession> {
        if (internalState.integrationLaunched) {
            return Result.failure(
                IllegalStateException(
                    "Cannot mutate checkout session while a payment flow is presented."
                )
            )
        }
        // Run network requests with a mutex to ensure events are processed in order.
        return mutex.withLock {
            block(internalState.checkoutSessionResponse.id).map { response ->
                internalState = internalState.copy(checkoutSessionResponse = response).additionalStateMutations()
                val checkoutSession = response.asCheckoutSession()
                _checkoutSession.value = checkoutSession
                checkoutSession
            }
        }
    }
}
