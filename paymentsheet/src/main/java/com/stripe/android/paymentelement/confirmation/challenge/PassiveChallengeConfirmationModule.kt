package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.challenge.passive.warmer.PassiveChallengeWarmerModule
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import javax.inject.Named

@Module(includes = [PassiveChallengeWarmerModule::class])
internal interface PassiveChallengeConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsPassiveChallengeConfirmationDefinition(
        definition: PassiveChallengeConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun provideApiConfiguration(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
            @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
        ): ApiConfiguration.State = ApiConfiguration.State(
            publishableKey = publishableKeyProvider(),
            stripeAccountId = stripeAccountIdProvider(),
        )
    }
}
