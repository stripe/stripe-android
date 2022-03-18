package com.stripe.android.identity.states

import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied
import kotlin.math.max
import kotlin.math.min

/**
 * Decide transition based on the Intersection Over Union(IoU) score of bounding boxes.
 *
 * If over [hitsRequired] consecutive hits with IoU over [iouThreshold], then transitions to [Satisfied].
 * Otherwise transition to [Unsatisfied].
 */
internal class IOUTransitioner(
    private val hitsRequired: Int = DEFAULT_HITS_REQUIRED,
    private val iouThreshold: Float = DEFAULT_IOU_THRESHOLD
) : IdentityFoundStateTransitioner {
    private var previousBoundingBox: BoundingBox? = null
    private var consecutiveHitCount = 0

    override fun transition(
        foundState: IdentityScanState.Found,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return when {
            !analyzerOutput.category.matchesScanType(foundState.type) -> Unsatisfied(
                "Type ${analyzerOutput.category} doesn't match ${foundState.type}",
                foundState.type
            )
            !iOUCheckPass(analyzerOutput.boundingBox) -> Unsatisfied(
                "IoU below threshold",
                foundState.type
            )
            moreResultsRequired() -> foundState
            else -> {
                Satisfied(foundState.type)
            }
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
            iou >= iouThreshold
        } ?: run {
            previousBoundingBox = newBoundingBox
            true
        }
    }

    private fun moreResultsRequired(): Boolean {
        return (consecutiveHitCount++) < hitsRequired
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
        const val DEFAULT_HITS_REQUIRED = 5
        const val DEFAULT_IOU_THRESHOLD = 0.95f
    }
}
