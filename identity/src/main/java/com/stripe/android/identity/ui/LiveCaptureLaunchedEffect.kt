package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState.Companion.isBack
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Effect to notify [IdentityViewModel] based on [IdentityScanViewModel.State] and reset
 * [IdentityScanViewModel.scannerState].
 * TODO(IDPROD-6662) - decouple any logic related to IdentityViewModel and handle them inside IdentityScanViewModel.
 */
@Composable
internal fun LiveCaptureLaunchedEffect(
    scannerState: IdentityScanViewModel.State,
    identityScanViewModel: IdentityScanViewModel,
    identityViewModel: IdentityViewModel,
    lifecycleOwner: LifecycleOwner,
    verificationPage: VerificationPage,
    navController: NavController,
    cameraManager: IdentityCameraManager? = null
) {
    LaunchedEffect(scannerState) {
        when (scannerState) {
            is IdentityScanViewModel.State.Scanned -> {
                handleScannedState(
                    scannerState = scannerState,
                    identityScanViewModel = identityScanViewModel,
                    identityViewModel = identityViewModel,
                    lifecycleOwner = lifecycleOwner,
                    verificationPage = verificationPage,
                    navController = navController,
                    cameraManager = cameraManager
                )
            }
            is IdentityScanViewModel.State.Timeout -> {
                handleTimeoutState(
                    scannerState = scannerState,
                    identityViewModel = identityViewModel,
                    navController = navController
                )
            }
            else -> Unit
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            identityScanViewModel.resetScannerState()
        }
    }
}

private suspend fun handleScannedState(
    scannerState: IdentityScanViewModel.State.Scanned,
    identityScanViewModel: IdentityScanViewModel,
    identityViewModel: IdentityViewModel,
    lifecycleOwner: LifecycleOwner,
    verificationPage: VerificationPage,
    navController: NavController,
    cameraManager: IdentityCameraManager?
) {
    val scanResult = scannerState.result
    val detectorOutput = scanResult.result
    val isSelfieResult = detectorOutput is FaceDetectorOutput

    if (isSelfieResult) {
        identityViewModel.setSelfieIsVirtualCamera(cameraManager?.isVirtualCamera())
    }
    identityScanViewModel.stopScan(lifecycleOwner)
    identityViewModel.setCameraMetadata(cameraManager)

    if (isSelfieResult) {
        identityViewModel.clearSelfieUploadedState()
    }
    identityViewModel.uploadScanResult(scanResult, verificationPage)

    when (detectorOutput) {
        is FaceDetectorOutput -> {
            identityViewModel.handleFaceDetectorOutput(detectorOutput, scanResult, navController)
        }
        is IDDetectorOutput -> {
            identityViewModel.handleIdDetectorOutput(detectorOutput, scanResult)
        }
    }
}

private fun IdentityViewModel.setCameraMetadata(cameraManager: IdentityCameraManager?) {
    cameraManager?.getCameraLensModel()?.let { setCameraLensModel(it) }
    cameraManager?.getExposureIso()?.let { setCameraExposureIso(it) }
    cameraManager?.getFocalLength()?.let { setCameraFocalLength(it) }
    cameraManager?.getExposureDuration()?.let { setCameraExposureDuration(it) }
    cameraManager?.isVirtualCamera()?.let { setIsVirtualCamera(it) }
}

private suspend fun IdentityViewModel.handleFaceDetectorOutput(
    detectorOutput: FaceDetectorOutput,
    scanResult: IdentityAggregator.FinalResult,
    navController: NavController
) {
    updateAnalyticsState { oldState ->
        oldState.copy(selfieModelScore = detectorOutput.resultScore)
    }
    (scanResult.identityState.transitioner as? FaceDetectorTransitioner)?.let {
        collectDataForSelfieScreen(
            navController = navController,
            faceDetectorTransitioner = it
        )
    }
}

private fun IdentityViewModel.handleIdDetectorOutput(
    detectorOutput: IDDetectorOutput,
    scanResult: IdentityAggregator.FinalResult
) {
    if (scanResult.identityState.type.isFront()) {
        updateAnalyticsState { oldState ->
            oldState.copy(
                docFrontModelScore = detectorOutput.resultScore,
                docFrontBlurScore = detectorOutput.blurScore
            )
        }
    } else if (scanResult.identityState.type.isBack()) {
        updateAnalyticsState { oldState ->
            oldState.copy(
                docBackModelScore = detectorOutput.resultScore,
                docBackBlurScore = detectorOutput.blurScore
            )
        }
    }
}

private fun handleTimeoutState(
    scannerState: IdentityScanViewModel.State.Timeout,
    identityViewModel: IdentityViewModel,
    navController: NavController
) {
    identityViewModel.screenTracker.screenTransitionStart(
        identityViewModel.analyticsLastScreenName ?: if (scannerState.fromSelfie) {
            IdentityAnalyticsRequestFactory.SCREEN_NAME_SELFIE
        } else {
            IdentityAnalyticsRequestFactory.SCREEN_NAME_LIVE_CAPTURE
        }
    )
    navController.navigateTo(
        CouldNotCaptureDestination(fromSelfie = scannerState.fromSelfie)
    )
}
