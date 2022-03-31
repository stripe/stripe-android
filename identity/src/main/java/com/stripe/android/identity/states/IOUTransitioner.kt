package com.stripe.android.identity.states

import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.ScanType
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied
import kotlin.math.max
import kotlin.math.min

/**
 * Decide transition based on the Intersection Over Union(IoU) score of bounding boxes.
 *
 * * The transitioner first checks if the ML output [Category] matches desired [ScanType], a mismatch
 * would not necessarily break the check, but will accumulate the [unmatchedFrame] streak,
 * see [outputMatchesTargetType] for details.
 * * Then it checks the IoU score, if the score is below threshold, then stays at Found state and
 * reset the [Found.reachedStateAt] timer.
 * * Finally it checks since the time elapsed since [Found] is reached, if it passed
 * [timeRequired], then transitions to [Satisfied], otherwise stays in [Found].
 */
internal class IOUTransitioner(
    private val iouThreshold: Float = DEFAULT_IOU_THRESHOLD,
    private val timeRequired: Int = DEFAULT_TIME_REQUIRED,
    private val allowedUnmatchedFrames: Int = DEFAULT_ALLOWED_UNMATCHED_FRAME
) : IdentityFoundStateTransitioner {
    private var previousBoundingBox: BoundingBox? = null
    private var unmatchedFrame = 0

    override fun transition(
        foundState: Found,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return when {
            !outputMatchesTargetType(analyzerOutput.category, foundState.type) -> Unsatisfied(
                "Type ${analyzerOutput.category} doesn't match ${foundState.type}",
                foundState.type,
                foundState.timeoutAt,
                foundState.transitioner
            )
            !iOUCheckPass(analyzerOutput.boundingBox) -> {
                // reset timer of the foundState
                foundState.reachedStateAt = Clock.markNow()
                foundState
            }
            moreResultsRequired(foundState) -> foundState
            else -> {
                Satisfied(foundState.type, foundState.timeoutAt, foundState.transitioner)
            }
        }
    }

    /**
     * Compare the ML output [Category] and the target [ScanType].
     *
     * If it's a match, then return true and reset the [unmatchedFrame] streak count.
     * If it's a unmatch, increase [unmatchedFrame] by one,
     * * If it's still within [allowedUnmatchedFrames], return true
     * * Otherwise return false
     *
     */
    private fun outputMatchesTargetType(
        outputCategory: Category,
        targetScanType: ScanType
    ): Boolean {
        return if (outputCategory.matchesScanType(targetScanType)) {
            // if it's a match, clear the unmatched frame streak count
            unmatchedFrame = 0
            true
        } else {
            // if it's an unmatch, check if it's still within allowedUnmatchedFrames
            unmatchedFrame++
            unmatchedFrame <= allowedUnmatchedFrames
        }
    }

    /**
     * Calculate the IoU between new box and old box.
     *
     * returns true if previous box is null or the result is above the threshold,
     * returns false otherwise.
     */
    private fun iOUCheckPass(newBoundingBox: BoundingBox): Boolean {
        return previousBoundingBox?.let { previousBox ->
            val iou = intersectionOverUnion(newBoundingBox, previousBox)
            previousBoundingBox = newBoundingBox
            return iou >= iouThreshold
        } ?: run {
            previousBoundingBox = newBoundingBox
            true
        }
    }

    private fun moreResultsRequired(foundState: Found): Boolean {
        return foundState.reachedStateAt.elapsedSince() < timeRequired.milliseconds
    }

    /**
     * Calculate IoU of two boxes, see https://stackoverflow.com/a/41660682/802372
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

        // determine the (x, y)-coordinates of the intersection rectangle
        val xA = max(aLeft, bLeft)
        val yA = max(aTop, bTop)
        val xB = min(aRight, bRight)
        val yB = min(aBottom, bBottom)

        // compute the area of intersection rectangle
        val interArea = (xB - xA) * (yB - yA)

        // compute the area of both the prediction and ground-truth
        // rectangles
        val boxAArea = (aRight - aLeft) * (aBottom - aTop)
        val boxBArea = (bRight - bLeft) * (bBottom - bTop)

        // compute the intersection over union by taking the intersection
        // area and dividing it by the sum of prediction + ground-truth
        // areas - the intersection area
        return interArea / (boxAArea + boxBArea - interArea)
    }

    private companion object {
        const val DEFAULT_TIME_REQUIRED = 500
        const val DEFAULT_IOU_THRESHOLD = 0.95f
        const val DEFAULT_ALLOWED_UNMATCHED_FRAME = 1
    }
}
