package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isBack
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * A LaunchedEffect to subscribe for the following events from [IdentityViewModel] and
 * [IdentityScanViewModel] for camera scanning.
 *  * Initialize identityScanFlow when pageAndModelFiles are ready
 *  * Track fps when an interim result is available
 *  * Process final result when one is available
 *
 * TODO(ccen): These logics were inside Fragment.onViewCreated before migrated to Jetpack Compose.
 *   They should be encapsulated within [IdentityScanViewModel] with corresponding events from
 *   compose views.
 */
@Composable
internal fun CameraScreenLaunchedEffect(
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
    verificationPage: VerificationPage,
    navController: NavController,
    cameraManager: IdentityCameraManager,
    onCameraReady: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(identityScanViewModel) {
        // Initialize identityScanFlow
        identityViewModel.pageAndModelFiles.observe(lifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    requireNotNull(it.data).let { pageAndModelFiles ->
                        identityScanViewModel.initializeScanFlow(
                            pageAndModelFiles.page,
                            idDetectorModelFile = pageAndModelFiles.idDetectorFile,
                            faceDetectorModelFile = pageAndModelFiles.faceDetectorFile
                        )
                        identityScanViewModel.initializeCameraManager(cameraManager)
                        onCameraReady()
                    }
                }
                Status.LOADING -> {} // no-op
                Status.IDLE -> {} // no-op
                Status.ERROR -> {
                    throw InvalidResponseException(
                        cause = it.throwable,
                        message = it.message
                    )
                }
            }
        }

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
                                oldState.copy(docFrontModelScore = finalResult.result.resultScore)
                            }
                        } else if (finalResult.identityState.type.isBack()) {
                            identityViewModel.updateAnalyticsState { oldState ->
                                oldState.copy(docBackModelScore = finalResult.result.resultScore)
                            }
                        }
                    }
                }
                identityViewModel.uploadScanResult(
                    finalResult,
                    verificationPage,
                    identityScanViewModel.targetScanTypeFlow.value
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
                        scanType = requireNotNull(identityScanViewModel.targetScanTypeFlow.value),
                        requireLiveCapture =
                        if (identityScanViewModel.targetScanTypeFlow.value
                            != IdentityScanState.ScanType.SELFIE
                        ) {
                            verificationPage.documentCapture.requireLiveCapture
                        } else {
                            false
                        }
                    )
                )
            }
        }
    }
}
