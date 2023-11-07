package com.stripe.android.identity.states

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
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
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * [IdentityScanStateTransitioner] for FaceDetector model.
 *
 * To transition from [Initial] state -
 * * Check if it's timeout since the start of the scan.
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
 * * Directly transitions to [Finished]
 *
 * To transition from [Unsatisfied] state -
 * * Directly transitions to [Initial]
 */
internal class FaceDetectorTransitioner(
    private val selfieCapturePage: VerificationPageStaticContentSelfieCapturePage,
    internal val selfieFrameSaver: SelfieFrameSaver = SelfieFrameSaver(),
    private val stayInFoundDuration: Int = DEFAULT_STAY_IN_FOUND_DURATION
) : IdentityScanStateTransitioner {
    @VisibleForTesting
    var timeoutAt: ClockMark =
        Clock.markNow() + selfieCapturePage.autoCaptureTimeout.milliseconds

    @VisibleForTesting
    fun resetAndReturn(): FaceDetectorTransitioner {
        timeoutAt = Clock.markNow() + selfieCapturePage.autoCaptureTimeout.milliseconds
        return this
    }

    internal val filteredFrames: List<Pair<AnalyzerInput, FaceDetectorOutput>>
        get() {
            val savedFrames = requireNotNull(selfieFrameSaver.getSavedFrames()[SELFIES]) {
                "No frames saved"
            }
            require(savedFrames.size >= NUM_FILTERED_FRAMES) {
                "Not enough frames saved, frames saved: ${savedFrames.size}"
            }

            // return the first, the best(based on resultScore) and the last frame collected
            return mutableListOf(
                savedFrames.last,
                requireNotNull(
                    savedFrames.subList(1, savedFrames.size - 1)
                        .maxByOrNull { it.second.resultScore }
                ) { "Couldn't find best frame" },
                savedFrames.first
            )
        }

    internal val numFrames = selfieCapturePage.numSamples

    internal val bestFaceScore: Float
        get() {
            return filteredFrames[INDEX_BEST].second.resultScore
        }

    internal val scoreVariance: Float
        get() {
            val savedFrames = requireNotNull(selfieFrameSaver.getSavedFrames()[SELFIES]) {
                "No frames saved"
            }
            require(savedFrames.size == numFrames) {
                "Not enough frames saved, score variance not calculated"
            }
            val mean =
                savedFrames.fold(0f) { acc, pair ->
                    acc + pair.second.resultScore
                }.div(numFrames.toFloat())

            return sqrt(
                savedFrames.fold(0f) { acc, pair ->
                    acc + (pair.second.resultScore - mean).pow(2)
                }.div(numFrames.toFloat())
            ).roundToMaxDecimals(2)
        }

    internal class SelfieFrameSaver :
        FrameSaver<String, Pair<AnalyzerInput, FaceDetectorOutput>, AnalyzerOutput>() {
        // Don't limit max number of saved frames, let the transitioner decide when to stop saving
        // new frames.
        override fun getMaxSavedFrames(savedFrameIdentifier: String) = Int.MAX_VALUE

        override fun getSaveFrameIdentifier(
            frame: Pair<AnalyzerInput, FaceDetectorOutput>,
            metaData: AnalyzerOutput
        ) = SELFIES

        fun selfieCollected(): Int = getSavedFrames()[SELFIES]?.size ?: 0
    }

    override suspend fun transitionFromInitial(
        initialState: Initial,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is FaceDetectorOutput) {
            "Unexpected output type: $analyzerOutput"
        }
        selfieFrameSaver.reset()
        return when {
            timeoutAt.hasPassed() -> {
                Log.d(TAG, "Timeout in Initial state: $initialState")
                IdentityScanState.TimeOut(initialState.type, this)
            }

            isFaceValid(analyzerOutput) -> {
                Log.d(TAG, "Valid face found, transition to Found")
                selfieFrameSaver.saveFrame(
                    (analyzerInput to analyzerOutput),
                    analyzerOutput
                )
                Found(initialState.type, this)
            }

            else -> {
                Log.d(TAG, "Valid face not found, stay in Initial")
                initialState
            }
        }
    }

    override suspend fun transitionFromFound(
        foundState: Found,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        require(analyzerOutput is FaceDetectorOutput) { "Unexpected output type: $analyzerOutput" }
        return when {
            timeoutAt.hasPassed() -> {
                Log.d(TAG, "Timeout in Found state: $foundState")
                IdentityScanState.TimeOut(foundState.type, this)
            }

            foundState.reachedStateAt.elapsedSince() < selfieCapturePage.sampleInterval.milliseconds -> {
                Log.d(
                    TAG,
                    "Get a selfie before selfie capture interval, ignored. " +
                        "Current selfieCollected: ${selfieFrameSaver.selfieCollected()}"
                )
                foundState
            }

            isFaceValid(analyzerOutput) -> {
                selfieFrameSaver.saveFrame((analyzerInput to analyzerOutput), analyzerOutput)
                if (selfieFrameSaver.selfieCollected() >= selfieCapturePage.numSamples) {
                    Log.d(
                        TAG,
                        "A valid selfie captured, enough selfie " +
                            "collected(${selfieCapturePage.numSamples}), transitions to Satisfied"
                    )
                    Satisfied(foundState.type, this)
                } else {
                    Log.d(
                        TAG,
                        "A valid selfie captured, need ${selfieCapturePage.numSamples} selfies" +
                            " but has ${selfieFrameSaver.selfieCollected()}, stays in Found"
                    )
                    Found(foundState.type, this)
                }
            }

            foundState.reachedStateAt.elapsedSince() < stayInFoundDuration.milliseconds -> {
                Log.d(
                    TAG,
                    "Get an invalid selfie in Found state, but not enough time " +
                        "passed(${foundState.reachedStateAt.elapsedSince()}), stays in Found. " +
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
        return Finished(satisfiedState.type, this)
    }

    override suspend fun transitionFromUnsatisfied(
        unsatisfiedState: Unsatisfied,
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState {
        return Initial(unsatisfiedState.type, this.resetAndReturn())
    }

    private fun isFaceValid(analyzerOutput: FaceDetectorOutput) =
        isFaceCentered(analyzerOutput.boundingBox) &&
            isFaceAwayFromEdges(analyzerOutput.boundingBox) &&
            isFaceCoverageOK(analyzerOutput.boundingBox) &&
            isFaceScoreOverThreshold(analyzerOutput.resultScore)

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

    internal enum class Selfie(val index: Int, val value: String) {
        FIRST(INDEX_FIRST, VALUE_FIRST), BEST(INDEX_BEST, VALUE_BEST), LAST(INDEX_LAST, VALUE_LAST)
    }

    internal companion object {
        val TAG: String = FaceDetectorTransitioner::class.java.simpleName
        const val SELFIES = "SELFIES"
        const val NUM_FILTERED_FRAMES = 3
        const val INDEX_FIRST = 0
        const val INDEX_BEST = 1
        const val INDEX_LAST = 2
        const val VALUE_FIRST = "first"
        const val VALUE_LAST = "last"
        const val VALUE_BEST = "best"
        const val DEFAULT_STAY_IN_FOUND_DURATION = 2000
    }
}
