package com.stripe.android.identity.navigation

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.CameraXAdapter
import com.stripe.android.camera.DefaultCameraErrorListener
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.startAnimation
import com.stripe.android.camera.scanui.util.startAnimationIfNotRunning
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingBack
import com.stripe.android.identity.networking.models.VerificationPageData.Companion.isMissingSelfie
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.states.IdentityScanState.Companion.isNullOrFront
import com.stripe.android.identity.ui.DocumentScanScreen
import com.stripe.android.identity.utils.fragmentIdToScreenName
import com.stripe.android.identity.utils.navigateOnResume
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.postVerificationPageData
import com.stripe.android.identity.utils.submitVerificationPageDataAndNavigate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Fragment for scanning ID, Passport and Driver's license
 */
internal abstract class IdentityDocumentScanFragment(
    identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    identityViewModelFactory: ViewModelProvider.Factory
) : IdentityCameraScanFragment(
    identityCameraScanViewModelFactory,
    identityViewModelFactory
) {
    abstract val frontScanType: IdentityScanState.ScanType
    abstract val backScanType: IdentityScanState.ScanType?

    @get:StringRes
    abstract val frontTitleStringRes: Int

    @get:StringRes
    abstract val backTitleStringRes: Int

    @get:StringRes
    abstract val frontMessageStringRes: Int

    @get:StringRes
    abstract val backMessageStringRes: Int

    abstract val collectedDataParamType: CollectedDataParam.Type

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
            val targetScanType by identityScanViewModel.targetScanTypeFlow.collectAsState()

            DocumentScanScreen(
                title =
                if (targetScanType.isNullOrFront()) {
                    stringResource(id = frontTitleStringRes)
                } else {
                    stringResource(id = backTitleStringRes)
                },
                message = when (newDisplayState) {
                    is IdentityScanState.Finished -> stringResource(id = R.string.scanned)
                    is IdentityScanState.Found -> stringResource(id = R.string.hold_still)
                    is IdentityScanState.Initial -> {
                        if (targetScanType.isNullOrFront()) {
                            stringResource(id = frontMessageStringRes)
                        } else {
                            stringResource(id = backMessageStringRes)
                        }
                    }

                    is IdentityScanState.Satisfied -> stringResource(id = R.string.scanned)
                    is IdentityScanState.TimeOut -> ""
                    is IdentityScanState.Unsatisfied -> ""
                    null -> {
                        if (targetScanType.isNullOrFront()) {
                            stringResource(id = frontMessageStringRes)
                        } else {
                            stringResource(id = backMessageStringRes)
                        }
                    }
                },
                newDisplayState = newDisplayState,
                onCameraViewCreated = {
                    if (cameraView == null) {
                        cameraView = it
                        requireNotNull(cameraView)
                            .viewFinderWindowView
                            .setBackgroundResource(
                                R.drawable.viewfinder_background
                            )
                        cameraAdapter = createCameraAdapter()
                    }
                },
                onContinueClicked = {
                    collectDocumentUploadedStateAndPost(
                        collectedDataParamType,
                        requireNotNull(targetScanType) {
                            "targetScanType is still null"
                        }.isFront()
                    )
                }
            )
            LaunchedEffect(newDisplayState) {
                when (newDisplayState) {
                    null -> {
                        requireNotNull(cameraView).toggleInitial()
                    }
                    is IdentityScanState.Initial -> {
                        requireNotNull(cameraView).toggleInitial()
                    }
                    is IdentityScanState.Found -> {
                        requireNotNull(cameraView).toggleFound()
                    }
                    is IdentityScanState.Finished -> {
                        requireNotNull(cameraView).toggleFinished()
                    }
                    else -> {} // no-op
                }
            }
        }
    }

    private fun CameraView.toggleInitial() {
        viewFinderBackgroundView.visibility = View.VISIBLE
        viewFinderWindowView.visibility = View.VISIBLE
        viewFinderBorderView.visibility = View.VISIBLE
        viewFinderBorderView.startAnimation(R.drawable.viewfinder_border_initial)
    }

    private fun CameraView.toggleFound() {
        viewFinderBorderView.startAnimationIfNotRunning(R.drawable.viewfinder_border_found)
    }

    private fun CameraView.toggleFinished() {
        viewFinderBackgroundView.visibility = View.INVISIBLE
        viewFinderWindowView.visibility = View.INVISIBLE
        viewFinderBorderView.visibility = View.INVISIBLE
        viewFinderBorderView.startAnimation(R.drawable.viewfinder_border_initial)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!shouldStartFromBack()) {
            identityViewModel.resetDocumentUploadedState()
        }
        super.onViewCreated(view, savedInstanceState)
        identityViewModel.observeForVerificationPage(
            this,
            onSuccess = {
                lifecycleScope.launch(identityViewModel.workContext) {
                    identityViewModel.screenTracker.screenTransitionFinish(fragmentId.fragmentIdToScreenName())
                }
                identityViewModel.sendAnalyticsRequest(
                    identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                        scanType = frontScanType,
                        screenName = fragmentId.fragmentIdToScreenName()
                    )
                )
            }
        )
    }

    /**
     * Check if should start scanning from back.
     */
    private fun shouldStartFromBack(): Boolean =
        arguments?.getBoolean(ARG_SHOULD_START_FROM_BACK) == true

    override fun onCameraReady() {
        if (shouldStartFromBack()) {
            startScanning(
                requireNotNull(backScanType) {
                    "$backScanType should not be null when trying to scan from back"
                }
            )
        } else {
            startScanning(frontScanType)
        }
    }

    private fun createCameraAdapter(): CameraAdapter<CameraPreviewImage<Bitmap>> {
        return CameraXAdapter(
            requireNotNull(activity),
            requireNotNull(cameraView).previewFrame,
            MINIMUM_RESOLUTION,
            DefaultCameraErrorListener(requireNotNull(activity)) { cause ->
                Log.e(TAG, "scan fails with exception: $cause")
                identityViewModel.sendAnalyticsRequest(
                    identityViewModel.identityAnalyticsRequestFactory.cameraError(
                        scanType = frontScanType,
                        throwable = IllegalStateException(cause)
                    )
                )
            }
        )
    }

    /**
     * Check the upload status of the document, post it with VerificationPageData, and decide
     * next step based on result.
     *
     * If result is missing back, then start scanning back of the document,
     * else if result is missing selfie, then start scanning selfie,
     * Otherwise submit
     */
    @VisibleForTesting
    internal fun collectDocumentUploadedStateAndPost(
        type: CollectedDataParam.Type,
        isFront: Boolean
    ) = viewLifecycleOwner.lifecycleScope.launch {
        if (isFront) {
            identityViewModel.documentFrontUploadedState
        } else {
            identityViewModel.documentBackUploadedState
        }.collectLatest { uploadedState ->
            if (uploadedState.hasError()) {
                navigateToDefaultErrorFragment(uploadedState.getError(), identityViewModel)
            } else if (uploadedState.isUploaded()) {
                identityViewModel.observeForVerificationPage(
                    viewLifecycleOwner,
                    onSuccess = {
                        viewLifecycleOwner.lifecycleScope.launch {
                            runCatching {
                                postVerificationPageData(
                                    identityViewModel = identityViewModel,
                                    collectedDataParam =
                                    if (isFront) {
                                        CollectedDataParam.createFromFrontUploadedResultsForAutoCapture(
                                            type = type,
                                            frontHighResResult = requireNotNull(uploadedState.highResResult.data),
                                            frontLowResResult = requireNotNull(uploadedState.lowResResult.data)
                                        )
                                    } else {
                                        CollectedDataParam.createFromBackUploadedResultsForAutoCapture(
                                            type = type,
                                            backHighResResult = requireNotNull(uploadedState.highResResult.data),
                                            backLowResResult = requireNotNull(uploadedState.lowResResult.data)
                                        )
                                    },
                                    fromFragment = fragmentId
                                ) { verificationPageDataWithNoError ->
                                    if (verificationPageDataWithNoError.isMissingBack()) {
                                        startScanning(
                                            requireNotNull(backScanType) {
                                                "backScanType is null while still missing back"
                                            }
                                        )
                                    } else if (verificationPageDataWithNoError.isMissingSelfie()) {
                                        navigateOnResume(SelfieDestination)
                                    } else {
                                        submitVerificationPageDataAndNavigate(
                                            identityViewModel,
                                            fragmentId
                                        )
                                    }
                                }
                            }.onFailure { throwable ->
                                Log.e(
                                    TAG,
                                    "fail to submit uploaded files: $throwable"
                                )
                                navigateToDefaultErrorFragment(throwable, identityViewModel)
                            }
                        }
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Fail to observeForVerificationPage: $throwable")
                        navigateToDefaultErrorFragment(throwable, identityViewModel)
                    }
                )
            }
        }
    }

    internal companion object {
        const val ARG_SHOULD_START_FROM_BACK = "startFromBack"
        private val TAG: String = IdentityDocumentScanFragment::class.java.simpleName
    }
}
