package com.stripe.android.identity.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel
import com.stripe.android.identity.viewmodel.PassportUploadViewModel

/**
 * Factory for creating Identity fragments.
 */
internal class IdentityFragmentFactory(
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val appSettingsOpenable: AppSettingsOpenable
) : FragmentFactory() {
    private val cameraViewModelFactory = CameraViewModel.CameraViewModelFactory()
    private val frontBackUploadViewModelFactory =
        FrontBackUploadViewModel.FrontBackUploadViewModelFactory()
    private val passportUploadViewModelFactory =
        PassportUploadViewModel.PassportUploadViewModelFactory()

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            IDScanFragment::class.java.name -> IDScanFragment(
                cameraPermissionEnsureable,
                cameraViewModelFactory
            )
            DriverLicenseScanFragment::class.java.name -> DriverLicenseScanFragment(
                cameraPermissionEnsureable,
                cameraViewModelFactory
            )
            PassportScanFragment::class.java.name -> PassportScanFragment(
                cameraPermissionEnsureable,
                cameraViewModelFactory
            )
            CameraPermissionDeniedFragment::class.java.name -> CameraPermissionDeniedFragment(
                appSettingsOpenable
            )
            IDUploadFragment::class.java.name -> IDUploadFragment(
                frontBackUploadViewModelFactory
            )
            DriverLicenseUploadFragment::class.java.name -> DriverLicenseUploadFragment(
                frontBackUploadViewModelFactory
            )
            PassportUploadFragment::class.java.name -> PassportUploadFragment(
                passportUploadViewModelFactory
            )
            else -> super.instantiate(classLoader, className)
        }
    }
}
