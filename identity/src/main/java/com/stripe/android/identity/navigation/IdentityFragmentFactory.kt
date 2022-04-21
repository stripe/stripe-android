package com.stripe.android.identity.navigation

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.networking.DefaultIDDetectorFetcher
import com.stripe.android.identity.networking.DefaultIdentityRepository
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.DefaultIdentityIO
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.viewmodel.ConsentFragmentViewModel
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityUploadViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel

/**
 * Factory for creating Identity fragments.
 *
 * TODO(ccen) Daggerize the dependencies
 */
internal class IdentityFragmentFactory(
    context: Context,
    private val cameraPermissionEnsureable: CameraPermissionEnsureable,
    private val appSettingsOpenable: AppSettingsOpenable,
    verificationArgsSupplier: () -> IdentityVerificationSheetContract.Args,
    private val verificationFlowFinishable: VerificationFlowFinishable,
    private val identityIO: IdentityIO = DefaultIdentityIO(context),
    private val identityRepository: IdentityRepository =
        DefaultIdentityRepository(identityIO = identityIO),
    private val identityScanViewModelFactory: ViewModelProvider.Factory =
        IdentityScanViewModel.IdentityScanViewModelFactory(),
    private val identityUploadViewModelFactory: ViewModelProvider.Factory =
        IdentityUploadViewModel.FrontBackUploadViewModelFactory(identityIO),
    private val consentFragmentViewModelFactory: ViewModelProvider.Factory =
        ConsentFragmentViewModel.ConsentFragmentViewModelFactory(
            identityIO,
            identityRepository
        ),
    internal val identityViewModelFactory: ViewModelProvider.Factory =
        IdentityViewModel.IdentityViewModelFactory(
            identityRepository,
            DefaultIDDetectorFetcher(identityRepository, identityIO),
            verificationArgsSupplier,
            identityIO
        )
) : FragmentFactory() {

//    internal val identityViewModelFactory by lazy {
//        IdentityViewModel.IdentityViewModelFactory(
//            verificationArgsSupplier(),
//            identityRepository,
//            DefaultIDDetectorFetcher(identityRepository, identityIO),
//            verificationArgsSupplier(),
//            identityIO
//        )
//    }

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
                identityUploadViewModelFactory,
                identityViewModelFactory
            )
            DriverLicenseUploadFragment::class.java.name -> DriverLicenseUploadFragment(
                identityUploadViewModelFactory,
                identityViewModelFactory
            )
            PassportUploadFragment::class.java.name -> PassportUploadFragment(
                identityUploadViewModelFactory,
                identityViewModelFactory
            )
            ConsentFragment::class.java.name -> ConsentFragment(
                identityViewModelFactory,
                consentFragmentViewModelFactory,
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
