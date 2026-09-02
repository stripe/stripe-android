@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class CheckoutSavedPaymentMethodSelectionHandlerTest {
    @Test
    fun `successful update commits refreshed response and selection before callback`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val originalResponse = CheckoutSessionResponseFactory.create()
        val refreshedResponse = CheckoutSessionResponseFactory.create().copy(liveMode = true)
        val selection = selectionWithBillingAddress()
        val scenario = createScenario(originalResponse, coroutineScope = this)
        whenever(
            scenario.taxRegionUpdater.updateServerStateIfNeeded(
                checkoutSessionResponse = any(),
                addressSource = any(),
                address = any(),
            )
        ).thenReturn(Result.success(refreshedResponse))

        scenario.handler.select(selection)

        val stateCaptor = argumentCaptor<CheckoutControllerState>()
        verify(scenario.stateLoader).reload(stateCaptor.capture())
        assertThat(stateCaptor.firstValue.checkoutSessionResponse).isEqualTo(refreshedResponse)
        assertThat(stateCaptor.firstValue.paymentSelection).isEqualTo(selection)
        assertThat(scenario.callbackCalls.awaitItem()).isEqualTo(Unit)
        assertThat(scenario.handler.pendingSelection.value).isNull()
        assertThat(scenario.handler.error.value).isNull()
    }

    @Test
    fun `failed update preserves prior selection and does not invoke callback`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val expectedError = IllegalStateException("update failed")
        val priorSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val scenario = createScenario(
            response = CheckoutSessionResponseFactory.create(),
            priorSelection = priorSelection,
            coroutineScope = this,
        )
        whenever(
            scenario.taxRegionUpdater.updateServerStateIfNeeded(any(), any(), any())
        ).thenReturn(Result.failure(expectedError))

        scenario.handler.select(selectionWithBillingAddress())

        verify(scenario.stateLoader, never()).reload(any())
        scenario.callbackCalls.expectNoEvents()
        assertThat(scenario.stateHolder.state?.paymentSelection).isEqualTo(priorSelection)
        assertThat(scenario.handler.pendingSelection.value).isNull()
        assertThat(scenario.handler.error.value).isSameInstanceAs(expectedError)
    }

    @Test
    fun `selection without billing address reloads and invokes callback without tax update`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val scenario = createScenario(CheckoutSessionResponseFactory.create(), coroutineScope = this)
        val selection = PaymentSelection.Saved(
            PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(billingDetails = null)
        )

        scenario.handler.select(selection)

        verify(scenario.taxRegionUpdater, never()).updateServerStateIfNeeded(any(), any(), any())
        verify(scenario.stateLoader).reload(any())
        assertThat(scenario.callbackCalls.awaitItem()).isEqualTo(Unit)
    }

    private fun createScenario(
        response: CheckoutSessionResponse,
        priorSelection: PaymentSelection? = null,
        coroutineScope: CoroutineScope,
    ): Scenario {
        val stateHolder = CheckoutControllerStateFactory.createStateHolder(SavedStateHandle()).apply {
            state = CheckoutControllerStateFactory.create(
                checkoutSessionResponse = response,
                paymentSelection = priorSelection,
            )
        }
        val callbackCalls = Turbine<Unit>()
        val stateLoader = mock<CheckoutStateLoader>()
        val taxRegionUpdater = mock<CheckoutSessionTaxRegionUpdater>()
        val operationCoordinator = CheckoutOperationCoordinator(
            confirmationHandler = FakeConfirmationHandler(),
            sheetStateHolder = SheetStateHolder(SavedStateHandle()),
            sessionRefresher = mock(),
            logger = FakeLogger(),
            resultCallback = {},
        )
        val handler = CheckoutSavedPaymentMethodSelectionHandler(
            stateHolder = stateHolder,
            operationCoordinator = operationCoordinator,
            taxRegionUpdater = taxRegionUpdater,
            stateLoader = stateLoader,
            immediateActionHandler = { callbackCalls.add(Unit) },
            coroutineScope = coroutineScope,
        )
        return Scenario(handler, stateHolder, stateLoader, taxRegionUpdater, callbackCalls)
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
        val stateHolder: CheckoutControllerStateHolder,
        val stateLoader: CheckoutStateLoader,
        val taxRegionUpdater: CheckoutSessionTaxRegionUpdater,
        val callbackCalls: Turbine<Unit>,
    )
}
