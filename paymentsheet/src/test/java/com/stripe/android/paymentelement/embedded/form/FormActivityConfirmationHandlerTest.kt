package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalEmbeddedPaymentElementApi::class)
internal class FormActivityConfirmationHandlerTest {

    @Test
    fun `confirm does not start confirmation if selection is null`() = testScenario {
        formConfirmationHelper.confirm()
        confirmationHandler.startTurbine.ensureAllEventsConsumed()
    }

    @Test
    fun `confirm starts confirmation if selection is not null`() = testScenario {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        formConfirmationHelper.confirm()
        val args = confirmationHandler.startTurbine.awaitItem()
        assertThat(args.confirmationOption).isInstanceOf<PaymentMethodConfirmationOption.New>()
    }

    @Test
    fun `uiStateHolder is updated on confirmationHandler state change`() = testScenario {
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        uiStateHolder.state.test {
            assertThat(awaitItem()).isNotNull()

            formConfirmationHelper.confirm()
            val args = confirmationHandler.startTurbine.awaitItem()
            confirmationHandler.state.value = ConfirmationHandler.State.Confirming(args.confirmationOption)

            assertThat(awaitItem()).isNotNull()

            confirmationHandler.state.value = ConfirmationHandler.State.Complete(
                ConfirmationHandler.Result.Succeeded(
                    intent = PaymentIntentFixtures.PI_SUCCEEDED,
                    deferredIntentConfirmationType = DeferredIntentConfirmationType.Client
                )
            )

            assertThat(awaitItem()).isNotNull()
            expectNoEvents()
        }
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val embeddedState = EmbeddedConfirmationStateFixtures.defaultState()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()
        val stateHolder = FormActivityUiStateHolder(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = embeddedState.configuration
        )

        val confirmationHelper = DefaultFormActivityConfirmationHelper(
            initializationMode = embeddedState.initializationMode,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            confirmationHandler = confirmationHandler,
            configuration = embeddedState.configuration,
            selectionHolder = selectionHolder,
            uiStateHolder = stateHolder,
            lifecycleOwner = TestLifecycleOwner(),
            activityResultCaller = mock()
        )

        assertThat(confirmationHandler.registerTurbine.awaitItem()).isNotNull()

        Scenario(
            formConfirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            selectionHolder = selectionHolder,
            uiStateHolder = stateHolder
        ).block()

        confirmationHandler.validate()
    }
    private class Scenario(
        val formConfirmationHelper: FormActivityConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val selectionHolder: EmbeddedSelectionHolder,
        val uiStateHolder: FormActivityUiStateHolder
    )
}
