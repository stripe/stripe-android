package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isBack
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * [CameraScreenLaunchedEffect] without checking pageAndModelFiles from IdentityViewModel.
 */
@Composable
internal fun CameraScreenLaunchedEffectLight(
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    verificationPage: VerificationPage,
    navController: NavController
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(identityScanViewModel) {
        // Handles interim result - track FPS
        identityScanViewModel.interimResults.observe(lifecycleOwner) {
            identityViewModel.fpsTracker.trackFrame()
        }

        // Handles final result - upload if success, transition to CouldNotCapture if timeout
        identityScanViewModel.finalResult.observe(lifecycleOwner) { finalResult ->
            lifecycleOwner.lifecycleScope.launch {
                identityViewModel.fpsTracker.reportAndReset(
                    if (finalResult.result is FaceDetectorOutput) {
                        IdentityAnalyticsRequestFactory.TYPE_SELFIE
                    } else {
                        IdentityAnalyticsRequestFactory.TYPE_DOCUMENT
                    }
                )
            }

            // Upload success result
            if (finalResult.identityState is IdentityScanState.Finished) {
                when (finalResult.result) {
                    is FaceDetectorOutput -> {
                        identityViewModel.updateAnalyticsState { oldState ->
                            oldState.copy(selfieModelScore = finalResult.result.resultScore)
                        }
                    }
                    is IDDetectorOutput -> {
                        if (finalResult.identityState.type.isFront()) {
                            identityViewModel.updateAnalyticsState { oldState ->
                                oldState.copy(
                                    docFrontModelScore = finalResult.result.resultScore,
                                    docFrontBlurScore = finalResult.result.blurScore
                                )
                            }
                        } else if (finalResult.identityState.type.isBack()) {
                            identityViewModel.updateAnalyticsState { oldState ->
                                oldState.copy(
                                    docBackModelScore = finalResult.result.resultScore,
                                    docBackBlurScore = finalResult.result.blurScore
                                )
                            }
                        }
                    }
                }
                identityViewModel.uploadScanResult(
                    finalResult,
                    verificationPage
                )
            }
            // Transition to CouldNotCaptureDestination
            else if (finalResult.identityState is IdentityScanState.TimeOut) {
                when (finalResult.result) {
                    is FaceDetectorOutput -> {
                        identityViewModel.sendAnalyticsRequest(
                            identityViewModel.identityAnalyticsRequestFactory.selfieTimeout()
                        )
                    }
                    is IDDetectorOutput -> {
                        identityViewModel.sendAnalyticsRequest(
                            identityViewModel.identityAnalyticsRequestFactory.documentTimeout(
                                scanType = finalResult.identityState.type
                            )
                        )
                    }
                }

                navController.navigateTo(
                    CouldNotCaptureDestination(
                        fromSelfie = finalResult.result is FaceDetectorOutput
                    )
                )
            }
        }
    }
}
