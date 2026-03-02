package com.stripe.android.checkout

import android.content.Context
import android.os.Parcelable
import androidx.annotation.RestrictTo
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
    var state: State
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
                Checkout(State(checkoutSessionClientSecret, response))
            }
        }

        fun createWithState(
            state: State,
        ): Checkout {
            return Checkout(state)
        }
    }

    @Poko
    @Parcelize
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    class State internal constructor(
        internal val checkoutSessionClientSecret: String,
        internal val checkoutSessionResponse: CheckoutSessionResponse,
    ) : Parcelable

    private val _checkoutSession = MutableStateFlow(state.checkoutSessionResponse.asCheckoutSession())
    val checkoutSession: StateFlow<CheckoutSession> = _checkoutSession.asStateFlow()
}
