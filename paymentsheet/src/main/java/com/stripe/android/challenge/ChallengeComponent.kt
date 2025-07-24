package com.stripe.android.challenge

import com.stripe.android.hcaptcha.HCaptchaModule
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        HCaptchaModule::class,
    ]
)
internal interface ChallengeComponent {
    val passiveChallengeViewModel: PassiveChallengeViewModel

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun paymentConfirmationOption(paymentMethodConfirmationOption: PaymentMethodConfirmationOption.New): Builder

        fun build(): ChallengeComponent
    }
}