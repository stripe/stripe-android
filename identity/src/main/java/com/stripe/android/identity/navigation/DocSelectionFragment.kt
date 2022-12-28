package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Type
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.DocSelectionScreen
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
            val verificationPage by identityViewModel.verificationPage.observeAsState(Resource.loading())
            DocSelectionScreen(
                verificationPage,
                onError = {
                    identityViewModel.errorCause.postValue(it)
                    findNavController().navigateToErrorScreenWithDefaultValues(requireContext())
                },
                onComposeFinish = {
                    lifecycleScope.launch(identityViewModel.workContext) {
                        identityViewModel.screenTracker.screenTransitionFinish(
                            SCREEN_NAME_DOC_SELECT
                        )
                    }
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                            screenName = SCREEN_NAME_DOC_SELECT
                        )
                    )
                }
            ) { type ->
                val navController = findNavController()
                identityViewModel.postVerificationPageDataAndMaybeNavigate(
                    navController,
                    CollectedDataParam(idDocumentType = type),
                    fromRoute = DocSelectionDestination.ROUTE.route,
                    onMissingFront = {
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
                                            navController.navigateTo(type.toScanDestination())
                                        }
                                        // model not ready, camera permission is granted -> navigate to manual capture
                                        Status.ERROR -> {
                                            identityViewModel.requireVerificationPage(
                                                viewLifecycleOwner,
                                                navController
                                            ) { verificationPage ->
                                                if (verificationPage.documentCapture.requireLiveCapture) {
                                                    identityViewModel.errorCause.postValue(
                                                        IllegalStateException(
                                                            "Can't access camera and client has " +
                                                                "required live capture."
                                                        )
                                                    )
                                                    navController.navigateToErrorScreenWithDefaultValues(
                                                        requireContext(),
                                                    )
                                                } else {
                                                    navController.navigateTo(
                                                        type.toUploadDestination(
                                                            shouldShowTakePhoto = true,
                                                            shouldShowChoosePhoto = true
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        Status.LOADING -> {} // no-op
                                        Status.IDLE -> {} // no-op
                                    }
                                }
                            },
                            onUserDeniedCameraPermission = {
                                identityViewModel.sendAnalyticsRequest(
                                    identityViewModel.identityAnalyticsRequestFactory.cameraPermissionDenied(
                                        type.toAnalyticsScanType()
                                    )
                                )
                                identityViewModel.requireVerificationPage(
                                    viewLifecycleOwner,
                                    navController
                                ) { verificationPage ->
                                    navController.navigateTo(
                                        CameraPermissionDeniedDestination(
                                            if (verificationPage.documentCapture.requireLiveCapture) {
                                                null
                                            } else {
                                                type
                                            }
                                        )
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    internal companion object {
        const val PASSPORT_KEY = "passport"
        const val DRIVING_LICENSE_KEY = "driving_license"
        const val ID_CARD_KEY = "id_card"
        const val SELECTION_NONE = ""
        val TAG: String = DocSelectionFragment::class.java.simpleName

        private fun Type.toScanDestination() =
            when (this) {
                Type.IDCARD -> IDScanDestination()
                Type.PASSPORT -> PassportScanDestination()
                Type.DRIVINGLICENSE -> DriverLicenseScanDestination()
            }

        private fun Type.toUploadDestination(
            shouldShowTakePhoto: Boolean,
            shouldShowChoosePhoto: Boolean
        ) =
            when (this) {
                Type.IDCARD ->
                    IDUploadDestination(
                        shouldShowTakePhoto,
                        shouldShowChoosePhoto
                    )
                Type.PASSPORT ->
                    PassportUploadDestination(
                        shouldShowTakePhoto,
                        shouldShowChoosePhoto
                    )
                Type.DRIVINGLICENSE ->
                    DriverLicenseUploadDestination(
                        shouldShowTakePhoto,
                        shouldShowChoosePhoto
                    )
            }

        private fun Type.toAnalyticsScanType() = when (this) {
            Type.DRIVINGLICENSE -> IdentityScanState.ScanType.DL_FRONT
            Type.IDCARD -> IdentityScanState.ScanType.ID_FRONT
            Type.PASSPORT -> IdentityScanState.ScanType.PASSPORT
        }
    }
}
