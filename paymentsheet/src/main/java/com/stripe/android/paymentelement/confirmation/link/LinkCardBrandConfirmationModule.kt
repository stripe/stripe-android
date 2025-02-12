package com.stripe.android.paymentelement.confirmation.link

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface LinkCardBrandConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsLinkCardBrandConfirmationDefinition(
        definition: LinkCardBrandConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>
}
