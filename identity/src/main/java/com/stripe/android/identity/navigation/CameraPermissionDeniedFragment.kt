package com.stripe.android.identity.navigation

import androidx.annotation.IdRes
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.utils.navigateToUploadFragment

/**
 * Fragment to show user denies camera permission.
 */
internal class CameraPermissionDeniedFragment(
    private val appSettingsOpenable: AppSettingsOpenable
) : BaseErrorFragment() {
    override fun onCustomizingViews() {
        val args = requireNotNull(arguments)

        val identityScanType = args[ARG_SCAN_TYPE] as IdDocumentParam.Type

        title.text = getString(R.string.camera_permission)
        message1.text = getString(R.string.grant_camera_permission_text)
        message2.text =
            getString(R.string.upload_file_text, identityScanType.getDisplayName())

        topButton.text = getString(R.string.file_upload)
        topButton.setOnClickListener {
            navigateToUploadFragment(
                identityScanType.toUploadDestinationId(),
                false
            )
        }

        bottomButton.text = getString(R.string.app_settings)
        bottomButton.setOnClickListener {
            appSettingsOpenable.openAppSettings()
            // navigate back to DocSelectFragment, so that when user is back to the app from settings
            // the camera permission check can be triggered again from there.
            findNavController().navigate(R.id.action_cameraPermissionDeniedFragment_to_docSelectionFragment)
        }
    }

    private fun IdDocumentParam.Type.getDisplayName() =
        when (this) {
            IdDocumentParam.Type.IDCARD -> {
                getString(R.string.id_card)
            }
            IdDocumentParam.Type.DRIVINGLICENSE -> {
                getString(R.string.driver_license)
            }
            IdDocumentParam.Type.PASSPORT -> {
                getString(R.string.passport)
            }
        }

    internal companion object {
        const val ARG_SCAN_TYPE = "scanType"

        @IdRes
        private fun IdDocumentParam.Type.toUploadDestinationId() =
            when (this) {
                IdDocumentParam.Type.IDCARD -> R.id.action_cameraPermissionDeniedFragment_to_IDUploadFragment
                IdDocumentParam.Type.DRIVINGLICENSE -> R.id.action_cameraPermissionDeniedFragment_to_driverLicenseUploadFragment
                IdDocumentParam.Type.PASSPORT -> R.id.action_cameraPermissionDeniedFragment_to_passportUploadFragment
            }
    }
}
