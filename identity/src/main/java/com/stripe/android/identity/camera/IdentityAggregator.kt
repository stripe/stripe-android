package com.stripe.android.identity.camera

import com.stripe.android.camera.framework.AggregateResultListener
import com.stripe.android.camera.framework.ResultAggregator
import com.stripe.android.camera.framework.time.Clock
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieModels
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IDDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanStateTransitioner

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
            // TODO(ccen) Read params from VerificationPage
            FaceDetectorTransitioner(
                VerificationPageStaticContentSelfieCapturePage(
                    autoCaptureTimeout = 15000,
                    filePurpose = "selfie",
                    numSamples = 8,
                    sampleInterval = 200,
                    models = VerificationPageStaticContentSelfieModels(
                        faceDetectorUrl = "",
                        faceDetectorMinScore = 0.8f,
                        faceDetectorIou = 0.5f
                    ),
                    maxCenteredThresholdX = 0.2f,
                    maxCenteredThresholdY = 0.2f,
                    minEdgeThreshold = 0.05f,
                    minCoverageThreshold = 0.07f,
                    maxCoverageThreshold = 0.8f,
                    lowResImageMaxDimension = 800,
                    lowResImageCompressionQuality = 0.82f,
                    highResImageMaxDimension = 1440,
                    highResImageCompressionQuality = 0.92f,
                    highResImageCropPadding = 0.5f,
                    consentText = "consent"
                )
            )
        } else
            IDDetectorTransitioner(
                timeoutAt = Clock.markNow() + verificationPage.documentCapture.autocaptureTimeout.milliseconds,
                iouThreshold = verificationPage.documentCapture.motionBlurMinIou,
                timeRequired = verificationPage.documentCapture.motionBlurMinDuration
            )
    ),
    statsName = null
) {
    private var isFirstResultReceived = false

    internal data class InterimResult(
        val identityState: IdentityScanState
    )

    internal data class FinalResult(
        val frame: AnalyzerInput,
        val result: AnalyzerOutput,
        val identityState: IdentityScanState,
        val savedFrames: List<AnalyzerInput>?
    )

    override suspend fun aggregateResult(
        frame: AnalyzerInput,
        result: AnalyzerOutput
    ): Pair<InterimResult, FinalResult?> {
        if (isFirstResultReceived) {
            val previousState = state
            state = previousState.consumeTransition(frame, result)
            val interimResult = InterimResult(state)
            return interimResult to when (state) {
                is IdentityScanState.Finished -> {
                    FinalResult(
                        frame,
                        result,
                        state,
                        (state.transitioner as? FaceDetectorTransitioner)?.filteredFrames
                    )
                }
                is IdentityScanState.TimeOut -> {
                    FinalResult(
                        frame,
                        result,
                        state,
                        null
                    )
                }
                else -> {
                    null
                }
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
