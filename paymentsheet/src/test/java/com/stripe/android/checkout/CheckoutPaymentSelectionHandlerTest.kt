@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

internal class CheckoutPaymentSelectionHandlerTest {
    @Test
    fun `saved selection completes only after controller refresh succeeds`() = runTest {
        val selection = savedSelection()
        val controller = mock<CheckoutController>()
        whenever(controller.isUpdating).thenReturn(MutableStateFlow(false))
        whenever { controller.selectSavedPaymentMethod(selection) }.thenReturn(Result.success(Unit))
        val scenario = createScenario(controller, this)

        scenario.handler.select(selection, true)

        assertThat(scenario.completions.awaitItem()).isEqualTo(Unit)
        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller).isUpdating
        verify(controller).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
        scenario.ensureAllEventsConsumed()
    }

    @Test
    fun `saved selection is rejected while Checkout is updating`() = runTest {
        val selection = savedSelection()
        val controller = mock<CheckoutController>()
        whenever(controller.isUpdating).thenReturn(MutableStateFlow(true))
        val scenario = createScenario(controller, this)

        scenario.handler.select(selection, true)

        scenario.completions.expectNoEvents()
        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller).isUpdating
        verify(controller, never()).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
        scenario.ensureAllEventsConsumed()
    }

    @Test
    fun `undispatched controller admission rejects a duplicate selection`() = runTest {
        val firstSelection = savedSelection()
        val secondSelection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_second")
        )
        val controller = mock<CheckoutController>()
        val isUpdating = MutableStateFlow(false)
        whenever(controller.isUpdating).thenReturn(isUpdating)
        whenever { controller.selectSavedPaymentMethod(firstSelection) }.thenAnswer {
            isUpdating.value = true
            Result.success(Unit)
        }
        val scenario = createScenario(controller, this)

        scenario.handler.select(firstSelection, true)
        scenario.handler.select(secondSelection, true)

        assertThat(scenario.completions.awaitItem()).isEqualTo(Unit)
        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller, times(2)).isUpdating
        verify(controller).selectSavedPaymentMethod(firstSelection)
        verify(controller, never()).selectSavedPaymentMethod(secondSelection)
        verifyNoMoreInteractions(controller)
        scenario.ensureAllEventsConsumed()
    }

    @Test
    fun `failed saved selection can retry without completing`() = runTest {
        val selection = savedSelection()
        val controller = mock<CheckoutController>()
        whenever(controller.isUpdating).thenReturn(MutableStateFlow(false))
        whenever { controller.selectSavedPaymentMethod(selection) }.thenReturn(
            Result.failure(IllegalStateException("update failed")),
            Result.success(Unit),
        )
        val scenario = createScenario(controller, this)

        scenario.handler.select(selection, true)

        scenario.completions.expectNoEvents()

        scenario.handler.select(selection, true)

        assertThat(scenario.completions.awaitItem()).isEqualTo(Unit)
        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller, times(2)).isUpdating
        verify(controller, times(2)).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
        scenario.ensureAllEventsConsumed()
    }

    @Test
    fun `Google Pay updates selection before completion without refreshing Checkout`() = runTest {
        val controller = mock<CheckoutController>()
        whenever(controller.isUpdating).thenReturn(MutableStateFlow(false))
        val scenario = createScenario(controller, this)

        scenario.handler.select(PaymentSelection.GooglePay, false)

        assertThat(scenario.selectionHolder.selectionCalls.awaitItem()).isEqualTo(PaymentSelection.GooglePay)
        assertThat(scenario.completions.awaitItem()).isEqualTo(Unit)
        verifyNoInteractions(controller)
        scenario.ensureAllEventsConsumed()
    }

    @Test
    fun `Link updates selection before completion without refreshing Checkout`() = runTest {
        val controller = mock<CheckoutController>()
        whenever(controller.isUpdating).thenReturn(MutableStateFlow(false))
        val scenario = createScenario(controller, this)
        val selection = PaymentSelection.Link(brand = com.stripe.android.model.LinkBrand.Link)

        scenario.handler.select(selection, false)

        assertThat(scenario.selectionHolder.selectionCalls.awaitItem()).isEqualTo(selection)
        assertThat(scenario.completions.awaitItem()).isEqualTo(Unit)
        verifyNoInteractions(controller)
        scenario.ensureAllEventsConsumed()
    }

    private fun createScenario(
        controller: CheckoutController,
        coroutineScope: CoroutineScope,
    ): Scenario {
        val selectionHolder = FakeEmbeddedSelectionHolder()
        val completions = Turbine<Unit>()
        val handler = CheckoutPaymentSelectionHandler(
            checkoutController = controller,
            selectionHolder = selectionHolder,
            immediateActionHandler = { completions.add(Unit) },
            coroutineScope = coroutineScope,
        )
        return Scenario(handler, selectionHolder, completions)
    }

    private fun savedSelection(): PaymentSelection.Saved {
        return PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
    }

    private data class Scenario(
        val handler: CheckoutPaymentSelectionHandler,
        val selectionHolder: FakeEmbeddedSelectionHolder,
        val completions: Turbine<Unit>,
    ) {
        fun ensureAllEventsConsumed() {
            selectionHolder.selectionCalls.ensureAllEventsConsumed()
            completions.ensureAllEventsConsumed()
        }
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
}
