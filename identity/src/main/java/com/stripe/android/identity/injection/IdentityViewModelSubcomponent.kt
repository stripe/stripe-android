package com.stripe.android.identity.injection

import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.FallbackUrlLauncher
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.viewmodel.IdentityViewModel
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent
internal interface IdentityViewModelSubcomponent {
    val viewModel: IdentityViewModel

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
        fun identityViewModelFactory(identityViewModelFactory: IdentityViewModel.IdentityViewModelFactory): Builder

        @BindsInstance
        fun fallbackUrlLauncher(fallbackUrlLauncher: FallbackUrlLauncher): Builder

        fun build(): IdentityViewModelSubcomponent
    }
}
