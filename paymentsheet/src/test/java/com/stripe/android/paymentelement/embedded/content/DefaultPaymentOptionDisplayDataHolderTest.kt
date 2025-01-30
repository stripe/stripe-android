@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class DefaultPaymentOptionDisplayDataHolderTest {

    @Test
    fun `null confirmationState emits null paymentOption`() = testScenario {
        paymentOptionDisplayDataHolder.paymentOption.test {
            assertThat(awaitItem()).isNull()
            selectionHolder.set(PaymentSelection.GooglePay)
        }
    }

    @Test
    fun `valid confirmationState emits valid paymentOption`() = testScenario(
        confirmationStateSupplier = { EmbeddedConfirmationStateFixtures.defaultState() }
    ) {
        paymentOptionDisplayDataHolder.paymentOption.test {
            assertThat(awaitItem()).isNull()
            selectionHolder.set(PaymentSelection.GooglePay)
            assertThat(awaitItem()?.paymentMethodType).isEqualTo("google_pay")
        }
    }

    private fun testScenario(
        confirmationStateSupplier: () -> EmbeddedConfirmationStateHolder.State? = { null },
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentOptionDisplayDataFactory = PaymentOptionDisplayDataFactory(
            iconLoader = mock(),
            context = ApplicationProvider.getApplicationContext(),
        )
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle = SavedStateHandle())
        Scenario(
            paymentOptionDisplayDataHolder = DefaultPaymentOptionDisplayDataHolder(
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                selectionHolder = selectionHolder,
                confirmationStateSupplier = confirmationStateSupplier,
                paymentOptionDisplayDataFactory = paymentOptionDisplayDataFactory,
            ),
            selectionHolder = selectionHolder,
        ).block()
    }

    private class Scenario(
        val paymentOptionDisplayDataHolder: PaymentOptionDisplayDataHolder,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
