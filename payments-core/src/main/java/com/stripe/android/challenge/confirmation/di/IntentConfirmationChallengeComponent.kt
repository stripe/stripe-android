package com.stripe.android.challenge.confirmation.di

import android.content.Context
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeArgs
import com.stripe.android.challenge.confirmation.IntentConfirmationChallengeViewModel
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        IntentConfirmationChallengeModule::class,
        CoreCommonModule::class,
        CoroutineContextModule::class,
    ]
)
internal interface IntentConfirmationChallengeComponent {
    val viewModel: IntentConfirmationChallengeViewModel

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun args(args: IntentConfirmationChallengeArgs): Builder

        fun build(): IntentConfirmationChallengeComponent
    }
}
