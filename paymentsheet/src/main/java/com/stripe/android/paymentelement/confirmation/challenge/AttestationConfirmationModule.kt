package com.stripe.android.paymentelement.confirmation.challenge

import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module(includes = [PaymentsIntegrityModule::class])
internal interface AttestationConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsAttestationConfirmationDefinition(
        definition: AttestationConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}