package com.stripe.android.identity.states

import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.scanui.ScanState
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.navigation.DriverLicenseScanDestination
import com.stripe.android.identity.navigation.DriverLicenseUploadDestination
import com.stripe.android.identity.navigation.IDScanDestination
import com.stripe.android.identity.navigation.IDUploadDestination
import com.stripe.android.identity.navigation.PassportScanDestination
import com.stripe.android.identity.navigation.PassportUploadDestination
import com.stripe.android.identity.navigation.SelfieDestination

/**
 * States during scanning a document.
 */
internal sealed class IdentityScanState(
    val type: ScanType,
    val transitioner: IdentityScanStateTransitioner,
    isFinal: Boolean
) : ScanState(isFinal) {

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
    internal abstract suspend fun consumeTransition(
        analyzerInput: AnalyzerInput,
        analyzerOutput: AnalyzerOutput
    ): IdentityScanState

    /**
     * Initial state when scan starts, no documents have been detected yet.
     */
    internal class Initial(
        type: ScanType,
        transitioner: IdentityScanStateTransitioner
    ) : IdentityScanState(type, transitioner, false) {
        /**
         * Only transitions to [Found] when ML output type matches scan type
         */
        override suspend fun consumeTransition(
            analyzerInput: AnalyzerInput,
            analyzerOutput: AnalyzerOutput
        ) =
            transitioner.transitionFromInitial(this, analyzerInput, analyzerOutput)
    }

    /**
     * State when scan has found the required type, the machine could stay in this state for a
     * while if more image needs to be processed to reach the next state.
     */
    internal class Found(
        type: ScanType,
        transitioner: IdentityScanStateTransitioner,
        internal var reachedStateAt: ClockMark = Clock.markNow()
    ) : IdentityScanState(type, transitioner, false) {
        override suspend fun consumeTransition(
            analyzerInput: AnalyzerInput,
            analyzerOutput: AnalyzerOutput
        ) =
            transitioner.transitionFromFound(this, analyzerInput, analyzerOutput)
    }

    /**
     * State when satisfaction checking passed.
     *
     * Note when Satisfied is reached, [timeoutAt] won't be checked.
     */
    internal class Satisfied(
        type: ScanType,
        transitioner: IdentityScanStateTransitioner,
        val reachedStateAt: ClockMark = Clock.markNow()
    ) : IdentityScanState(type, transitioner, false) {
        override suspend fun consumeTransition(
            analyzerInput: AnalyzerInput,
            analyzerOutput: AnalyzerOutput
        ) =
            transitioner.transitionFromSatisfied(this, analyzerInput, analyzerOutput)
    }

    /**
     * State when satisfaction checking failed.
     */
    internal class Unsatisfied(
        val reason: String,
        type: ScanType,
        transitioner: IdentityScanStateTransitioner,
        val reachedStateAt: ClockMark = Clock.markNow()
    ) : IdentityScanState(type, transitioner, false) {
        override suspend fun consumeTransition(
            analyzerInput: AnalyzerInput,
            analyzerOutput: AnalyzerOutput
        ) = transitioner.transitionFromUnsatisfied(this, analyzerInput, analyzerOutput)
    }

    /**
     * Terminal state, indicting the scan is finished.
     */
    internal class Finished(
        type: ScanType,
        transitioner: IdentityScanStateTransitioner
    ) : IdentityScanState(type, transitioner, true) {
        override suspend fun consumeTransition(
            analyzerInput: AnalyzerInput,
            analyzerOutput: AnalyzerOutput
        ) = this
    }

    /**
     * Terminal state, indicating the scan times out.
     */
    internal class TimeOut(
        type: ScanType,
        transitioner: IdentityScanStateTransitioner
    ) : IdentityScanState(type, transitioner, true) {
        override suspend fun consumeTransition(
            analyzerInput: AnalyzerInput,
            analyzerOutput: AnalyzerOutput
        ) = this
    }

    internal companion object {
        fun ScanType.isFront() =
            this == ScanType.ID_FRONT || this == ScanType.DL_FRONT || this == ScanType.PASSPORT

        fun ScanType.isBack() =
            this == ScanType.ID_BACK || this == ScanType.DL_BACK

        fun ScanType?.isNullOrFront() = this == null || this.isFront()

        fun ScanType.toUploadDestination() =
            when (this) {
                ScanType.ID_FRONT ->
                    IDUploadDestination(true)
                ScanType.ID_BACK ->
                    IDUploadDestination(true)
                ScanType.DL_FRONT ->
                    DriverLicenseUploadDestination(true)
                ScanType.DL_BACK ->
                    DriverLicenseUploadDestination(true)
                ScanType.PASSPORT ->
                    PassportUploadDestination(true)
                ScanType.SELFIE -> {
                    throw IllegalArgumentException("SELFIE doesn't support upload")
                }
            }

        fun ScanType.toScanDestination() =
            when (this) {
                ScanType.ID_FRONT ->
                    IDScanDestination(
                        shouldStartFromBack = false,
                        shouldPopUpToDocSelection = true
                    )
                ScanType.ID_BACK ->
                    IDScanDestination(
                        shouldStartFromBack = true,
                        shouldPopUpToDocSelection = true
                    )
                ScanType.DL_FRONT ->
                    DriverLicenseScanDestination(
                        shouldStartFromBack = false,
                        shouldPopUpToDocSelection = true
                    )
                ScanType.DL_BACK ->
                    DriverLicenseScanDestination(
                        shouldStartFromBack = true,
                        shouldPopUpToDocSelection = true
                    )
                ScanType.PASSPORT ->
                    PassportScanDestination(
                        shouldStartFromBack = false,
                        shouldPopUpToDocSelection = true
                    )
                ScanType.SELFIE ->
                    SelfieDestination
            }
    }
}
