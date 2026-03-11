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

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Checkout private constructor(
    var state: State,
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
                Checkout(State(response), component)
            }
        }

        fun createWithState(
            context: Context,
            state: State,
        ): Checkout {
            val component = DaggerCheckoutComponent.factory().create(context.applicationContext)
            return Checkout(state, component)
        }
    }

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val checkoutSessionResponse: CheckoutSessionResponse,
        internal val shippingName: String? = null,
    ) : Parcelable

    private val mutex = Mutex()
    private val _checkoutSession = MutableStateFlow(state.checkoutSessionResponse.asCheckoutSession())
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()

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
        address: Address,
        name: String? = null,
    ): Result<CheckoutSession> = withSessionId(
        setState = { State(it, shippingName = name ?: state.shippingName) },
    ) { sessionId ->
        component.checkoutSessionRepository.updateShippingAddress(sessionId, address.build())
    }

    suspend fun refresh(): Result<CheckoutSession> = withSessionId { sessionId ->
        component.checkoutSessionRepository.init(sessionId)
    }

    private suspend fun withSessionId(
        setState: (CheckoutSessionResponse) -> State = { State(it, shippingName = state.shippingName) },
        block: suspend (sessionId: String) -> Result<CheckoutSessionResponse>,
    ): Result<CheckoutSession> {
        // Run network requests with a mutex to ensure events are processed in order.
        return mutex.withLock {
            block(state.checkoutSessionResponse.id).updateState(setState)
        }
    }

    private fun Result<CheckoutSessionResponse>.updateState(
        setState: (CheckoutSessionResponse) -> State,
    ): Result<CheckoutSession> {
        return map { response ->
            state = setState(response)
            val checkoutSession = response.asCheckoutSession()
            _checkoutSession.value = checkoutSession
            checkoutSession
        }
    }
}
