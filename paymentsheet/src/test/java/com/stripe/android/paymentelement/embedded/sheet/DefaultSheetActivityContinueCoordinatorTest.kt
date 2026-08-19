package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

internal class DefaultSheetActivityContinueCoordinatorTest {

    @Test
    fun `without tax region updater returns complete synchronously`() = runScenario(
        taxRegionResult = null,
    ) {
        continueCoordinator.onContinue()

        assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
            completeResult(checkoutSessionResponse = null)
        )
        stateHolder.updateProcessingTurbine.expectNoEvents()
    }

    @Test
    fun `successful tax region update returns updated response`() = runScenario(
        taxRegionResult = Result.success(UPDATED_RESPONSE),
    ) {
        continueCoordinator.onContinue()
        selectionHolder.setSelection(PaymentMethodFixtures.CASHAPP_PAYMENT_SELECTION)
        testScope.runCurrent()

        assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
        assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
            completeResult(
                selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                checkoutSessionResponse = UPDATED_RESPONSE,
            )
        )
        verify(requireNotNull(taxRegionUpdater)).update(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `successful tax region update without response returns complete`() = runScenario(
        taxRegionResult = Result.success(null),
    ) {
        continueCoordinator.onContinue()
        testScope.runCurrent()

        assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
        assertThat(stateHolder.resultTurbine.awaitItem()).isEqualTo(
            completeResult(checkoutSessionResponse = null)
        )
    }

    @Test
    fun `failed tax region update stops processing and shows error`() = runScenario(
        taxRegionResult = Result.failure(ERROR),
    ) {
        continueCoordinator.onContinue()
        testScope.runCurrent()

        assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isTrue()
        assertThat(stateHolder.updateProcessingTurbine.awaitItem()).isFalse()
        assertThat(stateHolder.updateErrorTurbine.awaitItem()).isEqualTo(ERROR.stripeErrorMessage())
        stateHolder.resultTurbine.expectNoEvents()
    }

    private fun runScenario(
        taxRegionResult: Result<CheckoutSessionResponse?>?,
        selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(selection)
        }
        val customerStateHolder = FakeCustomerStateHolder()
        val stateHolder = FakeSheetActivityStateHolder()
        val taxRegionUpdater = taxRegionResult?.let { result ->
            mock<SheetTaxRegionUpdater>().also { updater ->
                whenever(updater.update(selection)).thenReturn(result)
            }
        }
        val continueCoordinator = DefaultSheetActivityContinueCoordinator(
            taxRegionUpdater = taxRegionUpdater,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            launchMode = LAUNCH_MODE,
            coroutineScope = this,
        )

        Scenario(
            continueCoordinator = continueCoordinator,
            taxRegionUpdater = taxRegionUpdater,
            stateHolder = stateHolder,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            testScope = this,
        ).block()

        stateHolder.validate()
        customerStateHolder.validate()
    }

    private data class Scenario(
        val continueCoordinator: DefaultSheetActivityContinueCoordinator,
        val taxRegionUpdater: SheetTaxRegionUpdater?,
        val stateHolder: FakeSheetActivityStateHolder,
        val selectionHolder: EmbeddedSelectionHolder,
        val customerStateHolder: FakeCustomerStateHolder,
        val testScope: TestScope,
    ) {
        fun completeResult(
            selection: PaymentSelection? = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
            checkoutSessionResponse: CheckoutSessionResponse?,
        ): EmbeddedActivityResult.Complete {
            return EmbeddedActivityResult.Complete(
                selection = selection,
                previousNewSelections = selectionHolder.previousNewSelections,
                hasBeenConfirmed = false,
                customerState = customerStateHolder.customer.value,
                checkoutSessionResponse = checkoutSessionResponse,
                shouldInvokeSelectionCallback = false,
                launchMode = LAUNCH_MODE,
            )
        }
    }

    private companion object {
        val UPDATED_RESPONSE = CheckoutSessionResponseFactory.create()
        val ERROR = IllegalStateException("Tax region update failed")
        val LAUNCH_MODE = EmbeddedLaunchMode.PaymentOptions
    }
}
