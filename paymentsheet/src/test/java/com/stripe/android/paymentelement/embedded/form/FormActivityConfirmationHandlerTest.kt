package com.stripe.android.paymentelement.embedded.form

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.testing.CoroutineTestRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

internal class FormActivityConfirmationHandlerTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

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
    fun `calls onClickOverride if provided`() = testScenario {
        var didCall = false
        onClickOverrideDelegate.set { didCall = true }
        formConfirmationHelper.confirm()
        confirmationHandler.startTurbine.expectNoEvents()
        assertThat(didCall).isTrue()

        onClickOverrideDelegate.clear()
        selectionHolder.set(PaymentMethodFixtures.CARD_PAYMENT_SELECTION)
        formConfirmationHelper.confirm()
        assertThat(confirmationHandler.startTurbine.awaitItem()).isNotNull()
    }

    private fun testScenario(
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val confirmationHandler = FakeConfirmationHandler()
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val embeddedState = EmbeddedConfirmationStateFixtures.defaultState()
        val stateHelper = FakeFormActivityStateHelper()
        val onClickOverrideDelegate = OnClickDelegateOverrideImpl()
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create()

        val customerStateHolder = FakeCustomerStateHolder()
        val confirmationHelper = DefaultFormActivityConfirmationHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            confirmationHandler = confirmationHandler,
            configuration = embeddedState.configuration,
            selectionHolder = selectionHolder,
            stateHelper = stateHelper,
            onClickDelegate = onClickOverrideDelegate,
            eventReporter = FakeEventReporter(),
            customerStateHolder = customerStateHolder,
            coroutineScope = backgroundScope,
        )

        Scenario(
            formConfirmationHelper = confirmationHelper,
            confirmationHandler = confirmationHandler,
            selectionHolder = selectionHolder,
            stateHelper = stateHelper,
            onClickOverrideDelegate = onClickOverrideDelegate
        ).block()

        stateHelper.validate()
        confirmationHandler.validate()
        customerStateHolder.validate()
    }
    private class Scenario(
        val formConfirmationHelper: FormActivityConfirmationHelper,
        val confirmationHandler: FakeConfirmationHandler,
        val selectionHolder: EmbeddedSelectionHolder,
        val stateHelper: FakeFormActivityStateHelper,
        val onClickOverrideDelegate: OnClickOverrideDelegate
    )
}
