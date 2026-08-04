package com.stripe.android.challenge.passive.warmer.activity

import android.content.Context
import com.stripe.android.ApiConfiguration
import com.stripe.android.challenge.passive.PassiveChallengeModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.hcaptcha.HCaptchaModule
import com.stripe.android.model.PassiveCaptchaParams
import com.stripe.android.payments.core.injection.ApiConfigurationStateToProviderModule
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        HCaptchaModule::class,
        PassiveChallengeModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        ApiConfigurationStateToProviderModule::class,
    ]
)
internal interface PassiveChallengeWarmerActivityComponent {
    val passiveChallengeWarmerViewModel: PassiveChallengeWarmerViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            context: Context,
            @BindsInstance
            apiConfigurationState: ApiConfiguration.State,
            @BindsInstance
            @Named(PRODUCT_USAGE)
            productUsage: Set<String>,
            @BindsInstance
            passiveCaptchaParams: PassiveCaptchaParams,
        ): PassiveChallengeWarmerActivityComponent
    }
}
