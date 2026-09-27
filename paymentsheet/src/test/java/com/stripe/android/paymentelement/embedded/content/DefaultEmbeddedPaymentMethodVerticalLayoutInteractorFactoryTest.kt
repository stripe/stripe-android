package com.stripe.android.paymentelement.embedded.content

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.checkout.CheckoutControllerStateFactory
import com.stripe.android.checkout.CheckoutControllerStateHolder
import com.stripe.android.checkout.injection.CheckoutControllerModule
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.FakeConfirmationHandler
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedRowSelectionImmediateActionHandler
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.verticalmode.ImmediateVerticalPaymentSelectionHandler
import com.stripe.android.testing.CleanupTestRule
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeIsNfcScanningAvailable
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.FakePaymentMethodMessagePromotionsHelper
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(CheckoutSessionPreview::class)
internal class DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactoryTest {
    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    // A new presenter, such as after activity recreation or process death, builds a new interactor
    // that re-applies the current selection.
    @Test
    fun `creating an interactor for the current selection keeps a saved selection failure`() {
        val selection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        val failure = SavedPaymentMethodSelectionState.Failed("Selection failed".resolvableString)
        runScenario(
            selection = selection,
            savedPaymentMethodSelectionState = failure,
        ) {
            factory.createInteractor()

            assertThat(selectionHolder.selection.value).isEqualTo(selection)
            assertThat(selectionHolder.state?.savedPaymentMethodSelectionState).isEqualTo(failure)
        }
    }

    @Test
    fun `creating an interactor acknowledges a preselected SEPA mandate`() = runScenario(
        selection = PaymentSelection.Saved(PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD),
        savedPaymentMethodSelectionState = SavedPaymentMethodSelectionState.Idle,
    ) {
        assertThat(selectionHolder.selection.value?.hasAcknowledgedSepaMandate).isFalse()

        factory.createInteractor()

        assertThat(selectionHolder.selection.value?.hasAcknowledgedSepaMandate).isTrue()
    }

    private fun DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory.createInteractor() = create(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        configuration = EmbeddedPaymentElement.Configuration.Builder("Example, Inc.").build(),
        walletsState = stateFlowOf(null),
        isImmediateAction = false,
        embeddedViewDisplaysMandateText = true,
    )

    private class Scenario(
        val factory: DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory,
        val selectionHolder: CheckoutControllerStateHolder,
    )

    @OptIn(ExperimentalAnalyticEventCallbackApi::class)
    private fun runScenario(
        selection: PaymentSelection.Saved,
        savedPaymentMethodSelectionState: SavedPaymentMethodSelectionState,
        block: Scenario.() -> Unit,
    ) {
        val savedStateHandle = SavedStateHandle()
        val selectionHolder = CheckoutControllerStateFactory.createStateHolder(savedStateHandle)
        selectionHolder.state = CheckoutControllerStateFactory.create(
            paymentSelection = selection,
            savedPaymentMethodSelectionState = savedPaymentMethodSelectionState,
        )
        val eventReporter = FakeEventReporter()
        val viewModelScope = coroutineScopeCleanupRule.track(CoroutineScope(Dispatchers.Unconfined))
        val customerStateHolder = DefaultCustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selectionHolder.selection,
            customerMetadata = stateFlowOf(PaymentMethodMetadataFixtures.DEFAULT_CUSTOMER_METADATA),
            paymentMethodMetadataFlow = stateFlowOf(null),
        )
        customerStateHolder.setCustomerState(
            PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                paymentMethods = listOf(selection.paymentMethod),
            )
        )
        val linkAccountHolder = LinkAccountHolder(SavedStateHandle())
        val sheetStateHolder = SheetStateHolder(savedStateHandle)
        val immediateActionHandler = DefaultEmbeddedRowSelectionImmediateActionHandler(
            coroutineScope = viewModelScope,
            internalRowSelectionCallback = { null },
        )
        val factory = DefaultEmbeddedPaymentMethodVerticalLayoutInteractorFactory(
            eventReporter = eventReporter,
            embeddedFormHelperFactory = EmbeddedFormHelperFactory(
                linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
                cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
                embeddedSelectionHolder = selectionHolder,
                savedStateHandle = savedStateHandle,
                isNfcScanningAvailable = FakeIsNfcScanningAvailable(result = false),
            ),
            confirmationHandler = FakeConfirmationHandler(),
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            paymentMethodMessagePromotionsHelper = FakePaymentMethodMessagePromotionsHelper(),
            verticalPaymentSelectionHandler = ImmediateVerticalPaymentSelectionHandler(
                updateSelection = { updatedSelection, _ -> selectionHolder.setSelection(updatedSelection) },
                completionAction = immediateActionHandler::invoke,
            ),
            coroutineScope = viewModelScope,
            sheetStateHolder = sheetStateHolder,
            savedPaymentMethodMutatorFactory = EmbeddedContentSavedPaymentMethodMutatorFactory(
                eventReporter = eventReporter,
                workContext = Dispatchers.Unconfined,
                uiContext = Dispatchers.Unconfined,
                savedPaymentMethodRepository = FakeSavedPaymentMethodRepository(),
                selectionHolder = selectionHolder,
                customerStateHolder = customerStateHolder,
                linkAccountHolder = linkAccountHolder,
                coroutineScope = viewModelScope,
                sheetStateHolder = sheetStateHolder,
            ),
            linkAccountHolder = linkAccountHolder,
            hostProcessing = stateFlowOf(false),
            savedPaymentMethodSelectionState = CheckoutControllerModule.provideSavedPaymentMethodSelectionState(
                stateHolder = selectionHolder,
            ),
        )

        Scenario(
            factory = factory,
            selectionHolder = selectionHolder,
        ).block()
    }
}
