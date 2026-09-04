@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.VerticalSavedPaymentMethodSelectionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

internal class CheckoutSavedPaymentMethodSelectionHandlerTest {
    @Test
    fun `successful selection delegates to controller and invokes callback`() = runTest {
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }.thenReturn(Result.success(Unit))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)

            scenario.handler.select(selection)

            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            scenario.callbackStates.expectNoEvents()
            runCurrent()
            assertThat(scenario.callbackStates.awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        verify(controller).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `selection ignores duplicate clicks while pending`() = runTest {
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
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)

            scenario.handler.select(firstSelection)
            scenario.handler.select(secondSelection)

            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(firstSelection)
            )
            expectNoEvents()
            runCurrent()
            assertThat(scenario.callbackStates.awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(firstSelection)
            )
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        verify(controller).selectSavedPaymentMethod(firstSelection)
        verify(controller, never()).selectSavedPaymentMethod(secondSelection)
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `controller failure clears pending, exposes error, and suppresses callback`() = runTest {
        val expectedError = IllegalStateException("update failed")
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }
            .thenReturn(Result.failure(expectedError))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)

            scenario.handler.select(selection)

            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Failed(expectedError)
            )
            ensureAllEventsConsumed()
        }

        verify(controller).selectSavedPaymentMethod(selection)
        scenario.callbackStates.expectNoEvents()
        verifyNoMoreInteractions(controller)
    }

    @Test
    fun `selection can retry after failure`() = runTest {
        val expectedError = IllegalStateException("update failed")
        val selection = selectionWithBillingAddress()
        val controller = mock<CheckoutController>()
        whenever { controller.selectSavedPaymentMethod(selection) }
            .thenReturn(Result.failure(expectedError), Result.success(Unit))
        val scenario = createScenario(controller, coroutineScope = this)

        scenario.handler.state.test {
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)

            scenario.handler.select(selection)
            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Failed(expectedError)
            )

            scenario.handler.select(selection)
            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(scenario.callbackStates.awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

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
            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)

            scenario.handler.select(selection)
            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Selecting(selection)
            )
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                VerticalSavedPaymentMethodSelectionHandler.State.Failed(expectedError)
            )

            scenario.handler.clearFailure()

            assertThat(awaitItem()).isEqualTo(VerticalSavedPaymentMethodSelectionHandler.State.Idle)
            ensureAllEventsConsumed()
        }

        scenario.callbackStates.expectNoEvents()
        verify(controller).selectSavedPaymentMethod(selection)
        verifyNoMoreInteractions(controller)
    }

    private fun createScenario(
        controller: CheckoutController,
        coroutineScope: CoroutineScope,
    ): Scenario {
        val callbackStates = Turbine<VerticalSavedPaymentMethodSelectionHandler.State>()
        lateinit var handler: CheckoutSavedPaymentMethodSelectionHandler
        handler = CheckoutSavedPaymentMethodSelectionHandler(
            checkoutController = controller,
            immediateActionHandler = { callbackStates.add(handler.state.value) },
            coroutineScope = coroutineScope,
        )
        return Scenario(handler, callbackStates)
    }

    private fun selectionWithBillingAddress(): PaymentSelection.Saved {
        return PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                billingDetails = com.stripe.android.model.PaymentMethod.BillingDetails(
                    address = Address(country = "US", postalCode = "94107")
                )
            )
        )
    }

    private data class Scenario(
        val handler: CheckoutSavedPaymentMethodSelectionHandler,
        val callbackStates: Turbine<VerticalSavedPaymentMethodSelectionHandler.State>,
    )
}
