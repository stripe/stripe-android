package com.stripe.android.paymentelement.embedded.linkinline

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class EmbeddedVerticalPaymentSelectionHandlerTest {

    @Test
    fun `tapping a row selects it and completes the selection`() = runScenario {
        handler.select(PaymentSelection.GooglePay, isUserInput = false)

        linkInlineAuth.endTypingCalls.awaitItem()
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
        immediateActionCalls.awaitItem()
    }

    @Test
    fun `tapping the Link row selects Link when it doesn't open the wallet`() = runScenario {
        handler.select(LINK, isUserInput = false)

        linkInlineAuth.endTypingCalls.awaitItem()
        linkInlineAuth.onLinkRowSelectedCalls.awaitItem()
        assertThat(selectionHolder.selection.value).isEqualTo(LINK)
        immediateActionCalls.awaitItem()
    }

    @Test
    fun `tapping the Link row that opens the wallet selects nothing yet`() = runScenario(linkRowOpensWallet = true) {
        handler.select(LINK, isUserInput = false)

        linkInlineAuth.endTypingCalls.awaitItem()
        linkInlineAuth.onLinkRowSelectedCalls.awaitItem()
        assertThat(selectionHolder.selection.value).isNull()
    }

    private fun runScenario(
        linkRowOpensWallet: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val immediateActionCalls = Turbine<Unit>()
        val linkInlineAuth = FakeEmbeddedLinkInlineAuth(linkRowOpensWallet = linkRowOpensWallet)
        val handler = EmbeddedVerticalPaymentSelectionHandler(
            selectionHolder = selectionHolder,
            immediateActionHandler = { immediateActionCalls.add(Unit) },
            linkInlineAuth = linkInlineAuth,
        )

        Scenario(
            handler = handler,
            selectionHolder = selectionHolder,
            immediateActionCalls = immediateActionCalls,
            linkInlineAuth = linkInlineAuth,
        ).block()

        immediateActionCalls.ensureAllEventsConsumed()
        linkInlineAuth.ensureAllEventsConsumed()
    }

    private class Scenario(
        val handler: EmbeddedVerticalPaymentSelectionHandler,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val immediateActionCalls: Turbine<Unit>,
        val linkInlineAuth: FakeEmbeddedLinkInlineAuth,
    )

    private companion object {
        val LINK = PaymentSelection.Link(brand = LinkBrand.Link)
    }
}
