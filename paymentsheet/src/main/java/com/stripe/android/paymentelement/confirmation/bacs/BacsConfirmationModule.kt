package com.stripe.android.paymentelement.confirmation.bacs

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.DefaultBacsMandateConfirmationLauncherFactory
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet

@Module
internal class BacsConfirmationModule {
    @Provides
    fun providesBacsMandateConfirmationLauncherFactory(): BacsMandateConfirmationLauncherFactory =
        DefaultBacsMandateConfirmationLauncherFactory

    @JvmSuppressWildcards
    @Provides
    @IntoSet
    fun providesBacsConfirmationDefinition(
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
    ): ConfirmationDefinition<*, *, *, *> {
        return BacsConfirmationDefinition(bacsMandateConfirmationLauncherFactory)
    }
}
