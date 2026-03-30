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
import com.stripe.android.identity.networking.DefaultIdentityModelFetcher
import com.stripe.android.identity.networking.IdentityModelFetcher
import com.stripe.android.identity.networking.IdentityRepository
import com.stripe.android.identity.utils.IdentityIO
import com.stripe.android.identity.utils.IdentityImageHandler
import com.stripe.android.identity.viewmodel.DocumentScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.SelfieScanViewModel
import com.stripe.android.mlcore.base.InterpreterInitializer
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@IdentityVerificationScope
@Subcomponent(
    modules = [
        IdentityVerificationModule::class,
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

    @Subcomponent.Factory
    interface Factory {
        fun create(
            @BindsInstance args: IdentityVerificationSheetContract.Args,
            @BindsInstance cameraPermissionEnsureable: CameraPermissionEnsureable,
            @BindsInstance appSettingsOpenable: AppSettingsOpenable,
            @BindsInstance verificationFlowFinishable: VerificationFlowFinishable,
            @BindsInstance identityViewModelFactory: ViewModelProvider.Factory,
            @BindsInstance viewModelStoreOwner: ViewModelStoreOwner,
            @BindsInstance fallbackUrlLauncher: FallbackUrlLauncher,
        ): IdentityActivitySubcomponent
    }
}

@Module
internal abstract class IdentityVerificationModule {
    @Binds
    @IdentityVerificationScope
    abstract fun bindIdentityModelFetcher(
        defaultIdentityModelFetcher: DefaultIdentityModelFetcher
    ): IdentityModelFetcher
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
