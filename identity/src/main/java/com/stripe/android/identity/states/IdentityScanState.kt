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
    internal class Initial(
        type: ScanType,
        private val transitioner: IdentityFoundStateTransitioner = IOUTransitioner()
    ) :
        IdentityScanState(type, false) {
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
                Found(type, transitioner)
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
    internal class Found(
        type: ScanType,
        @VisibleForTesting
        internal val transitioner: IdentityFoundStateTransitioner
    ) : IdentityScanState(type, false) {
        override fun consumeTransition(analyzerOutput: AnalyzerOutput): IdentityScanState {
            return transitioner.transition(this, analyzerOutput)
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
internal fun Category.matchesScanType(scanType: IdentityScanState.ScanType): Boolean {
    return this == Category.ID_BACK && scanType == IdentityScanState.ScanType.ID_BACK ||
        this == Category.ID_FRONT && scanType == IdentityScanState.ScanType.ID_FRONT ||
        this == Category.ID_BACK && scanType == IdentityScanState.ScanType.DL_BACK ||
        this == Category.ID_FRONT && scanType == IdentityScanState.ScanType.DL_FRONT ||
        this == Category.PASSPORT && scanType == IdentityScanState.ScanType.PASSPORT
}
