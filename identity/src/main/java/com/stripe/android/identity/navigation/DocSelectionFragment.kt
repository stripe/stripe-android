package com.stripe.android.identity.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_DOC_SELECT
import com.stripe.android.identity.databinding.DocSelectionFragmentBinding
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.ClearDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.CollectedDataParam.Type
import com.stripe.android.identity.networking.models.VerificationPage.Companion.requireSelfie
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.navigateToDefaultErrorFragment
import com.stripe.android.identity.utils.navigateToUploadFragment
import com.stripe.android.identity.utils.postVerificationPageDataAndMaybeSubmit
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.Dispatchers
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

    private lateinit var binding: DocSelectionFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DocSelectionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        identityViewModel.observeForVerificationPage(
            viewLifecycleOwner,
            onSuccess = { verificationPage ->
                binding.title.text = verificationPage.documentSelect.title
                when (verificationPage.documentSelect.idDocumentTypeAllowlist.count()) {
                    0 -> {
                        toggleMultiSelectionUI(requireSelfie = verificationPage.requireSelfie())
                    }
                    1 -> {
                        verificationPage.documentSelect.let { documentSelect ->
                            toggleSingleSelectionUI(
                                documentSelect.idDocumentTypeAllowlist.entries.first().key,
                                documentSelect.buttonText,
                                documentSelect.body,
                                verificationPage.requireSelfie()
                            )
                        }
                    }
                    else -> {
                        toggleMultiSelectionUI(
                            verificationPage.documentSelect.idDocumentTypeAllowlist,
                            verificationPage.requireSelfie()
                        )
                    }
                }
            },
            onFailure = {
                navigateToDefaultErrorFragment()
            }
        )
        lifecycleScope.launch(Dispatchers.IO) {
            identityViewModel.screenTracker.screenTransitionFinish(SCREEN_NAME_DOC_SELECT)
        }
        identityViewModel.sendAnalyticsRequest(
            identityViewModel.identityAnalyticsRequestFactory.screenPresented(
                screenName = SCREEN_NAME_DOC_SELECT
            )
        )
    }

    /**
     * Toggle UI to show multiple selection types. If idDocumentTypeAllowlist from server is null,
     * show all three types with default values.
     */
    private fun toggleMultiSelectionUI(
        idDocumentTypeAllowlist: Map<String, String>? = null,
        requireSelfie: Boolean
    ) {
        binding.multiSelectionContent.visibility = View.VISIBLE
        binding.singleSelectionContent.visibility = View.GONE
        idDocumentTypeAllowlist?.let {
            for ((allowedType, allowedTypeValue) in idDocumentTypeAllowlist) {
                when (allowedType) {
                    PASSPORT_KEY -> {
                        binding.passport.text = allowedTypeValue
                        binding.passportContainer.visibility = View.VISIBLE
                        binding.passport.setOnClickListener {
                            binding.passport.isEnabled = false
                            binding.dl.isClickable = false
                            binding.id.isClickable = false
                            binding.passportIndicator.visibility = View.VISIBLE
                            postVerificationPageDataAndNavigate(Type.PASSPORT, requireSelfie)
                        }
                        binding.passportSeparator.visibility = View.VISIBLE
                    }
                    DRIVING_LICENSE_KEY -> {
                        binding.dl.text = allowedTypeValue
                        binding.dlContainer.visibility = View.VISIBLE
                        binding.dl.setOnClickListener {
                            binding.dl.isEnabled = false
                            binding.passport.isClickable = false
                            binding.id.isClickable = false
                            binding.dlIndicator.visibility = View.VISIBLE
                            postVerificationPageDataAndNavigate(Type.DRIVINGLICENSE, requireSelfie)
                        }
                        binding.dlSeparator.visibility = View.VISIBLE
                    }
                    ID_CARD_KEY -> {
                        binding.id.text = allowedTypeValue
                        binding.idContainer.visibility = View.VISIBLE
                        binding.id.setOnClickListener {
                            binding.id.isEnabled = false
                            binding.passport.isClickable = false
                            binding.dl.isClickable = false
                            binding.idIndicator.visibility = View.VISIBLE
                            postVerificationPageDataAndNavigate(Type.IDCARD, requireSelfie)
                        }
                        binding.idSeparator.visibility = View.VISIBLE
                    }
                    else -> {
                        throw InvalidRequestException(message = "Unknown allow type: $allowedType")
                    }
                }
            }
        } ?: run {
            // Not possible for backend to send an empty list of allowed types.
            Log.e(TAG, "Received an empty idDocumentTypeAllowlist.")
            navigateToDefaultErrorFragment()
        }
    }

    /**
     * Toggle UI to show single selection type.
     */
    private fun toggleSingleSelectionUI(
        allowedType: String,
        buttonText: String,
        bodyText: String?,
        requireSelfie: Boolean
    ) {
        binding.multiSelectionContent.visibility = View.GONE
        binding.singleSelectionContent.visibility = View.VISIBLE
        binding.singleSelectionContinue.setText(buttonText)

        when (allowedType) {
            PASSPORT_KEY -> {
                binding.singleSelectionBody.text = bodyText
                binding.singleSelectionContinue.setOnClickListener {
                    binding.singleSelectionContinue.toggleToLoading()
                    postVerificationPageDataAndNavigate(Type.PASSPORT, requireSelfie)
                }
            }
            DRIVING_LICENSE_KEY -> {
                binding.singleSelectionBody.text = bodyText
                binding.singleSelectionContinue.setOnClickListener {
                    binding.singleSelectionContinue.toggleToLoading()
                    postVerificationPageDataAndNavigate(Type.DRIVINGLICENSE, requireSelfie)
                }
            }
            ID_CARD_KEY -> {
                binding.singleSelectionBody.text = bodyText
                binding.singleSelectionContinue.setOnClickListener {
                    binding.singleSelectionContinue.toggleToLoading()
                    postVerificationPageDataAndNavigate(Type.IDCARD, requireSelfie)
                }
            }
            else -> {
                throw InvalidRequestException(message = "Unknown allow type: $allowedType")
            }
        }
    }

    /**
     * Post VerificationPageData with the type and navigate base on its result.
     */
    private fun postVerificationPageDataAndNavigate(type: Type, requireSelfie: Boolean) {
        lifecycleScope.launch {
            postVerificationPageDataAndMaybeSubmit(
                identityViewModel = identityViewModel,
                collectedDataParam = CollectedDataParam(idDocumentType = type),
                clearDataParam =
                if (requireSelfie) ClearDataParam.DOC_SELECT_TO_UPLOAD_WITH_SELFIE else ClearDataParam.DOC_SELECT_TO_UPLOAD,
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
                    Log.e(TAG, "Can't access camera and client has required live capture.")
                    navigateToDefaultErrorFragment()
                } else {
                    navigateToUploadFragment(
                        type.toUploadDestinationId(),
                        shouldShowTakePhoto = true,
                        shouldShowChoosePhoto = true
                    )
                }
            },
            onFailure = {
                navigateToDefaultErrorFragment()
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
                navigateToDefaultErrorFragment()
            }
        )
    }

    internal companion object {
        const val PASSPORT_KEY = "passport"
        const val DRIVING_LICENSE_KEY = "driving_license"
        const val ID_CARD_KEY = "id_card"
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
