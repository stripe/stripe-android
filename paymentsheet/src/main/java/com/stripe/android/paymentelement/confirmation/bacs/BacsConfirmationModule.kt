package com.stripe.android.paymentelement.confirmation.bacs

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.DefaultBacsMandateConfirmationLauncherFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal interface BacsConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsBacsConfirmationDefinition(
        definition: BacsConfirmationDefinition,
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @Provides
        fun providesBacsMandateConfirmationLauncherFactory(): BacsMandateConfirmationLauncherFactory =
            DefaultBacsMandateConfirmationLauncherFactory
    }
}
