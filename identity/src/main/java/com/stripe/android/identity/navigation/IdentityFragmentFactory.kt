package com.stripe.android.identity.navigation

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.networking.DefaultIDDetectorFetcher
import com.stripe.android.identity.networking.DefaultIdentityRepository
import com.stripe.android.identity.utils.DefaultIdentityIO
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewmodel.FrontBackUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.PassportUploadViewModel

/**
 * Factory for creating Identity fragments.
 *
 * TODO(ccen) Daggerize the dependencies
 */
internal class IdentityFragmentFactory(
    context: Context,
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val appSettingsOpenable: AppSettingsOpenable,
    verificationArgs: IdentityVerificationSheetContract.Args,
    private val verificationFlowFinishable: VerificationFlowFinishable
) : FragmentFactory() {
    private val identityIO: IdentityIO = DefaultIdentityIO(context)
    private val identityRepository =
        DefaultIdentityRepository(identityIO = identityIO)
    private val identityScanViewModelFactory =
        IdentityScanViewModel.IdentityScanViewModelFactory()
    private val frontBackUploadViewModelFactory =
        FrontBackUploadViewModel.FrontBackUploadViewModelFactory(identityIO)
    private val passportUploadViewModelFactory =
        PassportUploadViewModel.PassportUploadViewModelFactory(identityIO)

    internal val identityViewModelFactory = IdentityViewModel.IdentityViewModelFactory(
        verificationArgs,
        identityRepository,
        DefaultIDDetectorFetcher(identityRepository, identityIO),
        verificationArgs,
        identityIO
    )

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
            CameraPermissionDeniedFragment::class.java.name -> CameraPermissionDeniedFragment(
                appSettingsOpenable
            )
            IDUploadFragment::class.java.name -> IDUploadFragment(
                frontBackUploadViewModelFactory,
                identityViewModelFactory
            )
            DriverLicenseUploadFragment::class.java.name -> DriverLicenseUploadFragment(
                frontBackUploadViewModelFactory,
                identityViewModelFactory
            )
            PassportUploadFragment::class.java.name -> PassportUploadFragment(
                passportUploadViewModelFactory,
                identityViewModelFactory
            )
            ConsentFragment::class.java.name -> ConsentFragment(
                identityViewModelFactory,
                verificationFlowFinishable
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
                verificationFlowFinishable
            )
            else -> super.instantiate(classLoader, className)
        }
    }
}
