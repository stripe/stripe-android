package com.stripe.android.identity.states

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
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
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * [IdentityScanStateTransitioner] for IDDetector model, decides transition based on the
 * Intersection Over Union(IoU) score of bounding boxes.
 *
 * * The transitioner first checks if the ML output [Category] matches desired [ScanType], a mismatch
 * would not necessarily break the check, but will accumulate the [unmatchedFrame] streak,
 * see [outputMatchesTargetType] for details.
 * * Then it checks the IoU score, if the score is below threshold, then stays at Found state and
 * reset the [Found.reachedStateAt] timer.
 */
internal class IDDetectorTransitioner(
    private val timeout: Duration,
    private val iouThreshold: Float = DEFAULT_IOU_THRESHOLD,
    private val blurThreshold: Float = DEFAULT_BLUR_THRESHOLD,
    private val allowedUnmatchedFrames: Int = DEFAULT_ALLOWED_UNMATCHED_FRAME,
    private val displaySatisfiedDuration: Int = DEFAULT_DISPLAY_SATISFIED_DURATION,
    private val displayUnsatisfiedDuration: Int = DEFAULT_DISPLAY_UNSATISFIED_DURATION
) : IdentityScanStateTransitioner {
    private var previousBoundingBox: BoundingBox? = null
    private var unmatchedFrame = 0
    private val bestFrameDetector = BestFrameDetector()
    private var bestLegacyOutput: IDDetectorOutput.Legacy? = null

    @VisibleForTesting
    var timeoutAt: ComparableTimeMark = TimeSource.Monotonic.markNow() + timeout

    /**
     * Rest internal state and return itself.
     */
    @VisibleForTesting
    fun resetAndReturn(): IDDetectorTransitioner {
        previousBoundingBox = null
        unmatchedFrame = 0
        timeoutAt = TimeSource.Monotonic.markNow() + timeout
        bestFrameDetector.reset()
        bestLegacyOutput = null
        Log.d(TAG, "Reset! timeoutAt: $timeoutAt")
        return this
    }

    @Suppress("NestedBlockDepth", "LongMethod")
    override suspend fun transitionFromInitial(
        initialState: Initial,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is IDDetectorOutput) {
            "Unexpected output type: $analyzerOutput"
        }
        return when {
            timeoutAt.hasPassedNow() -> {
                IdentityScanState.TimeOut(initialState.type, this)
            }

            analyzerOutput.category.matchesScanType(initialState.type) -> {
                Log.d(
                    TAG,
                    "Matching model output detected with score ${analyzerOutput.resultScore}, " +
                        "transition to Found."
                )
                Found(
                    initialState.type,
                    this,
                    isFromLegacyDetector = analyzerOutput is IDDetectorOutput.Legacy
                )
            }

            else -> {
                Log.d(
                    TAG,
                    "Model outputs ${analyzerOutput.category}, which doesn't match with " +
                        "scanType ${initialState.type}, stay in Initial"
                )
                return if (analyzerOutput.category == Category.INVALID) {
                    // No document detected now: clear any prior wrong-side feedback
                    if (initialState.feedbackRes != null) initialState.withFeedback(null) else initialState
                } else {
                    // Detected a document but wrong side: show side-specific guidance while staying in Initial
                    val feedbackRes =
                        when (initialState.type) {
                            ScanType.DOC_FRONT -> {
                                if (analyzerOutput.category == Category.ID_BACK) {
                                    com.stripe.android.identity.R.string.stripe_front_of_id_not_detected
                                } else {
                                    null
                                }
                            }

                            ScanType.DOC_BACK -> {
                                if (analyzerOutput.category == Category.ID_FRONT ||
                                    analyzerOutput.category == Category.PASSPORT
                                ) {
                                    com.stripe.android.identity.R.string.stripe_back_of_id_not_detected
                                } else {
                                    null
                                }
                            }

                            ScanType.SELFIE -> {
                                null
                            }
                        }

                    if (feedbackRes != null) {
                        initialState.withFeedback(feedbackRes)
                    } else {
                        initialState
                    }
                }
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
        return when (analyzerOutput) {
            is IDDetectorOutput.Legacy -> {
                transitionFromFoundLegacy(
                    foundState,
                    analyzerInput,
                    analyzerOutput
                )
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "MagicNumber")
    private fun transitionFromFoundLegacy(
        foundState: Found,
        analyzerInput: AnalyzerInput,
        analyzerOutput: IDDetectorOutput.Legacy
    ): IdentityScanState {
        val nowTimestampMs = SystemClock.elapsedRealtime()

        return when {
            foundState.isFromLegacyDetector != true -> Unsatisfied(
                "Expecting Legacy IDDetectorOutput but received a Modern IDDetectorOutput",
                foundState.type,
                foundState.transitioner
            )

            timeoutAt.hasPassedNow() -> {
                Log.d(TAG, "Timeout reached during scanning")
                IdentityScanState.TimeOut(foundState.type, foundState.transitioner)
            }

            // Once we have a good frame, wait for the 1s best-frame window to complete and then finish,
            // even if subsequent frames are bad.
            bestFrameDetector.hasBestFrame() && bestFrameDetector.isWindowExpired(nowTimestampMs) -> {
                Satisfied(foundState.type, foundState.transitioner)
            }

            !outputMatchesTargetType(analyzerOutput.category, foundState.type) -> {
                if (bestFrameDetector.hasBestFrame()) {
                    // We already have a good candidate; keep waiting for window completion.
                    foundState
                } else {
                    Unsatisfied(
                        "Type ${analyzerOutput.category} doesn't match ${foundState.type}",
                        foundState.type,
                        foundState.transitioner
                    )
                }
            }

            isBlurry(analyzerOutput.blurScore) -> {
                // reset timer of the foundState and show blur feedback
                foundState.reachedStateAt = TimeSource.Monotonic.markNow()
                val feedbackThreshold = if (blurThreshold > 0f) blurThreshold * BLUR_FEEDBACK_RATIO else 0f
                if (analyzerOutput.blurScore <= feedbackThreshold) {
                    foundState.withFeedback(
                        com.stripe.android.identity.R.string.stripe_reduce_blur_2
                    )
                } else {
                    // Clear blur feedback when it's only mildly blurry
                    foundState.withFeedback(null)
                }
            }

            // Center gating: if the detected document is not centered, keep in Found and reset timer
            !run {
                val centerX = analyzerOutput.boundingBox.left + analyzerOutput.boundingBox.width / 2f
                val centerY = analyzerOutput.boundingBox.top + analyzerOutput.boundingBox.height / 2f
                centerX in (0.5f - CENTER_TOLERANCE)..(0.5f + CENTER_TOLERANCE) &&
                    centerY in (0.5f - CENTER_TOLERANCE)..(0.5f + CENTER_TOLERANCE)
            } -> {
                foundState.reachedStateAt = TimeSource.Monotonic.markNow()
                foundState.withFeedback(
                    com.stripe.android.identity.R.string.stripe_move_id_to_center
                )
            }

            // Distance gating: if the detected document is too small or too large, keep in Found and reset timer
            tooSmall(analyzerOutput.boundingBox) -> {
                foundState.reachedStateAt = TimeSource.Monotonic.markNow()
                foundState.withFeedback(
                    com.stripe.android.identity.R.string.stripe_move_closer
                )
            }
            tooLarge(analyzerOutput.boundingBox) -> {
                foundState.reachedStateAt = TimeSource.Monotonic.markNow()
                foundState.withFeedback(
                    com.stripe.android.identity.R.string.stripe_move_farther
                )
            }

            !iOUCheckPass(analyzerOutput.boundingBox) -> {
                // reset timer of the foundState
                foundState.reachedStateAt = TimeSource.Monotonic.markNow()
                foundState
            }

            else -> {
                // Only record frames whose category strictly matches the target.
                // (outputMatchesTargetType can tolerate brief mismatches, but those should not become "best".)
                if (!analyzerOutput.category.matchesScanType(foundState.type)) {
                    return foundState
                }

                // Frame passed all checks, add it to best frame detector.
                // The detector will start a fixed 1s window from the first accepted frame.
                val frameBitmap = runCatching {
                    analyzerInput.cameraPreviewImage.image
                }.getOrNull() ?: analyzerOutput.croppedImage

                val updatedBest = bestFrameDetector.addFrame(
                    bitmap = frameBitmap,
                    blurScore = analyzerOutput.blurScore,
                    confidenceScore = analyzerOutput.resultScore,
                    timestamp = nowTimestampMs
                )
                if (updatedBest) {
                    bestLegacyOutput = analyzerOutput
                }

                foundState
            }
        }
    }

    override suspend fun transitionFromSatisfied(
        satisfiedState: Satisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return if (satisfiedState.reachedStateAt.elapsedNow() > displaySatisfiedDuration.milliseconds) {
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
            timeoutAt.hasPassedNow() -> {
                IdentityScanState.TimeOut(unsatisfiedState.type, this)
            }

            unsatisfiedState.reachedStateAt.elapsedNow() > displayUnsatisfiedDuration.milliseconds -> {
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

    /**
     * Returns the best frame's bitmap, or null if no frames were captured.
     */
    fun getBestFrameBitmap(): Bitmap? {
        return bestFrameDetector.getBestFrameBitmap()
    }

    fun getBestLegacyOutput(): IDDetectorOutput.Legacy? {
        return bestLegacyOutput
    }

    private fun coverage(box: BoundingBox): Float {
        // area fraction relative to the frame
        val w = box.width.coerceIn(0f, 1f)
        val h = box.height.coerceIn(0f, 1f)
        return w * h
    }

    private fun tooSmall(box: BoundingBox): Boolean = coverage(box) < MIN_BOX_COVERAGE_FEEDBACK
    private fun tooLarge(box: BoundingBox): Boolean = coverage(box) > MAX_BOX_COVERAGE_FEEDBACK

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
        const val DEFAULT_IOU_THRESHOLD = 0.95f
        const val DEFAULT_ALLOWED_UNMATCHED_FRAME = 1
        const val DEFAULT_DISPLAY_SATISFIED_DURATION = 0
        const val DEFAULT_DISPLAY_UNSATISFIED_DURATION = 0
        const val DEFAULT_BLUR_THRESHOLD = 0f

        // Only show blur feedback when blur is worse than this fraction of the gating threshold
        const val BLUR_FEEDBACK_RATIO = 0.85f

        // Distance feedback thresholds based on area coverage of the frame
        const val MIN_BOX_COVERAGE_FEEDBACK = 0.18f
        const val MAX_BOX_COVERAGE_FEEDBACK = 0.78f

        // Center tolerance for checking if document is centered
        const val CENTER_TOLERANCE = 0.05f

        val TAG: String = IDDetectorTransitioner::class.java.simpleName
    }

    /**
     * Checks if [Category] matches [IdentityScanState].
     * Note: the ML model will output ID_FRONT or ID_BACK for both ID and Driver License.
     */
    private fun Category.matchesScanType(scanType: ScanType): Boolean {
        return this == Category.ID_BACK && scanType == ScanType.DOC_BACK ||
            this == Category.ID_FRONT && scanType == ScanType.DOC_FRONT ||
            this == Category.PASSPORT && scanType == ScanType.DOC_FRONT
    }
}
