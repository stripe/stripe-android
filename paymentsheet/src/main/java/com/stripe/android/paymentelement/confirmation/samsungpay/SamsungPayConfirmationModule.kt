package com.stripe.android.paymentelement.confirmation.samsungpay

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface SamsungPayConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsSamsungPayConfirmationDefinition(
        definition: SamsungPayConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
