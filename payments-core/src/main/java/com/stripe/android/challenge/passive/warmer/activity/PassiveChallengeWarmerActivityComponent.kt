package com.stripe.android.challenge.passive.warmer.activity

import android.app.Application
import com.stripe.android.challenge.passive.PassiveChallengeModule
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.hcaptcha.HCaptchaModule
import com.stripe.android.model.PassiveCaptchaParams
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
        ApplicationContextModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class
    ]
)
internal interface PassiveChallengeWarmerActivityComponent {
    val passiveChallengeWarmerViewModel: PassiveChallengeWarmerViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            @Named(PUBLISHABLE_KEY)
            publishableKeyProvider: () -> String,
            @BindsInstance
            @Named(PRODUCT_USAGE)
            productUsage: Set<String>,
            @BindsInstance
            passiveCaptchaParams: PassiveCaptchaParams,
        ): PassiveChallengeWarmerActivityComponent
    }
}
