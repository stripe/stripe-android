@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.state.PaymentElementLoader

internal object EmbeddedConfirmationStateFixtures {
    fun defaultState(): EmbeddedConfirmationStateHolder.State = EmbeddedConfirmationStateHolder.State(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        selection = null,
        initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            intentConfiguration = PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Payment(amount = 5000, currency = "USD"),
            )
        ),
        configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build()
    )
}
