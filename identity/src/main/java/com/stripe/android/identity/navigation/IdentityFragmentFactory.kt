package com.stripe.android.identity.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.viewmodel.CameraViewModel

/**
 * Factory for creating Identity fragments.
 */
internal class IdentityFragmentFactory(
    private val cameraPermissionEnsureable: CameraPermissionEnsureable
) : FragmentFactory() {
    private val cameraViewModelFactory = CameraViewModel.CameraViewModelFactory()

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
            else -> super.instantiate(classLoader, className)
        }
    }
}
