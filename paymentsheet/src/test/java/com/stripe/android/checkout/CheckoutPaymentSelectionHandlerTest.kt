@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.EmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class CheckoutPaymentSelectionHandlerTest {
    @Test
    fun `saved selection completes only after selector succeeds`() = runScenario {
        val selection = savedSelection()

        handler.select(selection, true)

        assertThat(savedPaymentMethodSelector.selectCalls.awaitItem()).isEqualTo(selection)
        assertThat(immediateActionHandler.calls.awaitItem()).isEqualTo(Unit)
        selectionHolder.selectionCalls.expectNoEvents()
    }

    @Test
    fun `saved selection is rejected while Checkout is updating`() = runScenario(
        isProcessing = true,
    ) {
        handler.select(savedSelection(), true)

        savedPaymentMethodSelector.selectCalls.expectNoEvents()
        immediateActionHandler.calls.expectNoEvents()
        selectionHolder.selectionCalls.expectNoEvents()
    }

    @Test
    fun `undispatched selector admission rejects a duplicate selection`() = runScenario {
        val firstSelection = savedSelection()
        val secondSelection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_second")
        )
        savedPaymentMethodSelector.onSelect = { processing.value = true }

        handler.select(firstSelection, true)
        handler.select(secondSelection, true)

        assertThat(savedPaymentMethodSelector.selectCalls.awaitItem()).isEqualTo(firstSelection)
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
        assertThat(immediateActionHandler.calls.awaitItem()).isEqualTo(Unit)
        selectionHolder.selectionCalls.expectNoEvents()
    }

    @Test
    fun `failed saved selection can retry without completing`() = runScenario {
        val selection = savedSelection()
        savedPaymentMethodSelector.result = Result.failure(IllegalStateException("update failed"))

        handler.select(selection, true)

        assertThat(savedPaymentMethodSelector.selectCalls.awaitItem()).isEqualTo(selection)
        immediateActionHandler.calls.expectNoEvents()

        savedPaymentMethodSelector.result = Result.success(Unit)
        handler.select(selection, true)

        assertThat(savedPaymentMethodSelector.selectCalls.awaitItem()).isEqualTo(selection)
        assertThat(immediateActionHandler.calls.awaitItem()).isEqualTo(Unit)
        selectionHolder.selectionCalls.expectNoEvents()
    }

    @Test
    fun `Google Pay updates selection before completion without selecting a saved method`() = runScenario {
        immediateActionHandler.onInvoke = {
            assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.GooglePay)
        }

        handler.select(PaymentSelection.GooglePay, false)

        assertThat(selectionHolder.selectionCalls.awaitItem()).isEqualTo(PaymentSelection.GooglePay)
        assertThat(immediateActionHandler.calls.awaitItem()).isEqualTo(Unit)
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
    }

    @Test
    fun `Link updates selection before completion without selecting a saved method`() = runScenario {
        val selection = PaymentSelection.Link(brand = com.stripe.android.model.LinkBrand.Link)
        immediateActionHandler.onInvoke = {
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
        }

        handler.select(selection, false)

        assertThat(selectionHolder.selectionCalls.awaitItem()).isEqualTo(selection)
        assertThat(immediateActionHandler.calls.awaitItem()).isEqualTo(Unit)
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
    }

    private fun runScenario(
        isProcessing: Boolean = false,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedPaymentMethodSelector = FakeCheckoutSavedPaymentMethodSelector()
        val processing = MutableStateFlow(isProcessing)
        val selectionHolder = FakeEmbeddedSelectionHolder()
        val immediateActionHandler = FakeEmbeddedRowSelectionImmediateActionHandler()
        val handler = CheckoutPaymentSelectionHandler(
            savedPaymentMethodSelector = savedPaymentMethodSelector,
            processing = processing,
            selectionHolder = selectionHolder,
            immediateActionHandler = immediateActionHandler,
            coroutineScope = this,
        )

        Scenario(
            handler = handler,
            savedPaymentMethodSelector = savedPaymentMethodSelector,
            processing = processing,
            selectionHolder = selectionHolder,
            immediateActionHandler = immediateActionHandler,
        ).apply { block() }

        savedPaymentMethodSelector.ensureAllEventsConsumed()
        selectionHolder.ensureAllEventsConsumed()
        immediateActionHandler.ensureAllEventsConsumed()
    }

    private fun savedSelection(): PaymentSelection.Saved {
        return PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    private data class Scenario(
        val handler: CheckoutPaymentSelectionHandler,
        val savedPaymentMethodSelector: FakeCheckoutSavedPaymentMethodSelector,
        val processing: MutableStateFlow<Boolean>,
        val selectionHolder: FakeEmbeddedSelectionHolder,
        val immediateActionHandler: FakeEmbeddedRowSelectionImmediateActionHandler,
    )
}

internal class FakeCheckoutSavedPaymentMethodSelector : CheckoutSavedPaymentMethodSelector {
    val selectCalls = Turbine<PaymentSelection.Saved>()
    var result: Result<Unit> = Result.success(Unit)
    var onSelect: (PaymentSelection.Saved) -> Unit = {}

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        selectCalls.add(selection)
        onSelect(selection)
        return result
    }

    fun ensureAllEventsConsumed() {
        selectCalls.ensureAllEventsConsumed()
    }
}

internal class FakeEmbeddedRowSelectionImmediateActionHandler : EmbeddedRowSelectionImmediateActionHandler {
    val calls = Turbine<Unit>()
    var onInvoke: () -> Unit = {}

    override fun invoke() {
        calls.add(Unit)
        onInvoke()
    }

    fun ensureAllEventsConsumed() {
        calls.ensureAllEventsConsumed()
    }
}

internal class FakeEmbeddedSelectionHolder : EmbeddedSelectionHolder {
    private val selectionSource = MutableStateFlow<PaymentSelection?>(null)
    override val selection: StateFlow<PaymentSelection?> = selectionSource
    override val temporarySelection: StateFlow<String?> = MutableStateFlow(null)
    override val previousNewSelections = Bundle()
    val selectionCalls = Turbine<PaymentSelection?>()

    override fun setSelection(updatedSelection: PaymentSelection?) {
        selectionSource.value = updatedSelection
        selectionCalls.add(updatedSelection)
    }

    override fun setTemporarySelection(code: PaymentMethodCode?) = Unit

    override fun setPreviousNewSelections(bundle: Bundle) = Unit

    override fun getPreviousNewSelection(code: PaymentMethodCode): PaymentSelection.New? = null

    fun ensureAllEventsConsumed() {
        selectionCalls.ensureAllEventsConsumed()
    }
}
