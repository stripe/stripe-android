package com.stripe.android.identity.navigation

import android.view.View
import androidx.annotation.IdRes
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.identity.R
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.utils.navigateToUploadFragment

/**
 * Fragment to show user denies camera permission.
 */
internal class CameraPermissionDeniedFragment(
    private val appSettingsOpenable: AppSettingsOpenable,
    identityViewModelFactory: ViewModelProvider.Factory
) : BaseErrorFragment(identityViewModelFactory) {
    override fun onCustomizingViews() {

        title.text = getString(R.string.camera_permission)
        message1.text = getString(R.string.grant_camera_permission_text)

        (arguments?.get(ARG_SCAN_TYPE) as? CollectedDataParam.Type)?.let { identityScanType ->
            message2.text =
                getString(R.string.upload_file_text, identityScanType.getDisplayName())
            topButton.text = getString(R.string.file_upload)
            topButton.setOnClickListener {
                navigateToUploadFragment(
                    identityScanType.toUploadDestinationId(),
                    shouldShowTakePhoto = false,
                    shouldShowChoosePhoto = true
                )
            }
        } ?: run {
            message2.visibility = View.GONE
            topButton.visibility = View.GONE
        }

        bottomButton.text = getString(R.string.app_settings)
        bottomButton.setOnClickListener {
            appSettingsOpenable.openAppSettings()
            // navigate back to DocSelectFragment, so that when user is back to the app from settings
            // the camera permission check can be triggered again from there.
            findNavController().navigate(R.id.action_cameraPermissionDeniedFragment_to_docSelectionFragment)
        }
    }

    private fun CollectedDataParam.Type.getDisplayName() =
        when (this) {
            CollectedDataParam.Type.IDCARD -> {
                getString(R.string.id_card)
            }
            CollectedDataParam.Type.DRIVINGLICENSE -> {
                getString(R.string.driver_license)
            }
            CollectedDataParam.Type.PASSPORT -> {
                getString(R.string.passport)
            }
        }

    internal companion object {
        const val ARG_SCAN_TYPE = "scanType"

        @IdRes
        private fun CollectedDataParam.Type.toUploadDestinationId() =
            when (this) {
                CollectedDataParam.Type.IDCARD -> R.id.action_cameraPermissionDeniedFragment_to_IDUploadFragment
                CollectedDataParam.Type.DRIVINGLICENSE -> R.id.action_cameraPermissionDeniedFragment_to_driverLicenseUploadFragment
                CollectedDataParam.Type.PASSPORT -> R.id.action_cameraPermissionDeniedFragment_to_passportUploadFragment
            }
    }
}
