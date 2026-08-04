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
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage.Companion.POSE_LEFT
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage.Companion.POSE_RIGHT
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
@Suppress("LargeClass")
internal class FaceDetectorTransitioner(
    private val selfieCapturePage: VerificationPageStaticContentSelfieCapturePage,
    internal val selfieFrameSaver: SelfieFrameSaver = SelfieFrameSaver(),
    private val stayInFoundDuration: Int = DEFAULT_STAY_IN_FOUND_DURATION,
    private val sideCapturePromptDuration: Int = DEFAULT_SIDE_CAPTURE_PROMPT_DURATION,
    enable3DFaceCapture: Boolean = false,
    private val sideCaptureFallbackDuration: Int = DEFAULT_SIDE_CAPTURE_FALLBACK_DURATION
) : IdentityScanStateTransitioner {
    @VisibleForTesting
    var timeoutAt: ComparableTimeMark = captureTimeoutFromNow()

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

    @VisibleForTesting
    internal var sideCaptureBestFrameStartedAt: ComparableTimeMark? = null
    private var latestSideCaptureFallbackFrame: SelfieFrame? = null
    private val captureSequence = buildList {
        add(Capture.FRONT)
        if (enable3DFaceCapture) {
            addAll(selfieCapturePage.sideCaptureSequence())
        }
    }

    internal val uses3DFaceCapture: Boolean = captureSequence.any { it != Capture.FRONT }

    @VisibleForTesting
    internal var captureGuideProgress: Float = 0f
        private set

    private var consecutiveTooFarFrameCount = 0
    private var shouldShowMoveCloser = false

    internal val isWaitingForSideCapturePrompt: Boolean
        get() = shouldWaitForSideCapturePrompt()

    @VisibleForTesting
    fun resetAndReturn(): FaceDetectorTransitioner {
        restartCaptureTimeout()
        motionBlurDetector.reset()
        activeCapture = Capture.FRONT
        completedCapture = null
        captureGuideProgress = 0f
        captureStarted = false
        activeCaptureStartedAt = TimeSource.Monotonic.markNow()
        sideCapturePromptCompleted = true
        sideCaptureBestFrameStartedAt = null
        latestSideCaptureFallbackFrame = null
        resetMoveCloserFeedback()
        return this
    }

    internal data class SelfieFrame(
        val input: AnalyzerInput,
        val output: FaceDetectorOutput,
        val bestFrameScore: Float,
        val capture: Capture = Capture.FRONT,
        val capturedAt: Long = System.currentTimeMillis()
    )

    internal val filteredSelfieFrames: List<SelfieFrame>
        get() {
            val savedFrames = requireNotNull(selfieFrameSaver.getSavedFrames()[Capture.FRONT.frameIdentifier]) {
                "No frames saved"
            }
            require(savedFrames.size >= NUM_FILTERED_FRAMES) {
                "Not enough frames saved, frames saved: ${savedFrames.size}"
            }

            // Return the first, the best (based on bestFrameScore), and the last frame collected.
            val firstFrame = savedFrames.last()
            val lastFrame = savedFrames.first()
            val bestFrame = requireNotNull(
                savedFrames.subList(1, savedFrames.size - 1)
                    .maxByOrNull { it.bestFrameScore }
            ) { "Couldn't find best frame" }

            return listOf(firstFrame, bestFrame, lastFrame)
        }

    internal val filteredFrames: List<Pair<AnalyzerInput, FaceDetectorOutput>>
        get() = filteredSelfieFrames.map { it.input to it.output }

    internal val numFrames = selfieCapturePage.numSamples

    internal val bestFaceScore: Float
        get() {
            return filteredFrames[INDEX_BEST].second.resultScore
        }

    internal fun frameForSelfie(selfie: Selfie): Pair<AnalyzerInput, FaceDetectorOutput> {
        return selfieFrameForSelfie(selfie).let { it.input to it.output }
    }

    internal fun selfieFrameForSelfie(selfie: Selfie): SelfieFrame {
        return when (selfie) {
            Selfie.FIRST,
            Selfie.BEST,
            Selfie.LAST -> filteredSelfieFrames[selfie.index]
            Selfie.LEFT,
            Selfie.RIGHT -> sideFrame(selfie.capture)
        }
    }

    private fun sideFrame(capture: Capture): SelfieFrame {
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
        return bestFrame
    }

    internal val sideSelfies: List<Selfie>
        get() = captureSequence
            .filter { it != Capture.FRONT }
            .map { Selfie.fromCapture(it) }

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

        if (activeCapture == Capture.FRONT && timeoutAt.hasPassedNow()) {
            Log.d(TAG, "Timeout in Initial state: $initialState")
            return IdentityScanState.TimeOut(initialState.type, this)
        }

        val shouldRefreshInitialAfterSidePrompt = consumeSideCapturePromptCompletion()
        val nowTimestampMs = SystemClock.elapsedRealtime()
        val motionBlurResult = determineMotionBlurResult(analyzerOutput, nowTimestampMs)
        updateMoveCloserFeedback(analyzerOutput, motionBlurResult)
        val previousCaptureGuideProgress = captureGuideProgress
        val isFrameValid = isFrameValidForActiveCapture(analyzerOutput, motionBlurResult)
        rememberSideCaptureFallbackFrame(
            analyzerInput = analyzerInput,
            analyzerOutput = analyzerOutput,
            motionBlurResult = motionBlurResult
        )

        captureSideFallbackOrTimeout(initialState)?.let {
            return it
        }

        return when {
            isFrameValid -> {
                Log.d(TAG, "Valid face found, transition to Found")
                if (activeCapture != Capture.FRONT) {
                    sideCapturePromptCompleted = true
                    sideCaptureBestFrameStartedAt = TimeSource.Monotonic.markNow()
                    latestSideCaptureFallbackFrame = null
                }
                saveFrame(
                    analyzerInput = analyzerInput,
                    analyzerOutput = analyzerOutput,
                    motionBlurResult = motionBlurResult
                )
                if (activeCapture == Capture.FRONT && isActiveCaptureCollected()) {
                    completedCapture = activeCapture
                    Satisfied(initialState.type, this)
                } else {
                    if (activeCapture == Capture.FRONT) {
                        restartCaptureTimeout()
                    }
                    Found(initialState.type, this)
                }
            }

            else -> {
                Log.d(TAG, "Valid face not found, stay in Initial")
                if (shouldRefreshInitialAfterSidePrompt ||
                    shouldRefreshInitialForCaptureGuideProgress(previousCaptureGuideProgress) ||
                    initialState.feedbackRes != moveCloserFeedbackRes()
                ) {
                    initialState.withFeedback(moveCloserFeedbackRes())
                } else {
                    initialState
                }
            }
        }
    }

    private fun shouldRefreshInitialForCaptureGuideProgress(previousCaptureGuideProgress: Float): Boolean {
        return activeCapture != Capture.FRONT &&
            previousCaptureGuideProgress != captureGuideProgress
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
        updateMoveCloserFeedback(analyzerOutput, motionBlurResult)

        if (activeCapture != Capture.FRONT) {
            return transitionFromFoundForSideCapture(
                foundState = foundState,
                analyzerInput = analyzerInput,
                analyzerOutput = analyzerOutput,
                motionBlurResult = motionBlurResult
            )
        }

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
                foundState.withMoveCloserFeedbackIfChanged()
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
                    restartCaptureTimeout()
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
                foundState.withMoveCloserFeedbackIfChanged()
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

    private suspend fun transitionFromFoundForSideCapture(
        foundState: Found,
        analyzerInput: AnalyzerInput,
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?
    ): IdentityScanState {
        val bestFrameStartedAt = sideCaptureBestFrameStartedAt
            ?: TimeSource.Monotonic.markNow().also { sideCaptureBestFrameStartedAt = it }

        if (bestFrameStartedAt.elapsedNow() >= SIDE_CAPTURE_BEST_FRAME_DURATION.milliseconds) {
            completedCapture = activeCapture
            return Satisfied(foundState.type, this)
        }

        if (foundState.reachedStateAt.elapsedNow() < selfieCapturePage.sampleInterval.milliseconds) {
            return foundState
        }

        return if (isFrameValidForActiveCapture(analyzerOutput, motionBlurResult)) {
            saveFrame(
                analyzerInput = analyzerInput,
                analyzerOutput = analyzerOutput,
                motionBlurResult = motionBlurResult
            )
            Found(foundState.type, this)
        } else {
            foundState
        }
    }

    override suspend fun transitionFromSatisfied(
        satisfiedState: Satisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        if (satisfiedState.reachedStateAt.elapsedNow() < captureAcknowledgementDuration().milliseconds) {
            return satisfiedState
        }

        val nextCapture = nextCapture()
        return if (nextCapture == null) {
            Finished(satisfiedState.type, this)
        } else {
            activeCapture = nextCapture
            activeCaptureStartedAt = TimeSource.Monotonic.markNow()
            restartCaptureTimeout()
            resetMoveCloserFeedback()
            completedCapture = null
            captureGuideProgress = 0f
            sideCapturePromptCompleted = false
            sideCaptureBestFrameStartedAt = null
            latestSideCaptureFallbackFrame = null
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
            motionBlurDetector.determineMotionBlur(analyzerOutput.validationBoundingBox, nowTimestampMs)
        } else {
            null
        }
    }

    private fun updateMoveCloserFeedback(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?
    ) {
        if (activeCapture != Capture.FRONT || !isFaceTooFar(analyzerOutput, motionBlurResult)) {
            resetMoveCloserFeedback()
            return
        }

        if (!shouldShowMoveCloser) {
            consecutiveTooFarFrameCount += 1
            shouldShowMoveCloser = consecutiveTooFarFrameCount >= MOVE_CLOSER_REQUIRED_FRAME_COUNT
        }
    }

    private fun resetMoveCloserFeedback() {
        consecutiveTooFarFrameCount = 0
        shouldShowMoveCloser = false
    }

    private fun moveCloserFeedbackRes(): Int? {
        return if (shouldShowMoveCloser) {
            com.stripe.android.identity.R.string.stripe_selfie_move_closer
        } else {
            null
        }
    }

    private fun Found.withMoveCloserFeedbackIfChanged(): Found {
        val feedbackRes = moveCloserFeedbackRes()
        return if (this.feedbackRes == feedbackRes) {
            this
        } else {
            withFeedback(feedbackRes)
        }
    }

    private fun isFaceTooFar(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?
    ): Boolean {
        val boundingBox = analyzerOutput.validationBoundingBox
        return isFaceScoreOverThreshold(analyzerOutput.resultScore) &&
            motionBlurResult?.hasMotionBlur != true &&
            isFaceCentered(boundingBox) &&
            isFaceAwayFromEdges(boundingBox) &&
            boundingBox.width * boundingBox.height <= selfieCapturePage.minCoverageThreshold
    }

    private fun isFaceValid(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ) =
        isFaceCentered(analyzerOutput.validationBoundingBox) &&
            isFaceAwayFromEdges(analyzerOutput.validationBoundingBox) &&
            isFaceCoverageOK(analyzerOutput.validationBoundingBox) &&
            isFaceScoreOverThreshold(analyzerOutput.resultScore) &&
            // Match iOS: treat frames as invalid only when motion blur is explicitly detected.
            motionBlurResult?.hasMotionBlur != true

    private fun isFrameValidForActiveCapture(
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ): Boolean {
        updateCaptureGuideProgress(analyzerOutput)
        if (!isFaceValid(analyzerOutput, motionBlurResult)) {
            return false
        }

        return isPoseValidForActiveCapture(analyzerOutput)
    }

    private fun isPoseValidForActiveCapture(analyzerOutput: FaceDetectorOutput): Boolean {
        return when (activeCapture) {
            Capture.FRONT -> true
            Capture.LEFT,
            Capture.RIGHT -> {
                !shouldWaitForSideCapturePrompt() &&
                    analyzerOutput.pose != null &&
                    captureGuideProgress >= 1f
            }
        }
    }

    private fun updateCaptureGuideProgress(analyzerOutput: FaceDetectorOutput) {
        val previousProgress = captureGuideProgress
        captureGuideProgress = when (activeCapture) {
            Capture.FRONT -> 0f
            Capture.LEFT,
            Capture.RIGHT -> {
                val pose = analyzerOutput.pose
                if (pose == null || !isFaceScoreOverThreshold(analyzerOutput.resultScore)) {
                    0f
                } else {
                    captureGuideProgressForPose(activeCapture, pose.yaw)
                }
            }
        }
        logCaptureGuideProgress(
            analyzerOutput = analyzerOutput,
            previousProgress = previousProgress
        )
    }

    private fun captureGuideProgressForPose(capture: Capture, yaw: Float): Float {
        return when (capture) {
            Capture.LEFT -> yaw / SIDE_CAPTURE_YAW_THRESHOLD_DEGREES
            Capture.RIGHT -> -yaw / SIDE_CAPTURE_YAW_THRESHOLD_DEGREES
            Capture.FRONT -> 0f
        }.coerceIn(0f, 1f)
    }

    private fun logCaptureGuideProgress(
        analyzerOutput: FaceDetectorOutput,
        previousProgress: Float
    ) {
        if (activeCapture == Capture.FRONT) {
            return
        }

        val pose = analyzerOutput.pose
        Log.d(
            TAG,
            "Selfie side capture pose " +
                "activeCapture=$activeCapture, " +
                "score=${analyzerOutput.resultScore}, " +
                "faceScoreOk=${isFaceScoreOverThreshold(analyzerOutput.resultScore)}, " +
                "bbox=${analyzerOutput.boundingBox}, " +
                "fullFrameBBox=${analyzerOutput.fullFrameBoundingBox}, " +
                "pose=$pose, " +
                "yaw=${pose?.yaw}, " +
                "pitch=${pose?.pitch}, " +
                "roll=${pose?.roll}, " +
                "progress=$previousProgress->$captureGuideProgress, " +
                "threshold=$SIDE_CAPTURE_YAW_THRESHOLD_DEGREES"
        )
    }

    private fun rememberSideCaptureFallbackFrame(
        analyzerInput: AnalyzerInput,
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?
    ) {
        if (activeCapture == Capture.FRONT ||
            sideCaptureBestFrameStartedAt != null ||
            !isFaceValid(analyzerOutput, motionBlurResult)
        ) {
            return
        }

        latestSideCaptureFallbackFrame = createSelfieFrame(
            analyzerInput = analyzerInput,
            analyzerOutput = analyzerOutput,
            motionBlurResult = motionBlurResult
        )
    }

    private suspend fun captureSideFallbackIfAvailable(
        scanType: IdentityScanState.ScanType
    ): Satisfied? {
        if (activeCapture == Capture.FRONT ||
            shouldWaitForSideCapturePrompt() ||
            sideCaptureBestFrameStartedAt != null ||
            activeCaptureStartedAt.elapsedNow() < sideCaptureFallbackDuration.milliseconds
        ) {
            return null
        }

        val fallbackFrame = latestSideCaptureFallbackFrame ?: return null
        selfieFrameSaver.saveFrame(fallbackFrame, fallbackFrame.output)
        completedCapture = activeCapture
        captureGuideProgress = 1f
        sideCapturePromptCompleted = true
        latestSideCaptureFallbackFrame = null
        Log.d(TAG, "Captured latest usable fallback frame for $activeCapture")
        return Satisfied(scanType, this)
    }

    private suspend fun captureSideFallbackOrTimeout(initialState: Initial): IdentityScanState? {
        captureSideFallbackIfAvailable(initialState.type)?.let {
            return it
        }
        return if (timeoutAt.hasPassedNow()) {
            Log.d(TAG, "Timeout in Initial state: $initialState")
            IdentityScanState.TimeOut(initialState.type, this)
        } else {
            null
        }
    }

    private fun shouldWaitForSideCapturePrompt(): Boolean {
        return activeCapture != Capture.FRONT &&
            !sideCapturePromptCompleted &&
            activeCaptureStartedAt.elapsedNow() < sideCapturePromptDuration.milliseconds
    }

    private fun consumeSideCapturePromptCompletion(): Boolean {
        if (activeCapture == Capture.FRONT ||
            sideCapturePromptCompleted ||
            activeCaptureStartedAt.elapsedNow() < sideCapturePromptDuration.milliseconds
        ) {
            return false
        }
        sideCapturePromptCompleted = true
        return true
    }

    private fun captureAcknowledgementDuration(): Int {
        if (!uses3DFaceCapture) {
            return LEGACY_CAPTURE_ACKNOWLEDGEMENT_DURATION
        }
        return if (completedCapture == Capture.FRONT) {
            FRONT_CAPTURE_ACKNOWLEDGEMENT_DURATION
        } else {
            SIDE_CAPTURE_ACKNOWLEDGEMENT_DURATION
        }
    }

    private suspend fun saveFrame(
        analyzerInput: AnalyzerInput,
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ) {
        val selfieFrame = createSelfieFrame(
            analyzerInput = analyzerInput,
            analyzerOutput = analyzerOutput,
            motionBlurResult = motionBlurResult
        )
        selfieFrameSaver.saveFrame(
            selfieFrame,
            analyzerOutput
        )
    }

    private fun createSelfieFrame(
        analyzerInput: AnalyzerInput,
        analyzerOutput: FaceDetectorOutput,
        motionBlurResult: MotionBlurDetector.Output?,
    ): SelfieFrame {
        return SelfieFrame(
            input = analyzerInput,
            output = analyzerOutput,
            bestFrameScore = calculateBestFrameScore(analyzerOutput, motionBlurResult),
            capture = activeCapture,
        )
    }

    private fun captureTimeoutFromNow(): ComparableTimeMark {
        return TimeSource.Monotonic.markNow() + selfieCapturePage.autoCaptureTimeout.milliseconds
    }

    private fun restartCaptureTimeout() {
        timeoutAt = captureTimeoutFromNow()
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
        val centeringScore = calculateCenteringScore(analyzerOutput.validationBoundingBox)
        val coverageScore = calculateCoverageScore(analyzerOutput.validationBoundingBox)
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

    private fun nextCapture(): Capture? {
        val currentIndex = captureSequence.indexOf(activeCapture)
        return captureSequence.getOrNull(currentIndex + 1)
    }

    private fun VerificationPageStaticContentSelfieCapturePage.sideCaptureSequence(): List<Capture> {
        return poseSequence
            ?.mapNotNull { Capture.fromPoseSequenceValue(it) }
            ?.filter { it != Capture.FRONT }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_SIDE_CAPTURE_SEQUENCE
    }

    internal enum class Capture(val frameIdentifier: String) {
        FRONT(SELFIES),
        LEFT(LEFT_SELFIE),
        RIGHT(RIGHT_SELFIE);

        internal companion object {
            fun fromPoseSequenceValue(value: String): Capture? = when (value) {
                VALUE_FRONT -> FRONT
                POSE_LEFT -> LEFT
                POSE_RIGHT -> RIGHT
                else -> null
            }
        }
    }

    internal enum class Selfie(val index: Int, val value: String, val capture: Capture) {
        FIRST(INDEX_FIRST, VALUE_FIRST, Capture.FRONT),
        BEST(INDEX_BEST, VALUE_BEST, Capture.FRONT),
        LAST(INDEX_LAST, VALUE_LAST, Capture.FRONT),
        LEFT(INDEX_SIDE, VALUE_LEFT, Capture.LEFT),
        RIGHT(INDEX_SIDE, VALUE_RIGHT, Capture.RIGHT);

        internal companion object {
            fun fromCapture(capture: Capture): Selfie = when (capture) {
                Capture.FRONT -> error("Front capture maps to first, best, and last selfies")
                Capture.LEFT -> LEFT
                Capture.RIGHT -> RIGHT
            }
        }
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
        const val VALUE_FRONT = "front"
        const val VALUE_FIRST = "first"
        const val VALUE_LAST = "last"
        const val VALUE_BEST = "best"
        const val VALUE_LEFT = "left"
        const val VALUE_RIGHT = "right"
        const val DEFAULT_STAY_IN_FOUND_DURATION = 2000
        const val DEFAULT_SIDE_CAPTURE_PROMPT_DURATION = 1000
        const val DEFAULT_SIDE_CAPTURE_FALLBACK_DURATION = 8000

        private const val SIDE_CAPTURE_NUM_FRAMES = 2
        private const val SIDE_CAPTURE_BEST_FRAME_DURATION = 1000
        private const val LEGACY_CAPTURE_ACKNOWLEDGEMENT_DURATION = 550
        private const val FRONT_CAPTURE_ACKNOWLEDGEMENT_DURATION = 1400
        private const val SIDE_CAPTURE_ACKNOWLEDGEMENT_DURATION = 1500
        private const val SIDE_CAPTURE_YAW_THRESHOLD_DEGREES = 15f
        private const val DEFAULT_MOTION_BLUR_MIN_DURATION_MS = 100L
        private const val DEFAULT_UNKNOWN_STABILITY_SCORE = 0.5f
        private const val MOVE_CLOSER_REQUIRED_FRAME_COUNT = 3
        private val DEFAULT_SIDE_CAPTURE_SEQUENCE = listOf(Capture.RIGHT, Capture.LEFT)

        // Mirrors iOS FaceScannerOutput.BestFrame
        private const val BEST_FRAME_TARGET_COVERAGE = 0.16f
        private const val BEST_FRAME_MAX_COVERAGE_DELTA = 0.16f
    }
}
