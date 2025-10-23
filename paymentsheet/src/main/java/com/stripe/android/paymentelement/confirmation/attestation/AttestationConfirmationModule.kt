package com.stripe.android.paymentelement.confirmation.attestation

import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet

@Module
internal interface AttestationConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsAttestationConfirmationDefinition(
        definition: AttestationConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>
}
