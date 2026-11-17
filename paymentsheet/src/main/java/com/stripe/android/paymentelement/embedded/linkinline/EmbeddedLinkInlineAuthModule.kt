package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Inject

internal class DefaultEmbeddedLinkInlineVerificationGate @Inject constructor(
    private val linkInlineAuth: EmbeddedLinkInlineAuth,
) : EmbeddedLinkInlineVerificationGate {
    override fun isEligible(
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): Boolean {
        val embedded = integrationConfiguration as? PaymentElementLoader.Configuration.Embedded ?: return false
        return linkInlineAuth.isInExperiment(
            paymentMethodMetadata = metadata,
            customer = customer,
            configuration = embedded.configuration,
        )
    }
}

@Module
internal interface EmbeddedLinkInlineAuthModule {
    @Binds
    fun bindsEmbeddedLinkInlineAuth(impl: DefaultEmbeddedLinkInlineAuth): EmbeddedLinkInlineAuth

    @Binds
    fun bindsEmbeddedLinkInlineVerificationGate(
        impl: DefaultEmbeddedLinkInlineVerificationGate,
    ): EmbeddedLinkInlineVerificationGate

    companion object {
        @Provides
        fun providesEmbeddedLinkInlineAuthRegistry(): EmbeddedLinkInlineAuthRegistry {
            return EmbeddedLinkInlineAuthRegistry.shared
        }
    }
}
