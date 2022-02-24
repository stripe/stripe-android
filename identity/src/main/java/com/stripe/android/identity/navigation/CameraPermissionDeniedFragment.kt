package com.stripe.android.identity.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.identity.R
import com.stripe.android.identity.databinding.CameraPermissionDeniedFragmentBinding
import com.stripe.android.identity.states.IdentityScanState

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

        val identityScanType = args[ARG_SCAN_TYPE] as IdentityScanState.ScanType

        val binding = CameraPermissionDeniedFragmentBinding.inflate(inflater, container, false)

        binding.uploadFileText.text =
            getString(R.string.upload_file_text, identityScanType.getDisplayName())

        binding.appSettings.setOnClickListener {
            appSettingsOpenable.openAppSettings()
        }

        binding.fileUpload.setOnClickListener {
            findNavController().navigate(
                when (identityScanType) {
                    IdentityScanState.ScanType.ID_FRONT -> R.id.action_cameraPermissionDeniedFragment_to_IDUploadFragment
                    IdentityScanState.ScanType.DL_FRONT -> R.id.action_cameraPermissionDeniedFragment_to_driverLicenseUploadFragment
                    IdentityScanState.ScanType.PASSPORT -> R.id.action_cameraPermissionDeniedFragment_to_passportUploadFragment
                    else -> {
                        throw IllegalArgumentException("CameraPermissionDeniedFragment receives incorrect ScanType: $identityScanType")
                    }
                }
            )
        }

        return binding.root
    }

    private fun IdentityScanState.ScanType.getDisplayName() =
        when (this) {
            IdentityScanState.ScanType.ID_FRONT -> {
                getString(R.string.displayname_id)
            }
            IdentityScanState.ScanType.ID_BACK -> {
                getString(R.string.displayname_id)
            }
            IdentityScanState.ScanType.DL_FRONT -> {
                getString(R.string.displayname_dl)
            }
            IdentityScanState.ScanType.DL_BACK -> {
                getString(R.string.displayname_dl)
            }
            IdentityScanState.ScanType.PASSPORT -> {
                getString(R.string.displayname_passport)
            }
            IdentityScanState.ScanType.SELFIE -> {
                getString(R.string.displayname_selfie)
            }
        }

    companion object {
        const val ARG_SCAN_TYPE = "scanType"
    }
}
