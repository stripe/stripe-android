package com.stripe.android.stripecardscan.framework.time

import kotlin.math.roundToLong

/**
 * Allow delaying for a specified duration
 */
suspend fun delay(duration: Duration) =
    kotlinx.coroutines.delay(duration.inMilliseconds.roundToLong())
