package com.stripe.android.paymentsheet.events

import kotlin.time.Duration

@ExperimentalEventsApi
class PaymentSuccessEvent internal constructor(
    val duration: Duration?,
)
