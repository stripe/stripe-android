package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class EmbeddedManageScreenInteractorFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `manage launch selects through injected selector and closes`() = runTest {
        val selector = FakeEmbeddedSavedPaymentMethodSelector(Result.success(Unit))
        runScenario(
            launchMode = EmbeddedLaunchMode.Manage,
            selector = selector,
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(selector.selectCalls.awaitItem()).isEqualTo(selection)
            assertThat(interactor.state.value.isProcessing).isFalse()
            verify(eventReporter).onSelectPaymentOption(selection)
            assertThat(selectionHolder.selection.value).isNull()
            verify(navigator).performAction(EmbeddedNavigator.Action.Close(true))
        }
        selector.ensureAllEventsConsumed()
    }

    @Test
    fun `payment options launch uses selector and navigates back`() = runTest {
        val selector = FakeEmbeddedSavedPaymentMethodSelector(Result.success(Unit))
        runScenario(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            selector = selector,
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(selector.selectCalls.awaitItem()).isEqualTo(selection)
            assertThat(interactor.state.value.isProcessing).isFalse()
            verify(eventReporter).onSelectPaymentOption(selection)
            verify(navigator).performAction(EmbeddedNavigator.Action.Back)
        }
        selector.ensureAllEventsConsumed()
    }

    @Test
    fun `form launch uses selector and closes`() = runTest {
        val selector = FakeEmbeddedSavedPaymentMethodSelector(Result.success(Unit))
        runScenario(
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
            selector = selector,
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(selector.selectCalls.awaitItem()).isEqualTo(selection)
            assertThat(interactor.state.value.isProcessing).isFalse()
            verify(eventReporter).onSelectPaymentOption(selection)
            verify(navigator).performAction(EmbeddedNavigator.Action.Close(true))
        }
        selector.ensureAllEventsConsumed()
    }

    private suspend fun runScenario(
        launchMode: EmbeddedLaunchMode,
        selector: FakeEmbeddedSavedPaymentMethodSelector,
        block: suspend Scenario.() -> Unit,
    ) {
        val sourcePaymentMethod = PaymentMethodFixtures.createCard()
        val customerStateHolder = FakeCustomerStateHolder(paymentMethods = listOf(sourcePaymentMethod))
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val savedPaymentMethodMutator = mock<SavedPaymentMethodMutator>().also {
            whenever(it.editing).thenReturn(stateFlowOf(false))
            whenever(it.canEdit).thenReturn(stateFlowOf(true))
            whenever(it.defaultPaymentMethodId).thenReturn(stateFlowOf(null))
        }
        val navigator = mock<EmbeddedNavigator>()
        val eventReporter = mock<EventReporter>()
        val interactor = DefaultEmbeddedManageScreenInteractorFactory(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            embeddedNavigatorProvider = Provider { navigator },
            embeddedSavedPaymentMethodSelector = selector,
            eventReporter = eventReporter,
            launchMode = launchMode,
        ).createManageScreenInteractor()

        Scenario(
            interactor = interactor,
            paymentMethod = interactor.state.value.paymentMethods.single(),
            selectionHolder = selectionHolder,
            navigator = navigator,
            eventReporter = eventReporter,
            selection = PaymentSelection.Saved(sourcePaymentMethod),
        ).block()

        interactor.close()
        customerStateHolder.validate()
    }

    private data class Scenario(
        val interactor: ManageScreenInteractor,
        val paymentMethod: DisplayableSavedPaymentMethod,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val navigator: EmbeddedNavigator,
        val eventReporter: EventReporter,
        val selection: PaymentSelection.Saved,
    )
}
