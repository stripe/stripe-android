package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface PassiveChallengeConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsPassiveChallengeConfirmationDefinition(
        definition: PassiveChallengeConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
