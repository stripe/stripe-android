package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Turbine
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
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.SelectionBehavior
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import javax.inject.Provider

internal class EmbeddedManageScreenInteractorFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `manage launch uses injected coordinated selection behavior`() = runTest {
        val selectionCalls = Turbine<DisplayableSavedPaymentMethod>()
        runScenario(
            launchMode = EmbeddedLaunchMode.Manage,
            selectionBehavior = SelectionBehavior.Coordinated { paymentMethod ->
                selectionCalls.add(paymentMethod)
                Result.success(Unit)
            },
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(selectionCalls.awaitItem()).isEqualTo(paymentMethod)
            assertThat(interactor.state.value.isProcessing).isTrue()
            assertThat(interactor.state.value.paymentMethods.single().isSelectionPending).isTrue()
            assertThat(selectionHolder.selection.value).isNull()
            verifyNoInteractions(navigator)
        }
        selectionCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `payment options launch uses injected immediate behavior and navigates back`() = runTest {
        val selectionCalls = Turbine<DisplayableSavedPaymentMethod>()
        runScenario(
            launchMode = EmbeddedLaunchMode.PaymentOptions,
            selectionBehavior = SelectionBehavior.Immediate(onSelectPaymentMethod = selectionCalls::add),
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(selectionCalls.awaitItem()).isEqualTo(paymentMethod)
            assertThat(interactor.state.value.isProcessing).isFalse()
            verify(navigator).performAction(EmbeddedNavigator.Action.Back)
        }
        selectionCalls.ensureAllEventsConsumed()
    }

    @Test
    fun `form launch uses injected immediate behavior and closes`() = runTest {
        val selectionCalls = Turbine<DisplayableSavedPaymentMethod>()
        runScenario(
            launchMode = EmbeddedLaunchMode.Form(selectedPaymentMethodCode = "card"),
            selectionBehavior = SelectionBehavior.Immediate(onSelectPaymentMethod = selectionCalls::add),
        ) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(selectionCalls.awaitItem()).isEqualTo(paymentMethod)
            assertThat(interactor.state.value.isProcessing).isFalse()
            verify(navigator).performAction(EmbeddedNavigator.Action.Close(true))
        }
        selectionCalls.ensureAllEventsConsumed()
    }

    private suspend fun runScenario(
        launchMode: EmbeddedLaunchMode,
        selectionBehavior: SelectionBehavior,
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
        val interactor = DefaultEmbeddedManageScreenInteractorFactory(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            embeddedNavigatorProvider = Provider { navigator },
            launchMode = launchMode,
            selectionBehavior = selectionBehavior,
        ).createManageScreenInteractor()

        Scenario(
            interactor = interactor,
            paymentMethod = interactor.state.value.paymentMethods.single(),
            selectionHolder = selectionHolder,
            navigator = navigator,
        ).block()

        interactor.close()
        customerStateHolder.validate()
    }

    private data class Scenario(
        val interactor: ManageScreenInteractor,
        val paymentMethod: DisplayableSavedPaymentMethod,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val navigator: EmbeddedNavigator,
    )
}
