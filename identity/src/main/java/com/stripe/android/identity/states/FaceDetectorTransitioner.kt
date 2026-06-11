package com.stripe.android.identity.states

import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.camera.framework.util.FrameSaver
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.IdentityScanState.Finished
import com.stripe.android.identity.states.IdentityScanState.Found
import com.stripe.android.identity.states.IdentityScanState.Initial
import com.stripe.android.identity.states.IdentityScanState.Satisfied
import com.stripe.android.identity.states.IdentityScanState.Unsatisfied
import com.stripe.android.identity.utils.roundToMaxDecimals
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * [IdentityScanStateTransitioner] for FaceDetector model.
 *
 * To transition from [Initial] state -
 * * Check if it's timeout since the start of the scan.
 * * If waiting to capture a side pose, keep the instruction visible before saving frames.
 * * Check if a valid face is present, see [isFaceValid] for details. Save the frame and transition to Found if so.
 * * Otherwise stay in [Initial]
 *
 * To transition from [Found] state -
 * * Check if it's timeout since the start of the scan.
 * * Wait for an interval between two Found state, if the interval is not reached, keep waiting.
 * * Check if a valid face is present, save the frame and check if enough frames have been collected
 *  * If so, transition to [Satisfied]
 *  * Otherwise check how long it's been since the last transition to [Found]
 *  *   If it's within [stayInFoundDuration], stay in [Found]
 *  *   Otherwise transition to [Unsatisfied]
 *
 * To transition from [Satisfied] state -
 * * Move to the next selfie pose, or transition to [Finished] after all poses have been captured.
 *
 * To transition from [Unsatisfied] state -
 * * Directly transitions to [Initial]
 */
