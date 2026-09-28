package com.stripe.android.identity.states

import com.stripe.android.identity.ml.BoundingBox
import kotlin.math.max
import kotlin.math.min

/**
 * Determines whether the subject is likely motion-blurred by checking bounding box stability.
 *
 * This mirrors the iOS Identity selfie capture motion blur gate, using an IoU threshold over a
 * minimum duration before returning a non-null result.
 */
internal class MotionBlurDetector(
    private val minIou: Float,
    private val minDurationMs: Long,
) {
    internal data class Output(
        val hasMotionBlur: Boolean,
        val iou: Float,
    )

    private var windowStartTimestampMs: Long? = null
    private var previousBoundingBox: BoundingBox? = null

    fun reset() {
        windowStartTimestampMs = null
        previousBoundingBox = null
    }

    /**
     * Returns null until the detector has been running for [minDurationMs].
     */
    fun determineMotionBlur(bounds: BoundingBox, nowTimestampMs: Long): Output? {
        val start = windowStartTimestampMs ?: run {
            windowStartTimestampMs = nowTimestampMs
            previousBoundingBox = bounds
            return null
        }

        val previous = previousBoundingBox
        previousBoundingBox = bounds
        if (previous == null) {
            return null
        }

        if (nowTimestampMs - start < minDurationMs) {
            return null
        }

        val iou = intersectionOverUnion(bounds, previous)
        return Output(
            hasMotionBlur = iou < minIou,
            iou = iou,
        )
    }

    /**
     * Calculate IoU of two boxes.
     */
    private fun intersectionOverUnion(boxA: BoundingBox, boxB: BoundingBox): Float {
        val aLeft = boxA.left
        val aRight = boxA.left + boxA.width
        val aTop = boxA.top
        val aBottom = boxA.top + boxA.height

        val bLeft = boxB.left
        val bRight = boxB.left + boxB.width
        val bTop = boxB.top
        val bBottom = boxB.top + boxB.height

        val xA = max(aLeft, bLeft)
        val yA = max(aTop, bTop)
        val xB = min(aRight, bRight)
        val yB = min(aBottom, bBottom)

        val interW = (xB - xA).coerceAtLeast(0f)
        val interH = (yB - yA).coerceAtLeast(0f)
        val interArea = interW * interH

        val boxAArea = (aRight - aLeft).coerceAtLeast(0f) * (aBottom - aTop).coerceAtLeast(0f)
        val boxBArea = (bRight - bLeft).coerceAtLeast(0f) * (bBottom - bTop).coerceAtLeast(0f)

        val denom = boxAArea + boxBArea - interArea
        return if (denom > 0f) interArea / denom else 0f
    }
}
