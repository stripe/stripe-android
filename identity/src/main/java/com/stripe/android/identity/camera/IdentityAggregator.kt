package com.stripe.android.identity.camera

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IDDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanStateTransitioner
import kotlin.time.Duration.Companion.milliseconds

/**
 * [ResultAggregator] for Identity.
 *
 * Initialize the [IdentityScanState.Initial] state with corresponding
 * [IdentityScanStateTransitioner] based on [IdentityScanState.ScanType].
 */
internal class IdentityAggregator(
    identityScanType: IdentityScanState.ScanType,
    aggregateResultListener: AggregateResultListener<InterimResult, FinalResult>,
    verificationPage: VerificationPage
) : ResultAggregator<
    AnalyzerInput,
    IdentityScanState,
    AnalyzerOutput,
    IdentityAggregator.InterimResult,
    IdentityAggregator.FinalResult
    >(
    aggregateResultListener,
    IdentityScanState.Initial(
        type = identityScanType,
        transitioner =
        if (identityScanType == IdentityScanState.ScanType.SELFIE) {
            FaceDetectorTransitioner(
                requireNotNull(verificationPage.selfieCapture) {
                    "Failed to initialize FaceDetectorTransitioner - " +
                        "verificationPage.selfieCapture is null."
                }
            )
        } else {
            IDDetectorTransitioner(
                timeout = verificationPage.documentCapture.autocaptureTimeout.milliseconds,
                iouThreshold = verificationPage.documentCapture.motionBlurMinIou,
                timeRequired = verificationPage.documentCapture.motionBlurMinDuration,
                blurThreshold = verificationPage.documentCapture.blurThreshold
                    ?: IDDetectorTransitioner.DEFAULT_BLUR_THRESHOLD
            )
        }
    )
) {
    private var isFirstResultReceived = false

    internal data class InterimResult(
        val identityState: IdentityScanState
    )

    internal data class FinalResult(
        val frame: AnalyzerInput,
        val result: AnalyzerOutput,
        val identityState: IdentityScanState
    )

    override suspend fun aggregateResult(
        frame: AnalyzerInput,
        result: AnalyzerOutput
    ): Pair<InterimResult, FinalResult?> {
        if (isFirstResultReceived) {
            val previousState = state
            state = previousState.consumeTransition(frame, result)
            val interimResult = InterimResult(state)
            return interimResult to
                if (state.isFinal) {
                    FinalResult(
                        frame,
                        result,
                        state
                    )
                } else {
                    null
                }
        } else {
            // If this is the very first result, don't transition state and post InterimResult with
            // current state(IdentityScanState.Initial).
            // This makes sure the receiver always receives IdentityScanState.Initial as the first
            // callback.
            isFirstResultReceived = true
            return InterimResult(state) to null
        }
    }
}
