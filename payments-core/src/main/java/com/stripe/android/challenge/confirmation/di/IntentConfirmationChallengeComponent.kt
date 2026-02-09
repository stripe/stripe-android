package com.stripe.android.challenge.confirmation.di

import android.app.Application
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeArgs
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeViewModel
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        IntentConfirmationChallengeModule::class,
        ApplicationContextModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface IntentConfirmationChallengeComponent {
    val viewModel: IntentConfirmationChallengeViewModel

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance
            application: Application,
            @BindsInstance
            args: IntentConfirmationChallengeArgs,
        ): IntentConfirmationChallengeComponent
    }
}
