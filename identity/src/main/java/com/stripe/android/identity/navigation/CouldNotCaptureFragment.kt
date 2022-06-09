package com.stripe.android.identity.navigation

import android.view.View
import androidx.annotation.IdRes
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.IdentityDocumentScanFragment.Companion.ARG_SHOULD_START_FROM_BACK
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.navigateToUploadFragment

/**
 * Fragment to indicate live capture failure.
 */
internal class CouldNotCaptureFragment : BaseErrorFragment() {

    override fun onCustomizingViews() {
        val args = requireNotNull(arguments) {
            "Argument to CouldNotCaptureFragment is null"
        }
        val scanType = args[ARG_COULD_NOT_CAPTURE_SCAN_TYPE] as IdentityScanState.ScanType

        title.text = getString(R.string.could_not_capture_title)
        message1.text = getString(R.string.could_not_capture_body1)

        if (scanType == IdentityScanState.ScanType.SELFIE) {
            topButton.visibility = View.GONE
            message2.visibility = View.GONE
        } else {
            message2.text = getString(R.string.could_not_capture_body2)
            topButton.text = getString(R.string.file_upload)
            topButton.setOnClickListener {
                navigateToUploadFragment(
                    scanType.toUploadDestinationId(),
                    shouldShowTakePhoto = true,
                    shouldShowChoosePhoto = !(args[ARG_REQUIRE_LIVE_CAPTURE] as Boolean)
                )
            }
        }

        bottomButton.text = getString(R.string.try_again)
        bottomButton.setOnClickListener {
            findNavController().navigate(
                scanType.toScanDestinationId(),
                bundleOf(
                    ARG_SHOULD_START_FROM_BACK to scanType.toShouldStartFromBack()
                )
            )
        }
    }

    internal companion object {
        const val ARG_COULD_NOT_CAPTURE_SCAN_TYPE = "scanType"
        const val ARG_REQUIRE_LIVE_CAPTURE = "requireLiveCapture"

        @IdRes
        private fun IdentityScanState.ScanType.toUploadDestinationId() =
            when (this) {
                IdentityScanState.ScanType.ID_FRONT -> R.id.action_couldNotCaptureFragment_to_IDUploadFragment
                IdentityScanState.ScanType.ID_BACK -> R.id.action_couldNotCaptureFragment_to_IDUploadFragment
                IdentityScanState.ScanType.DL_FRONT -> R.id.action_couldNotCaptureFragment_to_driverLicenseUploadFragment
                IdentityScanState.ScanType.DL_BACK -> R.id.action_couldNotCaptureFragment_to_driverLicenseUploadFragment
                IdentityScanState.ScanType.PASSPORT -> R.id.action_couldNotCaptureFragment_to_passportUploadFragment
                IdentityScanState.ScanType.SELFIE -> {
                    throw IllegalArgumentException("SELFIE doesn't support upload")
                }
            }

        @IdRes
        private fun IdentityScanState.ScanType.toScanDestinationId() =
            when (this) {
                IdentityScanState.ScanType.ID_FRONT -> R.id.action_couldNotCaptureFragment_to_IDScanFragment
                IdentityScanState.ScanType.ID_BACK -> R.id.action_couldNotCaptureFragment_to_IDScanFragment
                IdentityScanState.ScanType.DL_FRONT -> R.id.action_couldNotCaptureFragment_to_driverLicenseScanFragment
                IdentityScanState.ScanType.DL_BACK -> R.id.action_couldNotCaptureFragment_to_driverLicenseScanFragment
                IdentityScanState.ScanType.PASSPORT -> R.id.action_couldNotCaptureFragment_to_passportScanFragment
                IdentityScanState.ScanType.SELFIE -> R.id.action_couldNotCaptureFragment_to_selfieFragment
            }

        private fun IdentityScanState.ScanType.toShouldStartFromBack() =
            when (this) {
                IdentityScanState.ScanType.ID_FRONT -> false
                IdentityScanState.ScanType.ID_BACK -> true
                IdentityScanState.ScanType.DL_FRONT -> false
                IdentityScanState.ScanType.DL_BACK -> true
                IdentityScanState.ScanType.PASSPORT -> false
                IdentityScanState.ScanType.SELFIE -> false
            }
    }
}
