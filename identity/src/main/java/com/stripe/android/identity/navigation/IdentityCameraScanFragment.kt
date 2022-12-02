package com.stripe.android.identity.navigation

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stripe.android.camera.CameraAdapter
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.camera.scanui.util.asRect
import com.stripe.android.core.exception.InvalidResponseException
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.TYPE_DOCUMENT
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.TYPE_SELFIE
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
import com.stripe.android.identity.navigation.CouldNotCaptureFragment.Companion.ARG_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.states.IdentityScanState.Companion.isBack
import com.stripe.android.identity.states.IdentityScanState.Companion.isFront
import com.stripe.android.identity.utils.navigateOnResume
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * An abstract [Fragment] class to access camera scanning for Identity.
 *
 * Subclasses are responsible for populating [cameraView] in its [Fragment.onCreateView] method.
 */
internal abstract class IdentityCameraScanFragment(
    private val identityCameraScanViewModelFactory: ViewModelProvider.Factory,
    private val identityViewModelFactory: ViewModelProvider.Factory
) : Fragment() {
    protected val identityScanViewModel: IdentityScanViewModel by viewModels { identityCameraScanViewModelFactory }
    protected val identityViewModel: IdentityViewModel by activityViewModels { identityViewModelFactory }

    @get:IdRes
    protected abstract val fragmentId: Int

    internal var cameraAdapter: CameraAdapter<CameraPreviewImage<Bitmap>>? = null
        set(value) {
            field = value
            value?.let {
                identityScanViewModel.cameraAdapterInitialized.postValue(true)
            }
        }

    /**
     * [CameraView] to initialize [CameraAdapter], subclasses needs to set its value in
     * [Fragment.onCreateView].
     */
    protected var cameraView: CameraView? = null

    /**
     * Called back at end of [onViewCreated] when permission is granted.
     */
    protected abstract fun onCameraReady()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        identityScanViewModel.interimResults.observe(viewLifecycleOwner) {
            identityViewModel.fpsTracker.trackFrame()
            if (it.identityState.isFinal) {
                stopScanning()
            }
        }

        identityScanViewModel.finalResult.observe(viewLifecycleOwner) { finalResult ->
            lifecycleScope.launch {
                identityViewModel.fpsTracker.reportAndReset(
                    if (finalResult.result is FaceDetectorOutput) TYPE_SELFIE else TYPE_DOCUMENT
                )
            }

            identityViewModel.observeForVerificationPage(
                viewLifecycleOwner,
                onSuccess = { verificationPage ->
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
                    } else if (finalResult.identityState is IdentityScanState.TimeOut) {
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
                        navigateOnResume(
                            R.id.action_global_couldNotCaptureFragment,
                            bundleOf(
                                ARG_COULD_NOT_CAPTURE_SCAN_TYPE to identityScanViewModel.targetScanTypeFlow.value
                            ).also {
                                if (identityScanViewModel.targetScanTypeFlow.value
                                    != IdentityScanState.ScanType.SELFIE
                                ) {
                                    it.putBoolean(
                                        ARG_REQUIRE_LIVE_CAPTURE,
                                        verificationPage.documentCapture.requireLiveCapture
                                    )
                                }
                            }
                        )
                    }
                },
                onFailure = {
                    Log.e(TAG, "Fail to observeForVerificationPage: $it")
                    navigateToDefaultErrorFragment(it)
                }
            )
        }

        identityViewModel.pageAndModelFiles.observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    requireNotNull(it.data).let { pageAndModelFiles ->
                        identityScanViewModel.initializeScanFlow(
                            pageAndModelFiles.page,
                            idDetectorModelFile = pageAndModelFiles.idDetectorFile,
                            faceDetectorModelFile = pageAndModelFiles.faceDetectorFile
                        )

                        identityScanViewModel.cameraAdapterInitialized.observe(viewLifecycleOwner) { isInitialized ->
                            if (isInitialized) {
                                viewLifecycleOwner.lifecycleScope.launch(identityViewModel.uiContext) {
                                    onCameraReady()
                                }
                            }
                        }
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
    }

    /**
     * Start scanning for the required scan type.
     */
    internal fun startScanning(scanType: IdentityScanState.ScanType) {
        identityViewModel.updateAnalyticsState { oldState ->
            when (scanType) {
                IdentityScanState.ScanType.ID_FRONT -> {
                    oldState.copy(
                        docFrontRetryTimes =
                        oldState.docFrontRetryTimes?.let { it + 1 } ?: 1
                    )
                }
                IdentityScanState.ScanType.ID_BACK -> {
                    oldState.copy(
                        docBackRetryTimes =
                        oldState.docBackRetryTimes?.let { it + 1 } ?: 1
                    )
                }
                IdentityScanState.ScanType.DL_FRONT -> {
                    oldState.copy(
                        docFrontRetryTimes =
                        oldState.docFrontRetryTimes?.let { it + 1 } ?: 1
                    )
                }
                IdentityScanState.ScanType.DL_BACK -> {
                    oldState.copy(
                        docBackRetryTimes =
                        oldState.docBackRetryTimes?.let { it + 1 } ?: 1
                    )
                }
                IdentityScanState.ScanType.PASSPORT -> {
                    oldState.copy(
                        docFrontRetryTimes =
                        oldState.docFrontRetryTimes?.let { it + 1 } ?: 1
                    )
                }
                IdentityScanState.ScanType.SELFIE -> {
                    oldState.copy(
                        selfieRetryTimes =
                        oldState.selfieRetryTimes?.let { it + 1 } ?: 1
                    )
                }
            }
        }
        identityScanViewModel.targetScanTypeFlow.update { scanType }
        requireNotNull(cameraAdapter).bindToLifecycle(this)
        identityScanViewModel.scanState = null
        identityScanViewModel.scanStatePrevious = null

        identityViewModel.fpsTracker.start()
        identityScanViewModel.identityScanFlow?.startFlow(
            context = requireContext(),
            imageStream = requireNotNull(cameraAdapter).getImageStream(),
            viewFinder = requireNotNull(cameraView).viewFinderWindowView.asRect(),
            lifecycleOwner = viewLifecycleOwner,
            coroutineScope = lifecycleScope,
            parameters = scanType
        )
    }

    /**
     * Stop scanning, may start again later.
     */
    private fun stopScanning() {
        identityScanViewModel.identityScanFlow?.resetFlow()
        cameraAdapter?.unbindFromLifecycle(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Cancelling IdentityScanFlow")
        identityScanViewModel.identityScanFlow?.cancelFlow()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // When the fragment is navigated away from, clean up stale displayStateChangedFlow states.
        identityScanViewModel.displayStateChangedFlow.update { null }
        cameraView = null
        cameraAdapter = null
    }

    internal companion object {
        private val TAG: String = IdentityCameraScanFragment::class.java.simpleName
        val MINIMUM_RESOLUTION = Size(1440, 1080)
    }
}
