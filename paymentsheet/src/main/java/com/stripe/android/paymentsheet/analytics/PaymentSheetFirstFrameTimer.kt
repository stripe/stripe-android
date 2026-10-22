package com.stripe.android.paymentsheet.analytics

import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/** A presentation-local timer shared by the loading worker and the UI drawing thread. */
internal class PaymentSheetFirstFrameTimer(
    private val nanoTime: () -> Long,
) {
    private val loadCompletedAt = AtomicReference<Long?>(null)

    fun onLoadComplete() {
        loadCompletedAt.set(nanoTime())
    }

    /** Consume the timing once, after drawing loaded content, before posting analytics work. */
    fun takeDuration(): Duration? {
        val startedAt = loadCompletedAt.getAndSet(null) ?: return null
        return (nanoTime() - startedAt).nanoseconds
    }
}
