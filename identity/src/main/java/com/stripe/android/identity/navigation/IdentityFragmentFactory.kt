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
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            IDScanFragment::class.java.name -> IDScanFragment(
                cameraPermissionEnsureable,
                CameraViewModel.CameraViewModelFactory()
            )
            else -> super.instantiate(classLoader, className)
        }
    }
}
