package com.stripe.android.paymentelement.confirmation.sepa

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface SepaMandateConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsSepaMandateConfirmationDefinition(
        definition: SepaMandateConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>
}
