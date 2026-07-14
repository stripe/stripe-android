package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.LoadedPaymentSelectionResolver
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import javax.inject.Inject

/**
 * Adapts [EmbeddedSelectionChooser] to the loader's [LoadedPaymentSelectionResolver] contract so
 * embedded selection-preservation logic can run in [DefaultEmbeddedConfigurationHandler] on both
 * fresh loads and cache hits.
 */
internal class EmbeddedSelectionChooserResolver @Inject constructor(
    private val chooser: EmbeddedSelectionChooser,
) : LoadedPaymentSelectionResolver {
    override fun resolve(
        state: PaymentElementLoader.State,
        integrationConfiguration: PaymentElementLoader.Configuration,
        reconfigureContext: PaymentElementLoader.ReconfigureContext?,
    ): PaymentSelection? {
        val embedded = integrationConfiguration as? PaymentElementLoader.Configuration.Embedded
            ?: return state.paymentSelection

        return chooser.choose(
            paymentMethodMetadata = state.paymentMethodMetadata,
            paymentMethods = state.customer?.paymentMethods,
            previousSelection = reconfigureContext?.previousSelection,
            newSelection = state.paymentSelection,
            newConfiguration = embedded.configuration.asCommonConfiguration(),
            formSheetAction = embedded.configuration.formSheetAction,
        )
    }
}
