@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import android.os.Bundle
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.VerticalPaymentSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

internal class CheckoutPaymentSelectionHandlerTest {
    @Test
    fun `successful saved selection delegates to controller and invokes callback`() = runTest {
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }.thenReturn(Result.success(Unit))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)

            scenario.handler.select(selection, true)

            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            scenario.callbackStates.expectNoEvents()
            runCurrent()
            assertThat(scenario.callbackStates.awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `saved selection ignores duplicate clicks while pending`() = runTest {
        val firstSelection = selectionWithBillingAddress()
        val secondSelection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_second")
        )
        val controller = mock<CheckoutController>()
        whenever {
            controller.selectSavedPaymentMethod(firstSelection)
        }.thenReturn(Result.success(Unit))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)

            scenario.handler.select(firstSelection, true)
            scenario.handler.select(secondSelection, true)

            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(firstSelection)
            )
            expectNoEvents()
            runCurrent()
            assertThat(scenario.callbackStates.awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(firstSelection)
            )
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller).selectSavedPaymentMethod(firstSelection)
        verify(controller, never()).selectSavedPaymentMethod(secondSelection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `saved selection failure clears pending, exposes error, and suppresses callback`() = runTest {
        val expectedError = IllegalStateException("update failed")
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }
            .thenReturn(Result.failure(expectedError))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)

            scenario.handler.select(selection, true)

            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Failed(expectedError)
            )
            ensureAllEventsConsumed()
        }

        scenario.selectionHolder.selectionCalls.expectNoEvents()
        scenario.callbackStates.expectNoEvents()
        verify(controller).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `saved selection can retry after failure`() = runTest {
        val expectedError = IllegalStateException("update failed")
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }
            .thenReturn(Result.failure(expectedError), Result.success(Unit))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)

            scenario.handler.select(selection, true)
            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Failed(expectedError)
            )

            scenario.handler.select(selection, true)
            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(scenario.callbackStates.awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        scenario.selectionHolder.selectionCalls.expectNoEvents()
        verify(controller, org.mockito.kotlin.times(2)).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `clearFailure returns failed handler to idle`() = runTest {
        val expectedError = IllegalStateException("update failed")
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }
            .thenReturn(Result.failure(expectedError))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)

            scenario.handler.select(selection, true)
            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                VerticalPaymentSelectionHandler.State.Failed(expectedError)
            )

            scenario.handler.clearFailure()

            assertThat(awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        scenario.selectionHolder.selectionCalls.expectNoEvents()
        scenario.callbackStates.expectNoEvents()
        verify(controller).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `Google Pay updates selection before completion without refreshing Checkout`() = runTest {
        val controller = mock<CheckoutController>()
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.select(PaymentSelection.GooglePay, false)

        assertThat(scenario.selectionHolder.selectionCalls.awaitItem()).isEqualTo(PaymentSelection.GooglePay)
        assertThat(scenario.callbackStates.awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
        verifyNoInteractions(controller)
    }

    @Test
    fun `Link updates selection before completion without refreshing Checkout`() = runTest {
        val controller = mock<CheckoutController>()
        val scenario = createScenario(controller, coroutineScope = this)
        val selection = PaymentSelection.Link(brand = com.stripe.android.model.LinkBrand.Link)

        scenario.handler.select(selection, false)

        assertThat(scenario.selectionHolder.selectionCalls.awaitItem()).isEqualTo(selection)
        assertThat(scenario.callbackStates.awaitItem()).isEqualTo(VerticalPaymentSelectionHandler.State.Idle)
        verifyNoInteractions(controller)
    }

    private fun createScenario(
        controller: CheckoutController,
        coroutineScope: CoroutineScope,
    ): Scenario {
        val selectionHolder = FakeEmbeddedSelectionHolder()
        val callbackStates = Turbine<VerticalPaymentSelectionHandler.State>()
        lateinit var handler: CheckoutPaymentSelectionHandler
        handler = CheckoutPaymentSelectionHandler(
            checkoutController = controller,
            selectionHolder = selectionHolder,
            immediateActionHandler = { callbackStates.add(handler.state.value) },
            coroutineScope = coroutineScope,
        )
        return Scenario(handler, selectionHolder, callbackStates)
    }

    private fun selectionWithBillingAddress(): PaymentSelection.Saved {
        return PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = PaymentMethod.BillingDetails(
                    address = Address(country = "US", postalCode = "94107")
                )
            )
        )
    }

    private data class Scenario(
        val handler: CheckoutPaymentSelectionHandler,
        val selectionHolder: FakeEmbeddedSelectionHolder,
        val callbackStates: Turbine<VerticalPaymentSelectionHandler.State>,
    )
}

private class FakeEmbeddedSelectionHolder : EmbeddedSelectionHolder {
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
