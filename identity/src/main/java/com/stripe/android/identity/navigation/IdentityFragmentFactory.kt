package com.stripe.android.identity.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewmodel.ConsentFragmentViewModel
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import javax.inject.Inject

/**
 * Factory for creating Identity fragments.
 */
internal class IdentityFragmentFactory @Inject constructor(
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val appSettingsOpenable: AppSettingsOpenable,
    private val verificationFlowFinishable: VerificationFlowFinishable,
    private val identityScanViewModelFactory: IdentityScanViewModel.IdentityScanViewModelFactory,
    private val consentFragmentViewModelFactory: ConsentFragmentViewModel.ConsentFragmentViewModelFactory,
    internal val identityViewModelFactory: ViewModelProvider.Factory,
    private val fallbackUrlLauncher: FallbackUrlLauncher,
    private val identityIO: IdentityIO
) : FragmentFactory() {

    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            IDScanFragment::class.java.name -> IDScanFragment(
                identityScanViewModelFactory,
                identityViewModelFactory
            )
            DriverLicenseScanFragment::class.java.name -> DriverLicenseScanFragment(
                identityScanViewModelFactory,
                identityViewModelFactory
            )
            PassportScanFragment::class.java.name -> PassportScanFragment(
                identityScanViewModelFactory,
                identityViewModelFactory
            )
            SelfieFragment::class.java.name -> SelfieFragment(
                identityScanViewModelFactory,
                identityViewModelFactory
            )
            CameraPermissionDeniedFragment::class.java.name -> CameraPermissionDeniedFragment(
                appSettingsOpenable,
                identityViewModelFactory
            )
            IDUploadFragment::class.java.name -> IDUploadFragment(
                identityIO,
                identityViewModelFactory
            )
            DriverLicenseUploadFragment::class.java.name -> DriverLicenseUploadFragment(
                identityIO,
                identityViewModelFactory
            )
            PassportUploadFragment::class.java.name -> PassportUploadFragment(
                identityIO,
                identityViewModelFactory
            )
            ConsentFragment::class.java.name -> ConsentFragment(
                identityViewModelFactory,
                consentFragmentViewModelFactory,
                fallbackUrlLauncher
            )
            DocSelectionFragment::class.java.name -> DocSelectionFragment(
                identityViewModelFactory,
                cameraPermissionEnsureable
            )
            ConfirmationFragment::class.java.name -> ConfirmationFragment(
                identityViewModelFactory,
                verificationFlowFinishable
            )
            ErrorFragment::class.java.name -> ErrorFragment(
                verificationFlowFinishable,
                identityViewModelFactory
            )
            CouldNotCaptureFragment::class.java.name -> CouldNotCaptureFragment(
                identityViewModelFactory
            )
            else -> super.instantiate(classLoader, className)
        }
    }
}
