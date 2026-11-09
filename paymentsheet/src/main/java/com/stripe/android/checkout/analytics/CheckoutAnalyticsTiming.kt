package com.stripe.android.checkout.analytics

import kotlin.time.DurationUnit
import kotlin.time.TimeSource

internal suspend fun <T> MutableMap<String, Any?>.measureDuration(
    key: String,
    block: suspend () -> T,
): T {
    val startedAt = TimeSource.Monotonic.markNow()
    return try {
        block()
    } finally {
        this[key] = startedAt.elapsedNow().toDouble(DurationUnit.SECONDS).toFloat()
    }
}
