package com.stripe.android

import androidx.annotation.RestrictTo
import com.stripe.android.model.PaymentIntent
import kotlinx.parcelize.Parcelize

/**
 * A model representing the result of a [PaymentIntent] confirmation via [Stripe.confirmPayment]
 * or handling of next actions via [Stripe.handleNextActionForPayment].
 */
@Parcelize
data class PaymentIntentResult
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
constructor(
    override val intent: PaymentIntent,
    @Outcome private val outcomeFromFlow: Int = 0,
    override val failureMessage: String? = null
) : StripeIntentResult<PaymentIntent>(outcomeFromFlow)
