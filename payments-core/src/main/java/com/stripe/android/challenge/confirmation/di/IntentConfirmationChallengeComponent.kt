package com.stripe.android.challenge.confirmation.di

import android.content.Context
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeArgs
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeViewModel
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.payments.core.injection.PaymentConfigurationModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        IntentConfirmationChallengeModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        PaymentConfigurationModule::class
    ]
)
internal interface IntentConfirmationChallengeComponent {
    val viewModel: IntentConfirmationChallengeViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            context: Context,
            @BindsInstance
            args: IntentConfirmationChallengeArgs,
        ): IntentConfirmationChallengeComponent
    }
}
