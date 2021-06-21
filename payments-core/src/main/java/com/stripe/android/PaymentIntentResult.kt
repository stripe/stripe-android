package com.stripe.android

import com.stripe.android.model.PaymentIntent
import kotlinx.parcelize.Parcelize

/**
 * A model representing the result of a [PaymentIntent] confirmation via [Stripe.confirmPayment]
 * or handling of next actions via [Stripe.handleNextActionForPayment].
 */
@Parcelize
data class PaymentIntentResult internal constructor(
    override val intent: PaymentIntent,
    @Outcome private val outcomeFromFlow: Int = 0,
    override val failureMessage: String? = null
) : StripeIntentResult<PaymentIntent>(outcomeFromFlow)
