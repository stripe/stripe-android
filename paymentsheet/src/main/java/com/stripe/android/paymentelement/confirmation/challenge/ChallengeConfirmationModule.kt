package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface ChallengeConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsChallengeConfirmationDefinition(
        definition: ChallengeConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
