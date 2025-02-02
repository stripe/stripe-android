package com.stripe.android.paymentelement.confirmation.gpay

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface GooglePayConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsGooglePayConfirmationDefinition(
        definition: GooglePayConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
