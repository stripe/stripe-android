
package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.FakeTapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddHelper
import com.stripe.android.common.taptoadd.TapToAddNextStep
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class DefaultFormActivityConfirmationHelperTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `constructor wires up confirmationHandler to stateHelper`() = testScenario {
        val exception = IllegalStateException("Test failure.")
        confirmationHandler.state.value = ConfirmationHandler.State.Complete(
            ConfirmationHandler.Result.Failed(
                cause = exception,
                message = "Error".resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            )
        )

        assertThat(stateHelper.updateTurbine.awaitItem()).isInstanceOf<ConfirmationHandler.State.Complete>()
    }

    @Test
    fun `confirm invokes onClickOverride when set`() = testScenario {
        val onClickTurbine = Turbine<Unit>()
        onClickDelegate.set {
            onClickTurbine.add(Unit)
        }

        confirmationHelper.confirm()

        assertThat(onClickTurbine.awaitItem()).isNotNull()
        onClickTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `confirm invokes eventReporter with the correct selection and starts confirmation`() = testScenario {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        confirmationHelper.confirm()

        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(confirmationHandler.startTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `confirm does not invoke eventReporter when selection is null`() = testScenario {
        confirmationHelper.confirm()

        // Should not report button press when there's no selection
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

    @Test
    fun `TapToAddResult Complete sets state helper result as expected`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            tapToAddHelper.emitNextStep(TapToAddNextStep.Complete)

            assertThat(stateHelper.resultTurbine.awaitItem()).isEqualTo(
                FormResult.Complete(
                    selection = null,
                    hasBeenConfirmed = true,
                    customerState = customerStateHolder.customer.value,
                )
            )
        }
    }

    @Test
    fun `TapToAddResult Continue sets state helper result as expected`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
            tapToAddHelper.emitNextStep(
                TapToAddNextStep.Continue(
                    paymentSelection = expectedSelection,
                )
            )

            assertThat(stateHelper.resultTurbine.awaitItem()).isEqualTo(
                FormResult.Complete(
                    selection = expectedSelection,
                    hasBeenConfirmed = false,
                    customerState = customerStateHolder.customer.value,
                )
            )
            assertThat(customerStateHolder.addPaymentMethodTurbine.awaitItem()).isEqualTo(
                expectedSelection.paymentMethod
            )
        }
    }

    @Test
    fun `TapToAddResult Canceled with payment selection sets state helper result as expected`() {
        val tapToAddHelper = FakeTapToAddHelper()
        val customerStateHolder = FakeCustomerStateHolder()
        val expectedSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        testScenario(
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
        ) {
            tapToAddHelper.emitNextStep(
                TapToAddNextStep.ConfirmSavedPaymentMethod(
                    paymentSelection = expectedSelection,
                )
            )

            assertThat(customerStateHolder.addPaymentMethodTurbine.awaitItem()).isEqualTo(
                expectedSelection.paymentMethod
            )
            assertThat(stateHelper.resultTurbine.awaitItem()).isEqualTo(
                FormResult.Complete(
                    selection = expectedSelection,
                    hasBeenConfirmed = false,
                    customerState = customerStateHolder.customer.value,
                )
            )
        }
    }

    private fun testScenario(
        configurationModifier:
        EmbeddedPaymentElement.Configuration.Builder.() -> EmbeddedPaymentElement.Configuration.Builder = {
            this
        },
        tapToAddHelper: TapToAddHelper = FakeTapToAddHelper.noOp(),
        customerStateHolder: FakeCustomerStateHolder = FakeCustomerStateHolder(),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val formActivityRegistrar = FakeFormActivityRegistrar()
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
        val testLifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined)
        val confirmationHelper = DefaultFormActivityConfirmationHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            confirmationHandler = confirmationHandler,
            configuration = configuration,
            selectionHolder = selectionHolder,
            stateHelper = stateHelper,
            onClickDelegate = onClickDelegate,
            eventReporter = eventReporter,
            tapToAddHelper = tapToAddHelper,
            customerStateHolder = customerStateHolder,
            lifecycleOwner = testLifecycleOwner,
            activityResultCaller = mock(),
            coroutineScope = this,
            formActivityRegistrar = formActivityRegistrar,
        )

        assertThat(formActivityRegistrar.registerAndBootstrapTurbine.awaitItem()).isNotNull()
        assertThat(stateHelper.updateTurbine.awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)
        // Bootstrap is no longer called during DefaultFormActivityConfirmationHelper initialization
        // It's now called in FormActivityViewModel.inject()
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
        val confirmationHelper: DefaultFormActivityConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val stateHelper: FakeFormActivityStateHelper,
        val onClickDelegate: OnClickDelegateOverrideImpl,
        val eventReporter: FakeEventReporter,
        val selectionHolder: EmbeddedSelectionHolder,
    )
}
