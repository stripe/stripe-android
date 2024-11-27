package com.stripe.android.link

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.StripeIntent
import kotlinx.coroutines.flow.Flow

interface LinkIntentConfirmationHandler {
    val state: Flow<State>

    suspend fun confirmIntent(
        intent: StripeIntent,
        params: PaymentMethodCreateParams
    )

    sealed interface State {
        data class Failed(
            val cause: Throwable,
            val message: ResolvableString,
        ): State

        data object Cancelled : State

        data object Idle : State

        data class Success(val paymentMethod: PaymentMethod) : State
    }
}

object LinkIntentConfirmation {
    var handler: LinkIntentConfirmationHandler? = null
}