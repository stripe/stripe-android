package com.stripe.android.camera.framework.util

import android.util.Log
import com.stripe.android.camera.BuildConfig
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.Duration
import com.stripe.android.camera.framework.time.Rate
import com.stripe.android.camera.framework.time.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

internal interface FrameRateListener {
    fun onFrameRateUpdate(overallRate: Rate, instantRate: Rate)
}

/**
 * A class that tracks the rate at which frames are processed. This is useful for debugging to
 * determine how quickly a device is handling data.
 */
internal class FrameRateTracker(
    private val name: String,
    private val listener: FrameRateListener? = null,
    private val notifyInterval: Duration = 1.seconds
) {
    private var firstFrameTime: ClockMark? = null
    private var lastNotifyTime: ClockMark = Clock.markNow()

    // This is -1 so that we do not calculate a rate for the first frame
    private val totalFramesProcessed: AtomicLong = AtomicLong(-1)
    private val framesProcessedSinceLastUpdate: AtomicLong = AtomicLong(0)

    private val frameRateMutex = Mutex()

    /**
     * Calculate the current rate at which frames are being processed. If the notify interval has
     * elapsed, notify the listener of the current rate.
     */
    suspend fun trackFrameProcessed() {
        val totalFrames = totalFramesProcessed.incrementAndGet()
        val framesSinceLastUpdate = framesProcessedSinceLastUpdate.incrementAndGet()

        val lastNotifyTime = this.lastNotifyTime
        val shouldNotifyOfFrameRate = totalFrames > 0 && frameRateMutex.withLock {
            val shouldNotify = lastNotifyTime.elapsedSince() > notifyInterval
            if (shouldNotify) {
                this.lastNotifyTime = Clock.markNow()
            }
            shouldNotify
        }

        val firstFrameTime = this.firstFrameTime ?: Clock.markNow()
        this.firstFrameTime = firstFrameTime

        if (shouldNotifyOfFrameRate) {
            val overallFrameRate = Rate(totalFrames, firstFrameTime.elapsedSince())
            val instantFrameRate = Rate(framesSinceLastUpdate, lastNotifyTime.elapsedSince())

            logProcessingRate(overallFrameRate, instantFrameRate)
            listener?.onFrameRateUpdate(overallFrameRate, instantFrameRate)
            framesProcessedSinceLastUpdate.set(0)
        }
    }

    /**
     * Reset the state of the frame rate tracker.
     */
    fun reset() {
        firstFrameTime = null
        lastNotifyTime = Clock.markNow()
        totalFramesProcessed.set(0)
        framesProcessedSinceLastUpdate.set(0)
    }

    /**
     * Get the average frame rate for this device
     */
    fun getAverageFrameRate() = Rate(
        amount = totalFramesProcessed.get(),
        duration = firstFrameTime?.elapsedSince() ?: Duration.ZERO
    )

    /**
     * The processing rate has been updated. This is useful for debugging and measuring performance.
     *
     * @param overallRate: The total frame rate at which the analyzer is running
     * @param instantRate: The instantaneous frame rate at which the analyzer is running
     */
    private fun logProcessingRate(overallRate: Rate, instantRate: Rate) {
        val overallFps = if (overallRate.duration != Duration.ZERO) {
            overallRate.amount / overallRate.duration.inSeconds
        } else {
            0.0
        }

        val instantFps = if (instantRate.duration != Duration.ZERO) {
            instantRate.amount / instantRate.duration.inSeconds
        } else {
            0.0
        }

        if (BuildConfig.DEBUG) {
            Log.d(logTag, "$name processing avg=$overallFps, inst=$instantFps")
        }
    }

    private companion object {
        val logTag = FrameRateTracker::class.java.simpleName
    }
}
