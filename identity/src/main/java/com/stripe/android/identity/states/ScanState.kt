package com.stripe.android.identity.states

import android.util.Log
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.Category

/**
 * States during scanning a document.
 */
internal sealed class ScanState(val type: ScanType) {
    /**
     * Type of documents being scanned
     */
    enum class ScanType {
        ID_FRONT,
        ID_BACK,
        PASSPORT,
        SELFIE
    }

    /**
     * Transitions to the next state based on model output.
     */
    internal abstract fun consumeTransition(
        analyzerOutput: AnalyzerOutput
    ): ScanState

    /**
     * Initial state when scan starts, no documents have been detected yet.
     */
    internal class Initial(type: ScanType) : ScanState(type) {
        /**
         * Only transitions to [Found] when ML output type matches scan type
         */
        override fun consumeTransition(analyzerOutput: AnalyzerOutput) =
            if (analyzerOutput.category.matchesScanType(type)) {
                Log.d(
                    TAG,
                    "Matching model output detected with score ${analyzerOutput.score}, " +
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

        private fun Category.matchesScanType(scanType: ScanType): Boolean {
            return this == Category.ID_BACK && scanType == ScanType.ID_BACK ||
                this == Category.ID_FRONT && scanType == ScanType.ID_FRONT ||
                this == Category.PASSPORT && scanType == ScanType.PASSPORT
        }
    }

    /**
     * State when scan has found the required type, the machine could stay in this state for a
     * while if more image needs to be processed to reach the next state.
     */
    internal class Found(type: ScanType) : ScanState(type) {
        override fun consumeTransition(analyzerOutput: AnalyzerOutput) =
            when {
                isUnsatisfied() -> {
                    val reason = "unsatisfied reason"
                    Log.d(
                        TAG, "Satisfaction check fails due to $reason, transition to Unsatisfied."
                    )
                    Unsatisfied(reason, type)
                }
                moreResultsRequired() -> {
                    Log.d(TAG, "More results needed, stay in Found.")
                    this
                }
                else -> {
                    Log.d(TAG, "Satisfaction check succeeds, transition to Satisfied.")
                    Satisfied(type)
                }
            }

        /**
         * Determine if more images should be processed before reaching [Satisfied].
         *
         * TODO(ccen) - Introduce conditions that requires more results
         */
        private fun moreResultsRequired(): Boolean {
            return false
        }

        /**
         * Determine if satisfaction failed and should transition to [Unsatisfied].
         *
         * TODO(ccen) - Introduce unsatisfied reasons
         */
        private fun isUnsatisfied(): Boolean {
            return false
        }
    }

    /**
     * State  when satisfaction checking passed.
     */
    internal class Satisfied(
        type: ScanType,
        private val reachedStateAt: ClockMark = Clock.markNow()
    ) : ScanState(type) {

        override fun consumeTransition(analyzerOutput: AnalyzerOutput): ScanState {
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
     * State  when satisfaction checking failed.
     */
    internal class Unsatisfied(private val reason: String, type: ScanType) : ScanState(type) {
        override fun consumeTransition(analyzerOutput: AnalyzerOutput): ScanState {
            Log.d(TAG, "Scan for $type Unsatisfied with reason $reason, transition to Initial.")
            return Initial(type)
        }
    }

    /**
     * Terminal state, indicting the scan is finished.
     */
    internal class Finished(type: ScanType) : ScanState(type) {
        override fun consumeTransition(analyzerOutput: AnalyzerOutput) = this
    }

    private companion object {
        val TAG: String = ScanState::class.java.simpleName
    }
}
