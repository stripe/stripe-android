package com.stripe.android.identity.injection

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.IdentityImageHandler
import com.stripe.android.identity.viewmodel.DocumentScanViewModel
import com.stripe.android.identity.viewmodel.SelfieScanViewModel
import com.stripe.android.mlcore.base.InterpreterInitializer
import dagger.BindsInstance
import dagger.Subcomponent

@IdentityVerificationScope
@Subcomponent
internal interface IdentityActivitySubcomponent {
    val documentScanViewModelFactory: DocumentScanViewModel.DocumentScanViewModelFactory
    val selfieScanViewModelFactory: SelfieScanViewModel.SelfieScanViewModelFactory
    val identityRepository: IdentityRepository
    val identityModelFetcher: IdentityModelFetcher
    val identityIO: IdentityIO
    val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
    val fpsTracker: FPSTracker
    val screenTracker: ScreenTracker
    val verificationArgs: IdentityVerificationSheetContract.Args
    val identityImageHandler: IdentityImageHandler
    val tfLiteInitializer: InterpreterInitializer

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun args(args: IdentityVerificationSheetContract.Args): Builder

        @BindsInstance
        fun cameraPermissionEnsureable(cameraPermissionEnsureable: CameraPermissionEnsureable): Builder

        @BindsInstance
        fun appSettingsOpenable(appSettingsOpenable: AppSettingsOpenable): Builder

        @BindsInstance
        fun verificationFlowFinishable(verificationFlowFinishable: VerificationFlowFinishable): Builder

        @BindsInstance
        fun identityViewModelFactory(identityViewModelFactory: ViewModelProvider.Factory): Builder

        fun build(): IdentityActivitySubcomponent
    }
}
