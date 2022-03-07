package com.stripe.android.identity.navigation

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.networking.DefaultIdentityRepository
import com.stripe.android.identity.viewmodel.CameraViewModel
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.PassportUploadViewModel

/**
 * Factory for creating Identity fragments.
 */
internal class IdentityFragmentFactory(
    context: Context,
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val appSettingsOpenable: AppSettingsOpenable,
    verificationArgs: IdentityVerificationSheetContract.Args
) : FragmentFactory() {
    private val identityRepository = DefaultIdentityRepository(context)
    private val cameraViewModelFactory = CameraViewModel.CameraViewModelFactory()
    private val frontBackUploadViewModelFactory =
        FrontBackUploadViewModel.FrontBackUploadViewModelFactory(
            identityRepository,
            verificationArgs
        )
    private val passportUploadViewModelFactory =
        PassportUploadViewModel.PassportUploadViewModelFactory(
            identityRepository,
            verificationArgs
        )

    internal val identityViewModelFactory = IdentityViewModel.IdentityViewModelFactory(
        verificationArgs,
        identityRepository
    )

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
            ConsentFragment::class.java.name -> ConsentFragment(
                identityViewModelFactory
            )
            else -> super.instantiate(classLoader, className)
        }
    }
}
