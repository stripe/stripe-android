
package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentelement.EmbeddedPaymentElement

internal object EmbeddedConfirmationStateFixtures {
    fun defaultState(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create()
    ): EmbeddedConfirmationStateHolder.State = EmbeddedConfirmationStateHolder.State(
        paymentMethodMetadata = paymentMethodMetadata,
        selection = null,
        configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            .build()
    )

    fun defaultStateWithOpenCardScanAutomatically(): EmbeddedConfirmationStateHolder.State =
        EmbeddedConfirmationStateHolder.State(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            selection = null,
            configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
                .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
                .opensCardScannerAutomatically(true)
                .build()
        )
}
