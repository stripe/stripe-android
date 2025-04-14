@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.mainthread.MainThreadSavedStateHandle
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
        assertThat(confirmationHelper.confirm()).isNull()
        assertThat(onClickTurbine.awaitItem()).isNotNull()
        onClickTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `confirm invokes eventReporter with the correct selection and starts confirmation`() = testScenario {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        assertThat(confirmationHelper.confirm()).isNull()

        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem())
            .isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        assertThat(confirmationHandler.startTurbine.awaitItem()).isNotNull()
    }

    @Test
    fun `confirm invokes eventReporter but does not start confirmation with null selection`() = testScenario {
        assertThat(confirmationHelper.confirm()).isNull()

        assertThat(eventReporter.pressConfirmButtonCalls.awaitItem()).isNull()
    }

    @Test
    fun `when formSheetAction=continue confirm returns result`() = testScenario(
        configurationModifier = { formSheetAction(EmbeddedPaymentElement.FormSheetAction.Continue) }
    ) {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)

        assertThat(confirmationHelper.confirm()).isEqualTo(
            FormResult.Complete(
                selection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
                hasBeenConfirmed = false,
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
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = EmbeddedSelectionHolder(MainThreadSavedStateHandle(savedStateHandle))
        val configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.")
            .configurationModifier()
            .build()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val stateHelper = FakeFormActivityStateHelper()
        val onClickDelegate = OnClickDelegateOverrideImpl()
        val eventReporter = FakeEventReporter()
        val confirmationHelper = DefaultFormActivityConfirmationHelper(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(5050, "USD")
                )
            ),
            paymentMethodMetadata = paymentMethodMetadata,
            confirmationHandler = confirmationHandler,
            configuration = configuration,
            selectionHolder = selectionHolder,
            stateHelper = stateHelper,
            onClickDelegate = onClickDelegate,
            eventReporter = eventReporter,
            lifecycleOwner = TestLifecycleOwner(coroutineDispatcher = Dispatchers.Unconfined),
            activityResultCaller = mock(),
        )
        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()
        assertThat(stateHelper.updateTurbine.awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)
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
