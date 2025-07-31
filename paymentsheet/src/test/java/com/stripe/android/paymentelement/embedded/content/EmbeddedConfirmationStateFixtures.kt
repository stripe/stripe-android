
package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.elements.payment.IntentConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal object EmbeddedConfirmationStateFixtures {
    fun defaultState(): EmbeddedConfirmationStateHolder.State = EmbeddedConfirmationStateHolder.State(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        selection = null,
        initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = IntentConfiguration(
                mode = IntentConfiguration.Mode.Payment(amount = 5000, currency = "USD"),
            )
        ),
        configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            .build()
    )
}
