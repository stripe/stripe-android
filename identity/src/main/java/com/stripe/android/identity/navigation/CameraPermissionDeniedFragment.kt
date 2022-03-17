package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.CameraPermissionDeniedFragmentBinding
import com.stripe.android.identity.networking.models.IdDocumentParam
import com.stripe.android.identity.utils.navigateToUploadFragment

/**
 * Fragment to show user denies camera permission.
 */
internal class CameraPermissionDeniedFragment(
    private val appSettingsOpenable: AppSettingsOpenable
) :
    Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireNotNull(arguments)

        val identityScanType = args[ARG_SCAN_TYPE] as IdDocumentParam.Type

        val binding = CameraPermissionDeniedFragmentBinding.inflate(inflater, container, false)

        binding.uploadFileText.text =
            getString(R.string.upload_file_text, identityScanType.getDisplayName())

        binding.appSettings.setOnClickListener {
            appSettingsOpenable.openAppSettings()
            // navigate back to DocSelectFragment, so that when user is back to the app from settings
            // the camera permission check can be triggered again from there.
            findNavController().navigate(R.id.action_cameraPermissionDeniedFragment_to_docSelectionFragment)
        }

        binding.fileUpload.setOnClickListener {
            navigateToUploadFragment(
                identityScanType.toUploadDestinationId(),
                false
            )
        }

        return binding.root
    }

    private fun IdDocumentParam.Type.getDisplayName() =
        when (this) {
            IdDocumentParam.Type.IDCARD -> {
                getString(R.string.displayname_id)
            }
            IdDocumentParam.Type.DRIVINGLICENSE -> {
                getString(R.string.displayname_dl)
            }
            IdDocumentParam.Type.PASSPORT -> {
                getString(R.string.displayname_passport)
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
