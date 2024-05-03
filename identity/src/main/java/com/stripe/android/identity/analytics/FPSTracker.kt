package com.stripe.android.identity.analytics

import com.stripe.android.identity.injection.IdentityVerificationScope
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * Tracker for frames processed per second.
 */
@IdentityVerificationScope
internal class FPSTracker @Inject constructor(
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
) {
    private lateinit var startedAt: ComparableTimeMark
    private val frames: AtomicInteger = AtomicInteger(0)

    /**
     * Resets tracking status and starts tracking.
     */
    fun start() {
        startedAt = TimeSource.Monotonic.markNow()
    }

    /**
     * Increments when a new frame arrives.
     */
    fun trackFrame() {
        frames.incrementAndGet()
    }

    /**
     * Reports FPS since last start.
     */
    fun reportAndReset(type: String) {
        frames.get().let { totalFrames ->
            identityAnalyticsRequestFactory.averageFps(
                type = type,
                value = totalFrames.div(startedAt.elapsedNow().inWholeSeconds).toInt(),
                frames = totalFrames
            )
        }
        frames.set(0)
    }
}
