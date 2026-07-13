package com.stripe.android.paymentelement.embedded.content

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

internal class EmbeddedSelectionChooserResolverTest {

    @Test
    fun `delegates to chooser and threads previous and new selection for Embedded configuration`() {
        var capturedPrevious: PaymentSelection? = null
        var capturedNew: PaymentSelection? = null
        val chooserResult = PaymentSelection.GooglePay

        val resolver = EmbeddedSelectionChooserResolver(
            chooser = { _, _, previousSelection, newSelection, _, _ ->
                capturedPrevious = previousSelection
                capturedNew = newSelection
                chooserResult
            },
        )

        val loaderSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val result = resolver.resolve(
            state = state(paymentSelection = loaderSelection),
            integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                isRowSelectionImmediateAction = false,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example").build(),
            ),
            reconfigureContext = PaymentElementLoader.ReconfigureContext(
                previousSelection = PaymentSelection.GooglePay,
            ),
        )

        assertThat(result).isEqualTo(chooserResult)
        assertThat(capturedPrevious).isEqualTo(PaymentSelection.GooglePay)
        assertThat(capturedNew).isEqualTo(loaderSelection)
    }

    @Test
    fun `returns loader selection without invoking chooser for non-Embedded configuration`() {
        var chooserInvoked = false
        val resolver = EmbeddedSelectionChooserResolver(
            chooser = { _, _, _, _, _, _ ->
                chooserInvoked = true
                PaymentSelection.GooglePay
            },
        )

        val result = resolver.resolve(
            state = state(paymentSelection = PaymentSelection.GooglePay),
            integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
                PaymentSheet.Configuration("Example"),
            ),
            reconfigureContext = null,
        )

        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
        assertThat(chooserInvoked).isFalse()
    }

    private fun state(paymentSelection: PaymentSelection?) = PaymentElementLoader.State(
        config = PaymentSheet.Configuration("Example").asCommonConfiguration(),
        customer = null,
        paymentSelection = paymentSelection,
        validationError = null,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    )
}
