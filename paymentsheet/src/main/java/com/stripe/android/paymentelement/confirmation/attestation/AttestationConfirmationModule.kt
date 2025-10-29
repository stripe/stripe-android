package com.stripe.android.paymentelement.confirmation.attestation

import com.stripe.android.link.injection.PaymentsIntegrityModule
import com.stripe.android.paymentelement.confirmation.ConfirmationDefinition
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope

@Module(includes = [PaymentsIntegrityModule::class])
internal interface AttestationConfirmationModule {
    @JvmSuppressWildcards
    @Binds
    @IntoSet
    fun bindsAttestationConfirmationDefinition(
        definition: AttestationConfirmationDefinition
    ): ConfirmationDefinition<*, *, *, *>

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @Provides
        @AttestationScope
        fun providesCoroutineScope(): CoroutineScope = GlobalScope
    }
}
