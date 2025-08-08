package com.stripe.android.identity.injection

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
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
import com.stripe.android. identity.viewmodel.IdentityViewModel
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@IdentityVerificationScope
@Subcomponent(
    modules = [
        IdentityViewModelModule::class
    ]
)
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

        @BindsInstance
        fun viewModelStoreOwner(viewModelStore0wner: ViewModelStoreOwner): Builder

        @BindsInstance
        fun fallbackUrlLauncher(fallbackUrlLauncher: FallbackUrlLauncher): Builder

        fun build(): IdentityActivitySubcomponent
    }
}

@Module
internal object IdentityViewModelModule {
    @Provides
    fun provideIdentityViewModel(
        factory: ViewModelProvider.Factory,
        owner: ViewModelStoreOwner
    ): IdentityViewModel =
        ViewModelProvider(owner, factory)[IdentityViewModel::class.java]

    @Provides
    fun provideVerificationPage(
        identityViewModel: IdentityViewModel
    ) = identityViewModel.verificationPage
}
