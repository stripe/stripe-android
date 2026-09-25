package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentelement.embedded.sheet.FakeSheetActivityStateHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.verticalmode.ManageScreenInteractor
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
    fun `manage launch coordinates selection without navigating`() = runTest {
        runScenario(launchMode = EmbeddedLaunchMode.Manage) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            assertThat(sheetActivityStateHolder.selectSavedPaymentMethodTurbine.awaitItem())
                .isEqualTo(selection)
            assertThat(selectionHolder.selection.value).isNull()
            verifyNoInteractions(navigator)
        }
    }

    @Test
    fun `payment options launch selects immediately and navigates back`() = runTest {
        runScenario(launchMode = EmbeddedLaunchMode.PaymentOptions) {
            interactor.handleViewAction(ManageScreenInteractor.ViewAction.SelectPaymentMethod(paymentMethod))

            sheetActivityStateHolder.selectSavedPaymentMethodTurbine.expectNoEvents()
            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            verify(navigator).performAction(EmbeddedNavigator.Action.Back)
        }
    }

    private suspend fun runScenario(
        launchMode: EmbeddedLaunchMode,
        block: suspend Scenario.() -> Unit,
    ) {
        val paymentMethod = PaymentMethodFixtures.createCard()
        val customerStateHolder = FakeCustomerStateHolder(paymentMethods = listOf(paymentMethod))
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val savedPaymentMethodMutator = mock<SavedPaymentMethodMutator>().also {
            whenever(it.editing).thenReturn(stateFlowOf(false))
            whenever(it.canEdit).thenReturn(stateFlowOf(true))
            whenever(it.defaultPaymentMethodId).thenReturn(stateFlowOf(null))
        }
        val eventReporter = FakeEventReporter()
        val navigator = mock<EmbeddedNavigator>()
        val sheetActivityStateHolder = FakeSheetActivityStateHolder()
        val interactor = DefaultEmbeddedManageScreenInteractorFactory(
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodMutator = savedPaymentMethodMutator,
            linkAccountHolder = LinkAccountHolder(SavedStateHandle()),
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { navigator },
            launchMode = launchMode,
            sheetActivityStateHolder = sheetActivityStateHolder,
        ).createManageScreenInteractor()
        val displayablePaymentMethod = interactor.state.value.paymentMethods.single()
        val selection = PaymentSelection.Saved(paymentMethod)

        Scenario(
            interactor = interactor,
            paymentMethod = displayablePaymentMethod,
            selection = selection,
            selectionHolder = selectionHolder,
            sheetActivityStateHolder = sheetActivityStateHolder,
            navigator = navigator,
        ).block()

        interactor.close()
        customerStateHolder.validate()
        eventReporter.validate()
        sheetActivityStateHolder.validate()
    }

    private data class Scenario(
        val interactor: ManageScreenInteractor,
        val paymentMethod: com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod,
        val selection: PaymentSelection.Saved,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val sheetActivityStateHolder: FakeSheetActivityStateHolder,
        val navigator: EmbeddedNavigator,
    )
}
