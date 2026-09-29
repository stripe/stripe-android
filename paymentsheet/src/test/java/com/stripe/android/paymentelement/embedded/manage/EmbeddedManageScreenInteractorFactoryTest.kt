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
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun `interactor projects injected selection state and selects through injected selector`() = runTest {
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
        val selectCalls = Turbine<PaymentSelection.Saved>()
        val selectionState = MutableStateFlow<SavedPaymentMethodSelectionState>(SavedPaymentMethodSelectionState.Idle)
        val interactor = DefaultEmbeddedManageScreenInteractorFactory(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            embeddedNavigatorProvider = Provider { navigator },
            embeddedSavedPaymentMethodSelector = { selection ->
                selectCalls.add(selection)
                Result.success(Unit)
            },
            savedPaymentMethodSelectionState = selectionState,
            eventReporter = eventReporter,
            launchMode = EmbeddedLaunchMode.Manage,
        ).createManageScreenInteractor()
        val selection = PaymentSelection.Saved(sourcePaymentMethod)

        selectionState.value = SavedPaymentMethodSelectionState.Pending(sourcePaymentMethod.id)
        assertThat(interactor.state.value.paymentMethods.single().isSelectionPending).isTrue()
        selectionState.value = SavedPaymentMethodSelectionState.Idle

        val paymentMethod = interactor.state.value.paymentMethods.single()
        interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

        assertThat(selectCalls.awaitItem()).isEqualTo(selection)
        verify(eventReporter).onSelectPaymentOption(selection)
        // The injected selector owns committing the selection.
        assertThat(selectionHolder.selection.value).isNull()
        verify(navigator).performAction(EmbeddedNavigator.Action.Close(true))

        interactor.close()
        selectCalls.ensureAllEventsConsumed()
        customerStateHolder.validate()
    }
}
