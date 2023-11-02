package com.stripe.android.identity.states

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.Duration
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Initial
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.ScanType
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied
import kotlin.math.max
import kotlin.math.min

/**
 * [IdentityScanStateTransitioner] for IDDetector model, decides transition based on the
 * Intersection Over Union(IoU) score of bounding boxes.
 *
 * * The transitioner first checks if the ML output [Category] matches desired [ScanType], a mismatch
 * would not necessarily break the check, but will accumulate the [unmatchedFrame] streak,
 * see [outputMatchesTargetType] for details.
 * * Then it checks the IoU score, if the score is below threshold, then stays at Found state and
 * reset the [Found.reachedStateAt] timer.
 * * Finally it checks since the time elapsed since [Found] is reached, if it passed
 * [timeRequired], then transitions to [Satisfied], otherwise stays in [Found].
 */
internal class IDDetectorTransitioner(
    private val timeout: Duration,
    private val iouThreshold: Float = DEFAULT_IOU_THRESHOLD,
    private val timeRequired: Int = DEFAULT_TIME_REQUIRED,
    private val blurThreshold: Float = DEFAULT_BLUR_THRESHOLD,
    private val allowedUnmatchedFrames: Int = DEFAULT_ALLOWED_UNMATCHED_FRAME,
    private val displaySatisfiedDuration: Int = DEFAULT_DISPLAY_SATISFIED_DURATION,
    private val displayUnsatisfiedDuration: Int = DEFAULT_DISPLAY_UNSATISFIED_DURATION
) : IdentityScanStateTransitioner {
    private var previousBoundingBox: BoundingBox? = null
    private var unmatchedFrame = 0

    @VisibleForTesting
    var timeoutAt: ClockMark = Clock.markNow() + timeout

    /**
     * Rest internal state and return itself.
     */
    @VisibleForTesting
    fun resetAndReturn(): IDDetectorTransitioner {
        previousBoundingBox = null
        unmatchedFrame = 0
        timeoutAt = Clock.markNow() + timeout
        Log.d(TAG, "Reset! timeoutAt: $timeoutAt")
        return this
    }

    override suspend fun transitionFromInitial(
        initialState: Initial,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is IDDetectorOutput) {
            "Unexpected output type: $analyzerOutput"
        }
        return when {
            timeoutAt.hasPassed() -> {
                IdentityScanState.TimeOut(initialState.type, this)
            }

            analyzerOutput.category.matchesScanType(initialState.type) -> {
                Log.d(
                    TAG,
                    "Matching model output detected with score ${analyzerOutput.resultScore}, " +
                        "transition to Found."
                )
                Found(initialState.type, this)
            }

            else -> {
                Log.d(
                    TAG,
                    "Model outputs ${analyzerOutput.category}, which doesn't match with " +
                        "scanType ${initialState.type}, stay in Initial"
                )
                initialState
            }
        }
    }

    override suspend fun transitionFromFound(
        foundState: Found,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is IDDetectorOutput) {
            "Unexpected output type: $analyzerOutput"
        }
        return when {
            timeoutAt.hasPassed() -> {
                IdentityScanState.TimeOut(foundState.type, foundState.transitioner)
            }

            !outputMatchesTargetType(analyzerOutput.category, foundState.type) -> Unsatisfied(
                "Type ${analyzerOutput.category} doesn't match ${foundState.type}",
                foundState.type,
                foundState.transitioner
            )

            !iOUCheckPass(analyzerOutput.boundingBox) -> {
                // reset timer of the foundState
                foundState.reachedStateAt = Clock.markNow()
                foundState
            }

            isBlurry(analyzerOutput.blurScore) -> {
                // reset timer of the foundState
                foundState.reachedStateAt = Clock.markNow()
                foundState
            }

            moreResultsRequired(foundState) -> foundState
            else -> {
                Satisfied(foundState.type, foundState.transitioner)
            }
        }
    }

    override suspend fun transitionFromSatisfied(
        satisfiedState: Satisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return if (satisfiedState.reachedStateAt.elapsedSince() > displaySatisfiedDuration.milliseconds) {
            Log.d(TAG, "Scan for ${satisfiedState.type} Satisfied, transition to Finished.")
            IdentityScanState.Finished(satisfiedState.type, this)
        } else {
            satisfiedState
        }
    }

    override suspend fun transitionFromUnsatisfied(
        unsatisfiedState: Unsatisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return when {
            timeoutAt.hasPassed() -> {
                IdentityScanState.TimeOut(unsatisfiedState.type, this)
            }

            unsatisfiedState.reachedStateAt.elapsedSince() > displayUnsatisfiedDuration.milliseconds -> {
                Log.d(
                    TAG,
                    "Scan for ${unsatisfiedState.type} Unsatisfied with reason " +
                        "${unsatisfiedState.reason}, transition to Initial."
                )
                Initial(unsatisfiedState.type, this.resetAndReturn())
            }

            else -> {
                unsatisfiedState
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

    /**
     * Decide if the image is blurry or not
     */
    private fun isBlurry(blurScore: Float): Boolean {
        return blurScore <= blurThreshold
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

    internal companion object {
        const val DEFAULT_TIME_REQUIRED = 500
        const val DEFAULT_IOU_THRESHOLD = 0.95f
        const val DEFAULT_ALLOWED_UNMATCHED_FRAME = 1
        const val DEFAULT_DISPLAY_SATISFIED_DURATION = 0
        const val DEFAULT_DISPLAY_UNSATISFIED_DURATION = 0
        const val DEFAULT_BLUR_THRESHOLD = 0f
        val TAG: String = IDDetectorTransitioner::class.java.simpleName
    }

    /**
     * Checks if [Category] matches [IdentityScanState].
     * Note: the ML model will output ID_FRONT or ID_BACK for both ID and Driver License.
     */
    private fun Category.matchesScanType(scanType: ScanType): Boolean {
        return this == Category.ID_BACK && scanType == ScanType.DOC_BACK ||
            this == Category.ID_FRONT && scanType == ScanType.DOC_FRONT ||
            this == Category.ID_BACK && scanType == ScanType.DOC_BACK ||
            this == Category.ID_FRONT && scanType == ScanType.DOC_FRONT ||
            this == Category.PASSPORT && scanType == ScanType.DOC_FRONT
    }
}
