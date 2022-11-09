package com.stripe.android.identity.analytics

import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.identity.injection.IdentityVerificationScope
import com.stripe.android.identity.networking.IdentityRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Tracker for frames processed per second.
 */
@IdentityVerificationScope
internal class FPSTracker @Inject constructor(
    private val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory,
    private val identityRepository: IdentityRepository
) {
    private lateinit var startedAt: ClockMark
    private val frames: AtomicInteger = AtomicInteger(0)

    /**
     * Resets tracking status and starts tracking.
     */
    fun start() {
        startedAt = Clock.markNow()
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
    suspend fun reportAndReset(type: String) {
        frames.get().let { totalFrames ->
            identityRepository.sendAnalyticsRequest(
                identityAnalyticsRequestFactory.averageFps(
                    type = type,
                    value = totalFrames.div(startedAt.elapsedSince().inSeconds).toInt(),
                    frames = totalFrames
                )
            )
        }
        frames.set(0)
    }
}
