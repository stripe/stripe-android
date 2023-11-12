package com.stripe.android.paymentsheet.prototype

import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.elements.SharedDataSpec

// TODO: Figure out a better name or place to put this.
internal class Foo {
    // TODO: Add a test for ensuring there aren't any duplicate keys for state.
    suspend fun parseStripeIntent(
        stripeIntent: StripeIntent,
        configuration: PaymentSheet.Configuration,
        sharedDataSpecs: List<SharedDataSpec>,
        isDeferred: Boolean,
    ): UiState {
        val metadata = ParsingMetadata(
            stripeIntent = stripeIntent,
            configuration = configuration,
            sharedDataSpecs = sharedDataSpecs,
            isDeferred = isDeferred,
        )
        val paymentMethodDefinitionMap = PaymentMethodDefinitionRegistry.all.associateBy { it.type.code }
        return UiState.create(
            stripeIntent.paymentMethodTypes.mapNotNull { type ->
                paymentMethodDefinitionMap[type]
            }.filter { paymentMethodDefinition ->
                paymentMethodDefinition.isSupported(metadata)
            }.map { paymentMethodDefinition ->
                paymentMethodDefinition.initialAddState(metadata = metadata)
            }
        )
    }
}
