
package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.DefaultSheetActivityConfirmationHelper
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class DefaultSheetActivityConfirmationHelperTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `confirm invokes onClickOverride when set`() = testScenario {
        val onClickTurbine = Turbine<Unit>()
        onClickDelegate.set {
            onClickTurbine.add(Unit)
        }

        confirmationHelper.confirm()

        assertThat(onClickTurbine.awaitItem()).isNotNull()
        onClickTurbine.ensureAllEventsConsumed()
        confirmationHandler.startTurbine.expectNoEvents()
    }

    @Test
    fun `confirm does not invoke onClickOverride after clearing`() = testScenario {
        onClickDelegate.set { }
        onClickDelegate.clear()

        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        confirmationHelper.confirm()

        assertThat(confirmationHandler.startTurbine.awaitItem()).isNotNull()
        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `confirm starts confirmation with correct option when selection is not null`() = testScenario {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        confirmationHelper.confirm()

        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()
        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    @Test
    fun `confirm does not start confirmation or report event when selection is null`() = testScenario {
        confirmationHelper.confirm()

        confirmationHandler.startTurbine.ensureAllEventsConsumed()
        eventReporter.pressConfirmButtonCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `when formSheetAction=continue confirm returns result`() = testScenario(
        configurationModifier = { formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue) }
    ) {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        confirmationHelper.confirm()

        assertThat(stateHelper.resultTurbine.awaitItem()).isEqualTo(
            FormResult.Complete(
                selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                hasBeenConfirmed = false,
                customerState = null,
            )
        )

        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
    }

    private fun testScenario(
        configurationModifier:
        EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = {
            this
        },
        customerStateHolder: FakeCustomerStateHolder = FakeCustomerStateHolder(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(savedStateHandle)
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .formSheetAction(EmbeddedPaymentElement.FormSheetAction.Confirm)
            .configurationModifier()
            .build()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val stateHelper = FakeFormActivityStateHelper()
        val onClickDelegate = OnClickDelegateOverrideImpl()
        val eventReporter = FakeEventReporter()
        val confirmationHelper = DefaultSheetActivityConfirmationHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            confirmationHandler = confirmationHandler,
            configuration = configuration,
            selectionHolder = selectionHolder,
            stateHelper = stateHelper,
            onClickDelegate = onClickDelegate,
            eventReporter = eventReporter,
            customerStateHolder = customerStateHolder,
            coroutineScope = backgroundScope,
        )

        Scenario(
            confirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            stateHelper = stateHelper,
            onClickDelegate = onClickDelegate,
            eventReporter = eventReporter,
            selectionHolder = selectionHolder,
        ).block()
        eventReporter.validate()
        confirmationHandler.validate()
        stateHelper.validate()
        customerStateHolder.validate()
    }

    private class Scenario(
        val confirmationHelper: DefaultSheetActivityConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val stateHelper: FakeFormActivityStateHelper,
        val onClickDelegate: OnClickDelegateOverrideImpl,
        val eventReporter: FakeEventReporter,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
