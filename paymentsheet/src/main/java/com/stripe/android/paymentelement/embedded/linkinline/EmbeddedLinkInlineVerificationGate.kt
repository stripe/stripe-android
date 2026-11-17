package com.stripe.android.paymentelement.embedded.linkinline

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import dagger.Module
import dagger.Provides

/**
 * Whether a session takes part in inline Link verification in the Embedded Payment Element, in any experiment group.
 * Those sessions stay out of other Link experiments.
 */
internal fun interface EmbeddedLinkInlineVerificationGate {
    fun isEligible(
        metadata: PaymentMethodMetadata,
        customer: CustomerState?,
        integrationConfiguration: PaymentElementLoader.Configuration,
    ): Boolean

    /**
     * For integrations other than the Embedded Payment Element.
     */
    object Never : EmbeddedLinkInlineVerificationGate {
        override fun isEligible(
            metadata: PaymentMethodMetadata,
            customer: CustomerState?,
            integrationConfiguration: PaymentElementLoader.Configuration,
        ): Boolean = false
    }
}

@Module
internal object NoEmbeddedLinkInlineVerificationGateModule {
    @Provides
    fun providesEmbeddedLinkInlineVerificationGate(): EmbeddedLinkInlineVerificationGate {
        return EmbeddedLinkInlineVerificationGate.Never
    }
}
