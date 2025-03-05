package com.stripe.android.identity.injection

import androidx.lifecycle.ViewModelProvider
import com.stripe.android.camera.AppSettingsOpenable
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.IdentityVerificationSheetContract
import com.stripe.android.identity.VerificationFlowFinishable
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.networking.IdentityRepository
import dagger.BindsInstance
import dagger.Subcomponent

@IdentityVerificationScope
@Subcomponent
internal interface IdentityActivitySubcomponent {
    val identityRepository: IdentityRepository
    val identityAnalyticsRequestFactory: IdentityAnalyticsRequestFactory
    val verificationArgs: IdentityVerificationSheetContract.Args

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
