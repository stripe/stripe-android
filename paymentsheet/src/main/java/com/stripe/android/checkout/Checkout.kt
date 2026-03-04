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
    ) : Parcelable

    private val _checkoutSession = MutableStateFlow(state.checkoutSessionResponse.asCheckoutSession())
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()

    suspend fun applyPromotionCode(promotionCode: String): Result<CheckoutSession> {
        val sessionId = state.checkoutSessionResponse.id
        return component.checkoutSessionRepository
            .applyPromotionCode(sessionId, promotionCode.trim())
            .map { response ->
                state = State(response)
                val checkoutSession = response.asCheckoutSession()
                _checkoutSession.value = checkoutSession
                checkoutSession
            }
    }
}
