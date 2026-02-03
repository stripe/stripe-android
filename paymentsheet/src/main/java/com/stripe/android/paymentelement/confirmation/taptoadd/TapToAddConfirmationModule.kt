package com.stripe.android.paymentelement.confirmation.taptoadd

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface TapToAddConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsTapToAddConfirmationModule(
        definition: TapToAddConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}