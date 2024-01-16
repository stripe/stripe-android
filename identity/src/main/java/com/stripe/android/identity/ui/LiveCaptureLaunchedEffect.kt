package com.stripe.android.identity.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureDestination
import com.stripe.android.identity.navigation.navigateTo
import com.stripe.android.identity.networking.models.VerificationPage
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
    navController: NavController

) {
    LaunchedEffect(scannerState) {
        if (scannerState is IdentityScanViewModel.State.Scanned) {
            identityScanViewModel.stopScan(lifecycleOwner)

            val scanResult = scannerState.result
            identityViewModel.uploadScanResult(
                scanResult,
                verificationPage
            )
            when (val detectorOutput = scanResult.result) {
                is FaceDetectorOutput -> {
                    identityViewModel.updateAnalyticsState { oldState ->
                        oldState.copy(selfieModelScore = detectorOutput.resultScore)
                    }
                }
                is IDDetectorOutput -> {
                    if (scanResult.identityState.type.isFront()) {
                        identityViewModel.updateAnalyticsState { oldState ->
                            oldState.copy(
                                docFrontModelScore = detectorOutput.resultScore,
                                docFrontBlurScore = detectorOutput.blurScore
                            )
                        }
                    } else if (scanResult.identityState.type.isBack()) {
                        identityViewModel.updateAnalyticsState { oldState ->
                            oldState.copy(
                                docBackModelScore = detectorOutput.resultScore,
                                docBackBlurScore = detectorOutput.blurScore
                            )
                        }
                    }
                }
            }
        } else if (scannerState is IdentityScanViewModel.State.Timeout) {
            navController.navigateTo(
                CouldNotCaptureDestination(fromSelfie = scannerState.fromSelfie)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            identityScanViewModel.resetScannerState()
        }
    }
}
