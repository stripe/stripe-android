package com.stripe.android.challenge

import com.stripe.android.hcaptcha.HCaptchaModule
import com.stripe.android.model.PassiveCaptchaParams
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        HCaptchaModule::class,
    ]
)
internal interface PassiveChallengeComponent {
    val passiveChallengeViewModel: PassiveChallengeViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun passiveCaptchaParams(passiveCaptchaParams: PassiveCaptchaParams): Builder

        fun build(): PassiveChallengeComponent
    }
}
