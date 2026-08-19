package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class DefaultSheetActivityContinueCoordinatorTest {

    @Test
    fun `onContinue returns current sheet state`() = runScenario {
        continueCoordinator.onContinue()

        assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
            EmbeddedActivityResult.Complete(
                selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                checkoutSessionResponse = null,
                shouldInvokeSelectionCallback = false,
                launchMode = LAUNCH_MODE,
            )
        )
    }

    private fun runScenario(
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val stateHolder = FakeSheetActivityStateHolder()
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        }
        val customerStateHolder = FakeCustomerStateHolder(
            customerState = PaymentSheetFixtures.EMPTY_CUSTOMER_STATE,
        )
        val continueCoordinator = DefaultSheetActivityContinueCoordinator(
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = LAUNCH_MODE,
        )

        Scenario(
            continueCoordinator = continueCoordinator,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
        ).block()

        stateHolder.validate()
        customerStateHolder.validate()
    }

    private data class Scenario(
        val continueCoordinator: SheetActivityContinueCoordinator,
        val stateHolder: FakeSheetActivityStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: FakeCustomerStateHolder,
    )

    private companion object {
        val LAUNCH_MODE = EmbeddedLaunchMode.Form(
            selectedPaymentMethodCode = "card",
        )
    }
}
