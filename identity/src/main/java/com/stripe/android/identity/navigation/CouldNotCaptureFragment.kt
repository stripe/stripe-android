package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_ERROR
import com.stripe.android.identity.navigation.CouldNotCaptureDestination.Companion.ARG_COULD_NOT_CAPTURE_SCAN_TYPE
import com.stripe.android.identity.navigation.CouldNotCaptureDestination.Companion.ARG_REQUIRE_LIVE_CAPTURE
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.ui.ErrorScreen
import com.stripe.android.identity.ui.ErrorScreenButton

/**
 * Fragment to indicate live capture failure.
 */
internal class CouldNotCaptureFragment(
    identityViewModelFactory: ViewModelProvider.Factory
) : BaseErrorFragment(identityViewModelFactory) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        val args = requireNotNull(arguments) {
            "Argument to CouldNotCaptureFragment is null"
        }
        val scanType = args[ARG_COULD_NOT_CAPTURE_SCAN_TYPE] as IdentityScanState.ScanType

        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ErrorScreen(
                title = stringResource(id = R.string.could_not_capture_title),
                message1 = stringResource(id = R.string.could_not_capture_body1),
                message2 = if (scanType == IdentityScanState.ScanType.SELFIE) {
                    null
                } else {
                    stringResource(
                        R.string.could_not_capture_body2
                    )
                },
                topButton = if (scanType == IdentityScanState.ScanType.SELFIE) {
                    null
                } else {
                    ErrorScreenButton(
                        buttonText = stringResource(id = R.string.file_upload),
                    ) {
                        identityViewModel.screenTracker.screenTransitionStart(
                            SCREEN_NAME_ERROR
                        )
                        findNavController().navigateTo(
                            scanType.toUploadDestination(
                                shouldShowTakePhoto = true,
                                shouldShowChoosePhoto = !args.getBoolean(ARG_REQUIRE_LIVE_CAPTURE)
                            )
                        )
                    }
                },
                bottomButton =
                ErrorScreenButton(
                    buttonText = stringResource(id = R.string.try_again)
                ) {
                    identityViewModel.screenTracker.screenTransitionStart(
                        SCREEN_NAME_ERROR
                    )
                    findNavController().navigateTo(
                        scanType.toScanDestination()
                    )
                }
            )
        }
    }

    internal companion object {

        private fun IdentityScanState.ScanType.toUploadDestination(
            shouldShowTakePhoto: Boolean,
            shouldShowChoosePhoto: Boolean
        ) =
            when (this) {
                IdentityScanState.ScanType.ID_FRONT ->
                    IDUploadDestination(shouldShowTakePhoto, shouldShowChoosePhoto, true)
                IdentityScanState.ScanType.ID_BACK ->
                    IDUploadDestination(shouldShowTakePhoto, shouldShowChoosePhoto, true)
                IdentityScanState.ScanType.DL_FRONT ->
                    DriverLicenseUploadDestination(shouldShowTakePhoto, shouldShowChoosePhoto, true)
                IdentityScanState.ScanType.DL_BACK ->
                    DriverLicenseUploadDestination(shouldShowTakePhoto, shouldShowChoosePhoto, true)
                IdentityScanState.ScanType.PASSPORT ->
                    PassportUploadDestination(shouldShowTakePhoto, shouldShowChoosePhoto, true)
                IdentityScanState.ScanType.SELFIE -> {
                    throw IllegalArgumentException("SELFIE doesn't support upload")
                }
            }

        private fun IdentityScanState.ScanType.toScanDestination() =
            when (this) {
                IdentityScanState.ScanType.ID_FRONT ->
                    IDScanDestination(
                        shouldStartFromBack = false,
                        shouldPopUpToDocSelection = true
                    )
                IdentityScanState.ScanType.ID_BACK ->
                    IDScanDestination(
                        shouldStartFromBack = true,
                        shouldPopUpToDocSelection = true
                    )
                IdentityScanState.ScanType.DL_FRONT ->
                    DriverLicenseScanDestination(
                        shouldStartFromBack = false,
                        shouldPopUpToDocSelection = true
                    )
                IdentityScanState.ScanType.DL_BACK ->
                    DriverLicenseScanDestination(
                        shouldStartFromBack = true,
                        shouldPopUpToDocSelection = true
                    )
                IdentityScanState.ScanType.PASSPORT ->
                    PassportScanDestination(
                        shouldStartFromBack = false,
                        shouldPopUpToDocSelection = true
                    )
                IdentityScanState.ScanType.SELFIE ->
                    SelfieDestination
            }
    }
}
