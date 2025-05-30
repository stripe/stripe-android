@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.content.DefaultPaymentOptionDisplayDataHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.content.PaymentOptionDisplayDataFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import javax.inject.Provider
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class EmbeddedRowSelectionImmediateActionHandlerTest {
    val coroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    @Test
    fun `setting new selection on null selection invokes callback after paymentOption changes`() = runTest {
        val callbackOrder = mutableListOf<String>()
        testScenario(
            coroutineScope = coroutineScope,
            callback = { { callbackOrder.add("callback") } }
        ) {
            assertThat(selectionHolder.selection.value).isNull()
            assertThat(paymentOptionDisplayDataHolder.paymentOption.value).isNull()

            val newPaymentSelection = PaymentMethodFixtures.GENERIC_PAYMENT_SELECTION
            selectionHolder.set(newPaymentSelection)
            rowSelectionHandler.handleImmediateRowSelectionCallback()

            paymentOptionDisplayDataHolder.paymentOption.test {
                assertThat(awaitItem()?.paymentMethodType).isEqualTo(PaymentMethod.Type.PayPal.code)
                callbackOrder.add("paymentOptionChanged")
                advanceUntilIdle()
                assertThat(callbackOrder).containsExactly("paymentOptionChanged", "callback")
            }
        }
    }

    @Test
    fun `setting new selection on non null selection invokes callback after paymentOption changes`() = runTest {
        val callbackOrder = mutableListOf<String>()
        testScenario(
            coroutineScope = coroutineScope,
            callback = { { callbackOrder.add("callback") } },
            setupSavedStateHandle = {
                set(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            }
        ) {
            assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

            val newPaymentSelection = PaymentMethodFixtures.GENERIC_PAYMENT_SELECTION
            selectionHolder.set(newPaymentSelection)
            rowSelectionHandler.handleImmediateRowSelectionCallback()

            paymentOptionDisplayDataHolder.paymentOption.test {
                assertThat(awaitItem()?.paymentMethodType).isEqualTo(PaymentMethod.Type.PayPal.code)
                callbackOrder.add("paymentOptionChanged")
                advanceUntilIdle()
                assertThat(callbackOrder).containsExactly("paymentOptionChanged", "callback")
            }
        }
    }

    @Test
    fun `reselection doesn't trigger paymentOption change, does invoke callback`() = runTest {
        var callbackInvoked = false
        val paymentOptionEmissions = mutableListOf<String?>()
        testScenario(
            coroutineScope = coroutineScope,
            callback = { { callbackInvoked = true } },
            setupSavedStateHandle = {
                set(EmbeddedSelectionHolder.EMBEDDED_SELECTION_KEY, PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
            }
        ) {
            assertThat(selectionHolder.selection.value).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

            paymentOptionDisplayDataHolder.paymentOption.test {
                val first = awaitItem()?.paymentMethodType
                paymentOptionEmissions.add(first)

                selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
                rowSelectionHandler.handleImmediateRowSelectionCallback()
                expectNoEvents()
                assertThat(callbackInvoked).isTrue()
                assertThat(paymentOptionEmissions).containsExactly(PaymentMethod.Type.Card.code)
            }
        }
    }

    private class Scenario(
        val selectionHolder: EmbeddedSelectionHolder,
        val paymentOptionDisplayDataHolder: DefaultPaymentOptionDisplayDataHolder,
        val rowSelectionHandler: EmbeddedRowSelectionImmediateActionHandler,
    )

    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    private fun testScenario(
        coroutineScope: CoroutineScope,
        setupSavedStateHandle: SavedStateHandle.() -> Unit = {},
        callback: Provider<InternalRowSelectionCallback?>,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val savedStateHandle = SavedStateHandle()
        setupSavedStateHandle(savedStateHandle)
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)

        val paymentOptionDisplayDataFactory = PaymentOptionDisplayDataFactory(
            iconLoader = mock(),
            context = ApplicationProvider.getApplicationContext(),
        )
        val confirmationStateSupplier = { EmbeddedConfirmationStateFixtures.defaultState() }
        val paymentOptionDisplayDataHolder = DefaultPaymentOptionDisplayDataHolder(
            coroutineScope = coroutineScope,
            selectionHolder = selectionHolder,
            confirmationStateSupplier = confirmationStateSupplier,
            paymentOptionDisplayDataFactory = paymentOptionDisplayDataFactory,
        )

        val rowSelectionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
            coroutineScope = coroutineScope,
            internalRowSelectionCallback = callback
        )
        Scenario(
            selectionHolder = selectionHolder,
            paymentOptionDisplayDataHolder = paymentOptionDisplayDataHolder,
            rowSelectionHandler = rowSelectionHandler,
        ).block()
    }
}
