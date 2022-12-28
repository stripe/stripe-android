package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.CameraXAdapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.SelfieScanScreen
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment to capture selfie.
 */
internal class SelfieFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(identityCameraScanViewModelFactory, identityViewModelFactory) {
    override val fragmentId = R.id.selfieFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val changedDisplayState by identityScanViewModel.displayStateChangedFlow.collectAsState()
            val newDisplayState by remember {
                derivedStateOf {
                    changedDisplayState?.first
                }
            }
            val verificationPage by identityViewModel.verificationPage.observeAsState(Resource.loading())
            SelfieScanScreen(
                title = stringResource(id = R.string.selfie_captures),
                message = when (newDisplayState) {
                    is IdentityScanState.Finished ->
                        stringResource(id = R.string.selfie_capture_complete)
                    is IdentityScanState.Found ->
                        stringResource(id = R.string.capturing)
                    is IdentityScanState.Initial ->
                        stringResource(id = R.string.position_selfie)
                    is IdentityScanState.Satisfied ->
                        stringResource(id = R.string.selfie_capture_complete)
                    is IdentityScanState.TimeOut -> ""
                    is IdentityScanState.Unsatisfied -> ""
                    null -> {
                        stringResource(id = R.string.position_selfie)
                    }
                },
                newDisplayState = newDisplayState,
                verificationPageState = verificationPage,
                onError = { navigateToDefaultErrorFragment(it, identityViewModel) },
                onCameraViewCreated = {
                    if (cameraView == null) {
                        cameraView = it
                        cameraAdapter = createCameraAdapter()
                    }
                },
                onContinueClicked = { allowImageCollection ->
                    collectUploadedStateAndUploadForCollectedSelfies(allowImageCollection)
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        identityViewModel.resetSelfieUploadedState()

        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = {
                lifecycleScope.launch(identityViewModel.workContext) {
                    identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_SELFIE)
                }
                identityViewModel.sendAnalyticsRequest(
                    identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                        screenName = SCREEN_NAME_SELFIE
                    )
                )
            }
        )
    }

    private fun createCameraAdapter() = CameraXAdapter(
        requireNotNull(activity),
        requireNotNull(cameraView).previewFrame,
        MINIMUM_RESOLUTION,
        DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
            Log.e(TAG, "scan fails with exception: $cause")
            identityViewModel.sendAnalyticsRequest(
                identityViewModel.identityAnalyticsRequestFactory.cameraError(
                    scanType = IdentityScanState.ScanType.SELFIE,
                    throwable = IllegalStateException(cause)
                )
            )
        },
        startWithBackCamera = false
    )

    override fun onCameraReady() {
        startScanning(IdentityScanState.ScanType.SELFIE)
    }

    /**
     * Collect the [IdentityViewModel.selfieUploadState] and update UI accordingly.
     *
     * Try to [postVerificationPageDataAndMaybeSubmit] when all images are uploaded and navigates
     * to error when error occurs.
     */
    @VisibleForTesting
    internal fun collectUploadedStateAndUploadForCollectedSelfies(allowImageCollection: Boolean) =
        lifecycleScope.launch {
            identityViewModel.selfieUploadState.collectLatest {
                when {
                    it.isIdle() -> {} // no-op
                    it.isAnyLoading() -> {} // no-op
                    it.hasError() -> {
                        "Fail to upload files: ${it.getError()}".let { msg ->
                            Log.e(TAG, msg)
                            navigateToDefaultErrorFragment(msg, identityViewModel)
                        }
                    }
                    it.isAllUploaded() -> {
                        runCatching {
                            val faceDetectorTransitioner =
                                requireNotNull(
                                    identityScanViewModel.finalResult
                                        .value?.identityState?.transitioner
                                        as? FaceDetectorTransitioner
                                ) {
                                    "Failed to retrieve final result for Selfie"
                                }
                            postVerificationPageDataAndMaybeSubmit(
                                identityViewModel = identityViewModel,
                                collectedDataParam = CollectedDataParam.createForSelfie(
                                    firstHighResResult = requireNotNull(it.firstHighResResult.data),
                                    firstLowResResult = requireNotNull(it.firstLowResResult.data),
                                    lastHighResResult = requireNotNull(it.lastHighResResult.data),
                                    lastLowResResult = requireNotNull(it.lastLowResResult.data),
                                    bestHighResResult = requireNotNull(it.bestHighResResult.data),
                                    bestLowResResult = requireNotNull(it.bestLowResResult.data),
                                    trainingConsent = allowImageCollection,
                                    faceScoreVariance = faceDetectorTransitioner.scoreVariance,
                                    bestFaceScore = faceDetectorTransitioner.bestFaceScore,
                                    numFrames = faceDetectorTransitioner.numFrames
                                ),
                                fromFragment = fragmentId
                            )
                        }.onFailure { throwable ->
                            Log.e(
                                TAG,
                                "fail to submit uploaded files: $throwable"
                            )
                            navigateToDefaultErrorFragment(throwable, identityViewModel)
                        }
                    }
                    else -> {
                        (
                            "collectUploadedStateAndUploadForCollectedSelfies " +
                                "reaches unexpected upload state: $it"
                            ).let { msg ->
                            Log.e(TAG, msg)
                            navigateToDefaultErrorFragment(msg, identityViewModel)
                        }
                    }
                }
            }
        }

    internal companion object {
        private val TAG: String = SelfieFragment::class.java.simpleName
    }
}
