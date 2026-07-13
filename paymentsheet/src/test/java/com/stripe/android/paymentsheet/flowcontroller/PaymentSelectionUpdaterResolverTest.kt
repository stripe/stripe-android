package com.stripe.android.paymentsheet.flowcontroller

import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

internal class PaymentSelectionUpdaterResolverTest {

    @Test
    fun `delegates to updater and threads reconfigure context for PaymentSheet configuration`() {
        var capturedSelection: PaymentSelection? = null
        var capturedPreviousConfig: PaymentSheet.Configuration? = null
        var capturedWalletButtonsAlreadyShown: Boolean? = null
        val updaterResult = PaymentSelection.Link(brand = LinkBrand.Link)

        val previousConfig = PaymentSheet.Configuration("Previous")
        val resolver = PaymentSelectionUpdaterResolver(
            updater = { selection, previousConfigArg, _, _, walletButtonsAlreadyShown ->
                capturedSelection = selection
                capturedPreviousConfig = previousConfigArg
                capturedWalletButtonsAlreadyShown = walletButtonsAlreadyShown
                updaterResult
            },
        )

        val result = resolver.resolve(
            state = state(paymentSelection = PaymentSelection.GooglePay),
            integrationConfiguration = PaymentElementLoader.Configuration.PaymentSheet(
                PaymentSheet.Configuration("Example"),
            ),
            reconfigureContext = PaymentElementLoader.ReconfigureContext(
                previousSelection = PaymentSelection.GooglePay,
                previousPaymentSheetConfig = previousConfig,
                walletButtonsAlreadyShown = true,
            ),
        )

        assertThat(result).isEqualTo(updaterResult)
        assertThat(capturedSelection).isEqualTo(PaymentSelection.GooglePay)
        assertThat(capturedPreviousConfig).isEqualTo(previousConfig)
        assertThat(capturedWalletButtonsAlreadyShown).isTrue()
    }

    @Test
    fun `returns loader selection without invoking updater for non-PaymentSheet configuration`() {
        var updaterInvoked = false
        val resolver = PaymentSelectionUpdaterResolver(
            updater = { _, _, _, _, _ ->
                updaterInvoked = true
                PaymentSelection.Link(brand = LinkBrand.Link)
            },
        )

        val result = resolver.resolve(
            state = state(paymentSelection = PaymentSelection.GooglePay),
            integrationConfiguration = PaymentElementLoader.Configuration.Embedded(
                isRowSelectionImmediateAction = false,
                configuration = EmbeddedPaymentElement.Configuration.Builder("Example").build(),
            ),
            reconfigureContext = null,
        )

        assertThat(result).isEqualTo(PaymentSelection.GooglePay)
        assertThat(updaterInvoked).isFalse()
    }

    private fun state(paymentSelection: PaymentSelection?) = PaymentElementLoader.State(
        config = PaymentSheet.Configuration("Example").asCommonConfiguration(),
        customer = null,
        paymentSelection = paymentSelection,
        validationError = null,
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
    )
}
