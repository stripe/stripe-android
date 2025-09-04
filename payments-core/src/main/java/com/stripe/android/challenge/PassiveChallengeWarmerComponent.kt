package com.stripe.android.challenge

import android.content.Context
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
        PassiveChallengeWarmerModule::class,
        StripeRepositoryModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class
    ]
)
internal interface PassiveChallengeWarmerComponent {
    val passiveChallengeWarmerViewModel: PassiveChallengeWarmerViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun publishableKeyProvider(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String
        ): Builder

        @BindsInstance
        fun productUsage(@Named(PRODUCT_USAGE) productUsage: Set<String>): Builder

        @BindsInstance
        fun passiveCaptchaParams(passiveCaptchaParams: PassiveCaptchaParams): Builder

        fun build(): PassiveChallengeWarmerComponent
    }
}
