package com.stripe.android.stripecardscan.framework.time

import com.stripe.android.camera.framework.time.Duration
import kotlin.math.roundToLong

/**
 * Allow delaying for a specified duration
 */
internal suspend fun delay(duration: Duration) =
    kotlinx.coroutines.delay(duration.inMilliseconds.roundToLong())
