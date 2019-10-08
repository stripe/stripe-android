package com.stripe.android

import com.stripe.android.model.PaymentIntent

class PaymentIntentResult internal constructor(
    paymentIntent: PaymentIntent,
    @Outcome outcome: Int = 0
) : StripeIntentResult<PaymentIntent>(paymentIntent, outcome)
