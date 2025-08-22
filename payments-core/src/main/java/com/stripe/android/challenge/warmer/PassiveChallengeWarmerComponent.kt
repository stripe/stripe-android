package com.stripe.android.challenge.warmer

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
internal interface PassiveChallengeWarmerComponent {
    val passiveChallengeWarmerViewModel: PassiveChallengeWarmerViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun passiveCaptchaParams(passiveCaptchaParams: PassiveCaptchaParams): Builder

        fun build(): PassiveChallengeWarmerComponent
    }
}