internal class FaceDetectorTransitioner(
    private val selfieCapturePage: VerificationPageStaticContentSelfieCapturePage,
    internal val selfieFrameSaver: SelfieFrameSaver = SelfieFrameSaver(),
    private val stayInFoundDuration: Int = DEFAULT_STAY_IN_FOUND_DURATION,
    private val sideCapturePromptDuration: Int = DEFAULT_SIDE_CAPTURE_PROMPT_DURATION
) : IdentityScanStateTransitioner {
    @VisibleForTesting
    var timeoutAt: ComparableTimeMark =
        TimeSource.Monotonic.markNow() + selfieCapturePage.autoCaptureTimeout.milliseconds

    private val motionBlurDetector = MotionBlurDetector(
        minIou = selfieCapturePage.models.faceDetectorIou,
        minDurationMs = DEFAULT_MOTION_BLUR_MIN_DURATION_MS,
    )

    @VisibleForTesting
    internal var activeCapture = Capture.FRONT
        private set

    @VisibleForTesting
    internal var completedCapture: Capture? = null
        private set

    private var captureStarted = false
    private var activeCaptureStartedAt: ComparableTimeMark = TimeSource.Monotonic.markNow()
    private var sideCapturePromptCompleted = true

    internal val isWaitingForSideCapturePrompt: Boolean
        get() = shouldWaitForSideCapturePrompt()

    @VisibleForTesting
    fun resetAndReturn(): FaceDetectorTransitioner {
        timeoutAt = TimeSource.Monotonic.markNow() + selfieCapturePage.autoCaptureTimeout.milliseconds
        motionBlurDetector.reset()
        activeCapture = Capture.FRONT
        completedCapture = null
        captureStarted = false
        activeCaptureStartedAt = TimeSource.Monotonic.markNow()
        sideCapturePromptCompleted = true
        return this
    }

    internal data class SelfieFrame(
        val input: AnalyzerInput,
        val output: FaceDetectorOutput,
        val bestFrameScore: Float,
        val capture: Capture = Capture.FRONT,
    )

    internal val filteredFrames: List<Pair<AnalyzerInput, FaceDetectorOutput>>
        get() {
            val savedFrames = requireNotNull(selfieFrameSaver.getSavedFrames()[Capture.FRONT.frameIdentifier]) {
                "No frames saved"
            }
            require(savedFrames.size >= NUM_FILTERED_FRAMES) {
                "Not enough frames saved, frames saved: ${savedFrames.size}"
            }

            // Return the first, the best (based on bestFrameScore), and the last frame collected.
            val firstFrame = savedFrames.last
            val lastFrame = savedFrames.first
            val bestFrame = requireNotNull(
                savedFrames.subList(1, savedFrames.size - 1)
                    .maxByOrNull { it.bestFrameScore }
            ) { "Couldn't find best frame" }

            return listOf(
                firstFrame.input to firstFrame.output,
                bestFrame.input to bestFrame.output,
                lastFrame.input to lastFrame.output,
            )
        }

    internal val numFrames = selfieCapturePage.numSamples

    internal val bestFaceScore: Float
        get() {
            return filteredFrames[INDEX_BEST].second.resultScore
        }

    internal fun frameForSelfie(selfie: Selfie): Pair<AnalyzerInput, FaceDetectorOutput> {
        return when (selfie) {
            Selfie.FIRST,
            Selfie.BEST,
            Selfie.LAST -> filteredFrames[selfie.index]
            Selfie.LEFT,
            Selfie.RIGHT -> sideFrame(selfie.capture)
        }
    }

    private fun sideFrame(capture: Capture): Pair<AnalyzerInput, FaceDetectorOutput> {
        require(capture == Capture.LEFT || capture == Capture.RIGHT) {
            "Expected a side capture, got $capture"
        }
        val savedFrames = requireNotNull(selfieFrameSaver.getSavedFrames()[capture.frameIdentifier]) {
            "No frames saved for $capture"
        }
        require(savedFrames.isNotEmpty()) {
            "No frames saved for $capture"
        }
        val bestFrame = requireNotNull(savedFrames.maxByOrNull { it.bestFrameScore }) {
            "Couldn't find best frame for $capture"
        }
        return bestFrame.input to bestFrame.output
    }

    internal val scoreVariance: Float
        get() {
            val savedFrames = requireNotNull(selfieFrameSaver.getSavedFrames()[Capture.FRONT.frameIdentifier]) {
                "No frames saved"
            }
            require(savedFrames.size == numFrames) {
                "Not enough frames saved, score variance not calculated"
            }
            val mean =
                savedFrames.fold(0f) { acc, frame ->
                    acc + frame.output.resultScore
                }.div(numFrames.toFloat())

            return sqrt(
                savedFrames.fold(0f) { acc, frame ->
                    acc + (frame.output.resultScore - mean).pow(2)
                }.div(numFrames.toFloat())
            ).roundToMaxDecimals(2)
        }

    internal class SelfieFrameSaver :
        FrameSaver<String, SelfieFrame, FaceDetectorOutput>() {
        // Don't limit max number of saved frames, let the transitioner decide when to stop saving
        // new frames.
        override fun getMaxSavedFrames(savedFrameIdentifier: String) = Int.MAX_VALUE

        override fun getSaveFrameIdentifier(
            frame: SelfieFrame,
            metaData: FaceDetectorOutput
        ) = frame.capture.frameIdentifier

        fun selfieCollected(): Int = framesCollected(Capture.FRONT)

        fun framesCollected(capture: Capture): Int =
            getSavedFrames()[capture.frameIdentifier]?.size ?: 0
    }

    override suspend fun transitionFromInitial(
        initialState: Initial,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is FaceDetectorOutput) {
            "Unexpected output type: $analyzerOutput"
        }
        if (!captureStarted) {
            selfieFrameSaver.reset()
            captureStarted = true
        }

        if (timeoutAt.hasPassedNow()) {
            Log.d(TAG, "Timeout in Initial state: $initialState")
            return IdentityScanState.TimeOut(initialState.type, this)
        }
        if (shouldWaitForSideCapturePrompt()) {
            Log.d(TAG, "Showing $activeCapture instruction prompt, stay in Initial")
            return initialState
        }

        val shouldRefreshInitialAfterSidePrompt = consumeSideCapturePromptCompletion()
        val nowTimestampMs = SystemClock.elapsedRealtime()
        val motionBlurResult = determineMotionBlurResult(analyzerOutput, nowTimestampMs)

        return when {
            isFrameValidForActiveCapture(analyzerOutput, motionBlurResult) -> {
                Log.d(TAG, "Valid face found, transition to Found")
                saveFrame(
                    analyzerInput = analyzerInput,
                    analyzerOutput = analyzerOutput,
                    motionBlurResult = motionBlurResult
                )
                if (isActiveCaptureCollected()) {
                    completedCapture = activeCapture
                    Satisfied(initialState.type, this)
                } else {
                    Found(initialState.type, this)
                }
            }

            else -> {
                Log.d(TAG, "Valid face not found, stay in Initial")
                if (shouldRefreshInitialAfterSidePrompt) {
                    Initial(initialState.type, this)
                } else {
                    initialState
                }
            }
        }
    }

    @Suppress("LongMethod")
    override suspend fun transitionFromFound(
        foundState: Found,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is FaceDetectorOutput) { "Unexpected output type: $analyzerOutput" }

        val nowTimestampMs = SystemClock.elapsedRealtime()
        val motionBlurResult = determineMotionBlurResult(analyzerOutput, nowTimestampMs)

        return when {
            timeoutAt.hasPassedNow() -> {
                Log.d(TAG, "Timeout in Found state: $foundState")
                IdentityScanState.TimeOut(foundState.type, this)
            }

            foundState.reachedStateAt.elapsedNow() < selfieCapturePage.sampleInterval.milliseconds -> {
                Log.d(
                    TAG,
                    "Get a selfie before selfie capture interval, ignored. " +
                        "Current selfieCollected: ${selfieFrameSaver.selfieCollected()}"
                )
                foundState
            }

            isFrameValidForActiveCapture(analyzerOutput, motionBlurResult) -> {
                saveFrame(
                    analyzerInput = analyzerInput,
                    analyzerOutput = analyzerOutput,
                    motionBlurResult = motionBlurResult
                )
                if (isActiveCaptureCollected()) {
                    completedCapture = activeCapture
                    Log.d(
                        TAG,
                        "A valid selfie captured for $activeCapture, transitions to Satisfied"
                    )
                    Satisfied(foundState.type, this)
                } else {
                    Log.d(
                        TAG,
                        "A valid selfie captured for $activeCapture, need " +
                            "${requiredFramesForActiveCapture()} frames but has " +
                            "${activeCaptureCollected()}, stays in Found"
                    )
                    Found(foundState.type, this)
                }
            }

            foundState.reachedStateAt.elapsedNow() < stayInFoundDuration.milliseconds -> {
                Log.d(
                    TAG,
                    "Get an invalid selfie in Found state, but not enough time " +
                        "passed(${foundState.reachedStateAt.elapsedNow()}), stays in Found. " +
                        "Current selfieCollected: ${selfieFrameSaver.selfieCollected()}"
                )
                foundState
            }

            else -> {
                Log.d(
                    TAG,
                    "Didn't get a valid selfie in Found state after $stayInFoundDuration " +
                        "milliseconds, transition to Unsatisfied"
                )
                return Unsatisfied(
                    "Didn't get a valid selfie in Found state after " +
                        "$stayInFoundDuration milliseconds",
                    foundState.type,
                    foundState.transitioner
                )
            }
        }
    }

    override suspend fun transitionFromSatisfied(
        satisfiedState: Satisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        val nextCapture = activeCapture.next()
        return if (nextCapture == null) {
            Finished(satisfiedState.type, this)
        } else {
            activeCapture = nextCapture
            activeCaptureStartedAt = TimeSource.Monotonic.markNow()
            completedCapture = null
            sideCapturePromptCompleted = false
            motionBlurDetector.reset()
            Initial(
                type = satisfiedState.type,
                transitioner = this
            )
        }
    }

    override suspend fun transitionFromUnsatisfied(
        unsatisfiedState: Unsatisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return Initial(unsatisfiedState.type, this.resetAndReturn())
    }

    private fun determineMotionBlurResult(
        analyzerOutput: FaceDetectorOutput,
        nowTimestampMs: Long,
    ): MotionBlurDetector.Output? {
        // Avoid feeding noisy bounding boxes to the detector when the face isn't confidently detected.
        return if (isFaceScoreOverThreshold(analyzerOutput.resultScore)) {
            motionBlurDetector.determineMotionBlur(analyzerOutput.boundingBox, nowTimestampMs)
        } else {
            null
        }
    }

    private fun isFaceValid(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ) =
        isFaceCentered(analyzerOutput.boundingBox) &&
            isFaceAwayFromEdges(analyzerOutput.boundingBox) &&
            isFaceCoverageOK(analyzerOutput.boundingBox) &&
            isFaceScoreOverThreshold(analyzerOutput.resultScore) &&
            // Match iOS: treat frames as invalid only when motion blur is explicitly detected.
            motionBlurResult?.hasMotionBlur != true

    private fun isFrameValidForActiveCapture(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ): Boolean {
        return isFaceValid(analyzerOutput, motionBlurResult) &&
            isPoseValidForActiveCapture(analyzerOutput)
    }

    private fun isPoseValidForActiveCapture(analyzerOutput: FaceDetectorOutput): Boolean {
        val pose = analyzerOutput.pose ?: return true
        return when (activeCapture) {
            Capture.FRONT -> true
            Capture.LEFT -> pose.yaw < -SIDE_CAPTURE_YAW_THRESHOLD
            Capture.RIGHT -> pose.yaw > SIDE_CAPTURE_YAW_THRESHOLD
        }
    }

    private fun shouldWaitForSideCapturePrompt(): Boolean {
        return activeCapture != Capture.FRONT &&
            activeCaptureStartedAt.elapsedNow() < sideCapturePromptDuration.milliseconds
    }

    private fun consumeSideCapturePromptCompletion(): Boolean {
        if (activeCapture == Capture.FRONT || sideCapturePromptCompleted) {
            return false
        }
        sideCapturePromptCompleted = true
        return true
    }

    private suspend fun saveFrame(
        analyzerInput: AnalyzerInput,
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ) {
        selfieFrameSaver.saveFrame(
            SelfieFrame(
                input = analyzerInput,
                output = analyzerOutput,
                bestFrameScore = calculateBestFrameScore(analyzerOutput, motionBlurResult),
                capture = activeCapture,
            ),
            analyzerOutput
        )
    }

    private fun isActiveCaptureCollected() =
        activeCaptureCollected() >= requiredFramesForActiveCapture()

    private fun activeCaptureCollected(): Int {
        return if (activeCapture == Capture.FRONT) {
            selfieFrameSaver.selfieCollected()
        } else {
            selfieFrameSaver.framesCollected(activeCapture)
        }
    }

    private fun requiredFramesForActiveCapture(): Int {
        return if (activeCapture == Capture.FRONT) {
            selfieCapturePage.numSamples
        } else {
            SIDE_CAPTURE_NUM_FRAMES
        }
    }

    @Suppress("MagicNumber")
    private fun calculateBestFrameScore(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ): Float {
        val faceScore = analyzerOutput.resultScore.coerceIn(0f, 1f)
        val centeringScore = calculateCenteringScore(analyzerOutput.boundingBox)
        val coverageScore = calculateCoverageScore(analyzerOutput.boundingBox)
        val stabilityScore = when (motionBlurResult?.hasMotionBlur) {
            true -> 0f
            false -> 1f
            null -> DEFAULT_UNKNOWN_STABILITY_SCORE
        }

        // Matches iOS: each component is weighted evenly.
        return (
            faceScore +
                centeringScore +
                coverageScore +
                stabilityScore
            ) / 4f
    }

    @Suppress("MagicNumber")
    private fun calculateCenteringScore(boundingBox: BoundingBox): Float {
        // Mirrors iOS: euclidean distance from center, normalized to [0, 1].
        val midX = boundingBox.left + (boundingBox.width / 2f)
        val midY = boundingBox.top + (boundingBox.height / 2f)

        val dx = abs(midX - 0.5f)
        val dy = abs(midY - 0.5f)
        val distanceFromCenter = sqrt((dx * dx) + (dy * dy))
        val maxDistanceFromCenter = sqrt(0.5f)
        val normalizedDistance = min(1f, distanceFromCenter / maxDistanceFromCenter)

        return 1f - normalizedDistance
    }

    private fun calculateCoverageScore(boundingBox: BoundingBox): Float {
        // Mirrors iOS: prefer coverage close to a fixed target.
        val coverage = boundingBox.width * boundingBox.height
        val delta = abs(coverage - BEST_FRAME_TARGET_COVERAGE)
        val normalizedDelta = min(1f, delta / BEST_FRAME_MAX_COVERAGE_DELTA)
        return 1f - normalizedDelta
    }

    /**
     * Check face is centered by making sure center of face is
     * within corresponding threshold of center of image in both dimensions.
     */
    private fun isFaceCentered(boundingBox: BoundingBox): Boolean {
        return abs(1 - (boundingBox.top + boundingBox.top + boundingBox.height)) <
            selfieCapturePage.maxCenteredThresholdY &&
            abs(1 - (boundingBox.left + boundingBox.left + boundingBox.width)) <
            selfieCapturePage.maxCenteredThresholdX
    }

    private fun isFaceAwayFromEdges(boundingBox: BoundingBox): Boolean {
        selfieCapturePage.minEdgeThreshold.let { edgeThreshold ->
            return boundingBox.top > edgeThreshold && boundingBox.left > edgeThreshold &&
                (boundingBox.top + boundingBox.height) < (1 - edgeThreshold) &&
                (boundingBox.left + boundingBox.width) < (1 - edgeThreshold)
        }
    }

    /**
     * Check coverage is within range.
     *
     * coverage = (area of bounding box)/(area of input image)
     */
    private fun isFaceCoverageOK(boundingBox: BoundingBox): Boolean {
        (boundingBox.width * boundingBox.height).let { coverage ->
            return coverage < selfieCapturePage.maxCoverageThreshold &&
                coverage > selfieCapturePage.minCoverageThreshold
        }
    }

    private fun isFaceScoreOverThreshold(actualScore: Float) =
        actualScore > selfieCapturePage.models.faceDetectorMinScore

    internal enum class Capture(val frameIdentifier: String) {
        FRONT(SELFIES),
        LEFT(LEFT_SELFIE),
        RIGHT(RIGHT_SELFIE);

        fun next(): Capture? = when (this) {
            FRONT -> LEFT
            LEFT -> RIGHT
            RIGHT -> null
        }
    }

    internal enum class Selfie(val index: Int, val value: String, val capture: Capture) {
        FIRST(INDEX_FIRST, VALUE_FIRST, Capture.FRONT),
        BEST(INDEX_BEST, VALUE_BEST, Capture.FRONT),
        LAST(INDEX_LAST, VALUE_LAST, Capture.FRONT),
        LEFT(INDEX_SIDE, VALUE_LEFT, Capture.LEFT),
        RIGHT(INDEX_SIDE, VALUE_RIGHT, Capture.RIGHT)
    }

    internal companion object {
        val TAG: String = FaceDetectorTransitioner::class.java.simpleName
        const val SELFIES = "SELFIES"
        const val LEFT_SELFIE = "LEFT_SELFIE"
        const val RIGHT_SELFIE = "RIGHT_SELFIE"
        const val NUM_FILTERED_FRAMES = 3
        const val INDEX_FIRST = 0
        const val INDEX_BEST = 1
        const val INDEX_LAST = 2
        const val INDEX_SIDE = -1
        const val VALUE_FIRST = "first"
        const val VALUE_LAST = "last"
        const val VALUE_BEST = "best"
        const val VALUE_LEFT = "left"
        const val VALUE_RIGHT = "right"
        const val DEFAULT_STAY_IN_FOUND_DURATION = 2000
        const val DEFAULT_SIDE_CAPTURE_PROMPT_DURATION = 900

        private const val SIDE_CAPTURE_NUM_FRAMES = 2
        private const val SIDE_CAPTURE_YAW_THRESHOLD = 10f
        private const val DEFAULT_MOTION_BLUR_MIN_DURATION_MS = 100L
        private const val DEFAULT_UNKNOWN_STABILITY_SCORE = 0.5f

        // Mirrors iOS FaceScannerOutput.BestFrame
        private const val BEST_FRAME_TARGET_COVERAGE = 0.16f
        private const val BEST_FRAME_MAX_COVERAGE_DELTA = 0.16f
    }
}
