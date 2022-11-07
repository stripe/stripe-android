package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Type
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.DocSelectionScreen
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.navigateToUploadFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.launch

/**
 * Screen to select type of ID to scan.
 */
internal class DocSelectionFragment(
    private val identityViewModelFactory: ViewModelProvider.Factory,
    private val cameraPermissionEnsureable: CameraPermissionEnsureable
) : Fragment() {

    private val identityViewModel: IdentityViewModel by activityViewModels {
        identityViewModelFactory
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val verificationPage by identityViewModel.verificationPage.observeAsState()
            DocSelectionScreen(verificationPage, ::postVerificationPageDataAndNavigate)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch(identityViewModel.workContext) {
            identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_DOC_SELECT)
        }
        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = SCREEN_NAME_DOC_SELECT
            )
        )
    }

    /**
     * Post VerificationPageData with the type and navigate base on its result.
     */
    private fun postVerificationPageDataAndNavigate(type: Type) {
        lifecycleScope.launch {
            postVerificationPageDataAndMaybeSubmit(
                identityViewModel = identityViewModel,
                collectedDataParam = CollectedDataParam(idDocumentType = type),
                fromFragment = R.id.docSelectionFragment,
                notSubmitBlock = {
                    cameraPermissionEnsureable.ensureCameraPermission(
                        onCameraReady = {
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory.cameraPermissionGranted(
                                    type.toAnalyticsScanType()
                                )
                            )
                            identityViewModel.idDetectorModelFile.observe(viewLifecycleOwner) { modelResource ->
                                when (modelResource.status) {
                                    // model ready, camera permission is granted -> navigate to scan
                                    Status.SUCCESS -> {
                                        findNavController().navigate(type.toScanDestinationId())
                                    }
                                    // model not ready, camera permission is granted -> navigate to manual capture
                                    Status.ERROR -> {
                                        tryNavigateToUploadFragment(type)
                                    }
                                    Status.LOADING -> {} // no-op
                                }
                            }
                        },
                        onUserDeniedCameraPermission = {
                            identityViewModel.sendAnalyticsRequest(
                                identityViewModel.identityAnalyticsRequestFactory.cameraPermissionDenied(
                                    type.toAnalyticsScanType()
                                )
                            )
                            tryNavigateToCameraPermissionDeniedFragment(type)
                        }
                    )
                }
            )
        }
    }

    /**
     * Navigate to the corresponding type's upload fragment, or to [ErrorFragment]
     * if required data is not available.
     */
    private fun tryNavigateToUploadFragment(type: Type) {
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = { verificationPage ->
                if (verificationPage.documentCapture.requireLiveCapture) {
                    "Can't access camera and client has required live capture.".let { msg ->
                        Log.e(TAG, msg)
                        navigateToDefaultErrorFragment(msg)
                    }
                } else {
                    navigateToUploadFragment(
                        type.toUploadDestinationId(),
                        shouldShowTakePhoto = true,
                        shouldShowChoosePhoto = true
                    )
                }
            },
            onFailure = {
                navigateToDefaultErrorFragment(it)
            }
        )
    }

    /**
     * Navigate to [CameraPermissionDeniedFragment], or to [ErrorFragment]
     * if requireLiveCapture is true.
     */
    private fun tryNavigateToCameraPermissionDeniedFragment(type: Type) {
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = { verificationPage ->
                findNavController().navigate(
                    R.id.action_camera_permission_denied,
                    if (verificationPage.documentCapture.requireLiveCapture) {
                        null
                    } else {
                        bundleOf(
                            CameraPermissionDeniedFragment.ARG_SCAN_TYPE to type
                        )
                    }
                )
            },
            onFailure = {
                Log.e(TAG, "failed to observeForVerificationPage: $it")
                navigateToDefaultErrorFragment(it)
            }
        )
    }

    internal companion object {
        const val PASSPORT_KEY = "passport"
        const val DRIVING_LICENSE_KEY = "driving_license"
        const val ID_CARD_KEY = "id_card"
        const val SELECTION_NONE = ""
        val TAG: String = DocSelectionFragment::class.java.simpleName

        @IdRes
        private fun Type.toScanDestinationId() =
            when (this) {
                Type.IDCARD -> R.id.action_docSelectionFragment_to_IDScanFragment
                Type.PASSPORT -> R.id.action_docSelectionFragment_to_passportScanFragment
                Type.DRIVINGLICENSE -> R.id.action_docSelectionFragment_to_driverLicenseScanFragment
            }

        @IdRes
        private fun Type.toUploadDestinationId() =
            when (this) {
                Type.IDCARD -> R.id.action_docSelectionFragment_to_IDUploadFragment
                Type.PASSPORT -> R.id.action_docSelectionFragment_to_passportUploadFragment
                Type.DRIVINGLICENSE -> R.id.action_docSelectionFragment_to_driverLicenseUploadFragment
            }

        private fun Type.toAnalyticsScanType() = when (this) {
            Type.DRIVINGLICENSE -> IdentityScanState.ScanType.DL_FRONT
            Type.IDCARD -> IdentityScanState.ScanType.ID_FRONT
            Type.PASSPORT -> IdentityScanState.ScanType.PASSPORT
        }
    }
}
