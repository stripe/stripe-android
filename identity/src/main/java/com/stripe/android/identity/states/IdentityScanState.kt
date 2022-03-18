package com.stripe.android.identity.states

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.camera.scanui.ScanState
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.Category

/**
 * States during scanning a document.
 */
internal sealed class IdentityScanState(val type: ScanType, isFinal: Boolean) : ScanState(isFinal) {
    /**
     * Type of documents being scanned
     */
    enum class ScanType {
        ID_FRONT,
        ID_BACK,
        DL_FRONT,
        DL_BACK,
        PASSPORT,
        SELFIE
    }

    /**
     * Transitions to the next state based on model output.
     */
    internal abstract fun consumeTransition(
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    /**
     * Initial state when scan starts, no documents have been detected yet.
     */
    internal class Initial(type: ScanType) : IdentityScanState(type, false) {
        /**
         * Only transitions to [Found] when ML output type matches scan type
         */
        override fun consumeTransition(analyzerOutput: AnalyzerOutput) =
            if (analyzerOutput.category.matchesScanType(type)) {
                Log.d(
                    TAG,
                    "Matching model output detected with score ${analyzerOutput.resultScore}, " +
                        "transition to Found."
                )
                Found(type)
            } else {
                Log.d(
                    TAG,
                    "Model outputs ${analyzerOutput.category}, which doesn't match with " +
                        "scanType $type, stay in Initial"
                )
                this
            }
    }

    /**
     * State when scan has found the required type, the machine could stay in this state for a
     * while if more image needs to be processed to reach the next state.
     */
    internal class Found(type: ScanType) : IdentityScanState(type, false) {
        @VisibleForTesting
        internal var hitsCount = 0

        // saves the results of previous certain number of frames
        @VisibleForTesting
        internal val results = ArrayDeque<Boolean>()

        override fun consumeTransition(analyzerOutput: AnalyzerOutput): IdentityScanState {
            val isHit = analyzerOutput.category.matchesScanType(type)
            if (isHit) {
                hitsCount++
            }
            results.addLast(isHit)
            // only save the last certain number of frames, dropping the first one if it goes beyond
            // If the first result is a hit, then decrease the hitsCount
            if (results.size > FRAMES_REQUIRED) {
                val firstResultIsHit = results.removeFirst()
                if (firstResultIsHit) {
                    hitsCount--
                }
            }

            return when {
                isUnsatisfied() -> {
                    val reason =
                        "hits count below expected: $hitsCount"
                    Log.d(
                        TAG,
                        "Satisfaction check fails due to $reason, transition to Unsatisfied."
                    )
                    Unsatisfied(reason, type)
                }
                moreResultsRequired() -> {
                    Log.d(
                        TAG,
                        "More results needed, stay in Found, currently ${results.size} results are collected"
                    )
                    this
                }
                else -> {
                    Log.d(TAG, "Satisfaction check succeeds, transition to Satisfied.")
                    Satisfied(type)
                }
            }
        }

        /**
         * Determine if more images should be processed before reaching [Satisfied].
         *
         * Need to collect [FRAMES_REQUIRED] results.
         */
        private fun moreResultsRequired(): Boolean {
            return results.size < FRAMES_REQUIRED
        }

        /**
         * Determine if satisfaction failed and should transition to [Unsatisfied].
         *
         * Transfers to when the previous [FRAMES_REQUIRED] number of frames has hits below
         * [HITS_REQUIRED].
         */
        private fun isUnsatisfied(): Boolean {
            return (results.size == FRAMES_REQUIRED) && (hitsCount < HITS_REQUIRED)
        }

        @VisibleForTesting
        internal companion object {
            // The number of frames needs to collected to determine if a model has found the
            // correct item.
            const val FRAMES_REQUIRED = 100

            // The number of hits to determine if the model has found the correct item.
            const val HITS_REQUIRED = 50
        }
    }

    /**
     * State when satisfaction checking passed.
     */
    internal class Satisfied(
        type: ScanType,
        private val reachedStateAt: ClockMark = Clock.markNow()
    ) : IdentityScanState(type, false) {

        override fun consumeTransition(analyzerOutput: AnalyzerOutput): IdentityScanState {
            return if (reachedStateAt.elapsedSince() > DISPLAY_SATISFIED_DURATION) {
                Log.d(TAG, "Scan for $type Satisfied, transition to Finished.")
                Finished(type)
            } else {
                Log.d(TAG, "Displaying satisfied state, waiting for timeout")
                this
            }
        }

        private companion object {
            val DISPLAY_SATISFIED_DURATION = 500.milliseconds
        }
    }

    /**
     * State when satisfaction checking failed.
     */
    internal class Unsatisfied(
        @VisibleForTesting
        internal val reason: String,
        type: ScanType,
        private val reachedStateAt: ClockMark = Clock.markNow()
    ) : IdentityScanState(type, false) {

        override fun consumeTransition(analyzerOutput: AnalyzerOutput): IdentityScanState {
            return if (reachedStateAt.elapsedSince() > DISPLAY_UNSATISFIED_DURATION) {
                Log.d(TAG, "Scan for $type Unsatisfied with reason $reason, transition to Initial.")
                Initial(type)
            } else {
                Log.d(TAG, "Displaying unsatisfied state, waiting for timeout")
                this
            }
        }

        private companion object {
            val DISPLAY_UNSATISFIED_DURATION = 500.milliseconds
        }
    }

    /**
     * Terminal state, indicting the scan is finished.
     */
    internal class Finished(type: ScanType) : IdentityScanState(type, true) {
        override fun consumeTransition(analyzerOutput: AnalyzerOutput) = this
    }

    private companion object {
        val TAG: String = IdentityScanState::class.java.simpleName
    }
}

/**
 * Checks if [Category] matches [IdentityScanState].
 * Note: the ML model will output ID_FRONT or ID_BACK for both ID and Driver License.
 */
private fun Category.matchesScanType(scanType: IdentityScanState.ScanType): Boolean {
    return this == Category.ID_BACK && scanType == IdentityScanState.ScanType.ID_BACK ||
        this == Category.ID_FRONT && scanType == IdentityScanState.ScanType.ID_FRONT ||
        this == Category.ID_BACK && scanType == IdentityScanState.ScanType.DL_BACK ||
        this == Category.ID_FRONT && scanType == IdentityScanState.ScanType.DL_FRONT ||
        this == Category.PASSPORT && scanType == IdentityScanState.ScanType.PASSPORT
}
