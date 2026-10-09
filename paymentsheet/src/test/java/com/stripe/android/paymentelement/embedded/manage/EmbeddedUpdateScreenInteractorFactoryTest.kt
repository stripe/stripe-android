package com.stripe.android.paymentelement.embedded.manage

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentelement.embedded.DefaultEmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.sheet.EmbeddedNavigator
import com.stripe.android.paymentsheet.DefaultCustomerStateHolder
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.addresselement.TestAutocompleteAddressInteractor
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.state.SavedPaymentMethodSelectionState
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor.Companion.updateCardBrandErrorMessage
import com.stripe.android.paymentsheet.ui.EditCardDetailsInteractor
import com.stripe.android.paymentsheet.ui.FakeUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.UpdatePaymentMethodInteractor
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.BillingAddressElement
import com.stripe.android.uicore.elements.AutocompleteAddressElement
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeSavedPaymentMethodRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import javax.inject.Provider

internal class EmbeddedUpdateScreenInteractorFactoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    @Test
    fun `saved card update form receives autocomplete factory`() = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            canUpdateCardExpiryAndBillingDetails = true,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle())
        val eventReporter = FakeEventReporter()
        val customerStateHolder = DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
        )
        val savedPaymentMethodSelector = FakeManageScreenSavedPaymentMethodSelector(
            setSelection = selectionHolder::setSelection,
        )
        val autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory()
        val factory = DefaultEmbeddedUpdateScreenInteractorFactory(
            savedPaymentMethodMutatorProvider = Provider { error("Not expected") },
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodSelector = savedPaymentMethodSelector,
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { error("Not expected") },
            autocompleteAddressInteractorFactory = autocompleteAddressInteractorFactory,
        )

        val interactor = factory.createUpdateScreenInteractor(
            PaymentMethodFixtures.displayableCard()
        ) as DefaultUpdatePaymentMethodInteractor
        val billingAddressElement = interactor.editCardDetailsInteractor.state.value.billingDetailsForm
            ?.addressSectionElement
            ?.fields
            ?.single() as BillingAddressElement

        assertThat(billingAddressElement.addressElement)
            .isInstanceOf(AutocompleteAddressElement::class.java)

        interactor.close()
        savedPaymentMethodSelector.ensureAllEventsConsumed()
        eventReporter.validate()
    }

    @Test
    fun `saving selected card with changed billing address waits for tax update`() = runScenario {
        val taxUpdate = CompletableDeferred<Result<Unit>>()
        savedPaymentMethodSelector.onSelect = {
            assertThat(customerStateHolder.paymentMethods.value.single()).isEqualTo(UPDATED_PAYMENT_METHOD)
            taxUpdate.await()
        }
        interactor.editCardDetailsInteractor.handleViewAction(
            EditCardDetailsInteractor.ViewAction.BillingDetailsChanged(
                PaymentSheetFixtures.billingDetailsFormState(
                    postalCode = FormFieldEntry("10001", isComplete = true),
                )
            )
        )
        testScope.advanceUntilIdle()

        assertThat(interactor.state.value.isSaveButtonEnabled).isTrue()
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val updateRequest = repository.updateRequests.awaitItem()
        assertThat(updateRequest.paymentMethodId).isEqualTo(PAYMENT_METHOD.id)
        val updateParams = updateRequest.params as PaymentMethodUpdateParams.Card
        assertThat(updateParams.billingDetails?.address?.postalCode).isEqualTo("10001")
        assertThat(customerStateHolder.paymentMethods.value.single()).isEqualTo(UPDATED_PAYMENT_METHOD)
        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Updating)
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()

        val selection = savedPaymentMethodSelector.selectCalls.awaitItem().selection
        assertThat(selection).isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        navigatorResultCalls.expectNoEvents()

        taxUpdate.complete(Result.success(Unit))
        testScope.advanceUntilIdle()

        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Idle)
        assertThat(interactor.state.value.error).isNull()
        assertThat(interactor.state.value.isSaveButtonEnabled).isFalse()
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `tax failure retains the updated card and retry repeats payment and tax updates`() = runScenario {
        val taxError = IllegalStateException("Tax update failed")
        savedPaymentMethodSelector.onSelect = { Result.failure(taxError) }
        changeBillingPostalCode()

        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val firstUpdate = repository.updateRequests.awaitItem()
        assertThat(firstUpdate.params.billingDetails?.address?.postalCode).isEqualTo("10001")
        assertThat(customerStateHolder.paymentMethods.value.single()).isEqualTo(UPDATED_PAYMENT_METHOD)
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        assertThat(savedPaymentMethodSelector.selectCalls.awaitItem().selection)
            .isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Idle)
        assertThat(interactor.state.value.error)
            .isEqualTo(updateCardBrandErrorMessage)
        assertThat(interactor.state.value.isSaveButtonEnabled).isTrue()
        navigatorResultCalls.expectNoEvents()

        savedPaymentMethodSelector.onSelect = { Result.success(Unit) }
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val retryUpdate = repository.updateRequests.awaitItem()
        assertThat(retryUpdate.paymentMethodId).isEqualTo(firstUpdate.paymentMethodId)
        assertThat(retryUpdate.params).isEqualTo(firstUpdate.params)
        assertThat(savedPaymentMethodSelector.selectCalls.awaitItem().selection)
            .isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Idle)
        assertThat(interactor.state.value.error).isNull()
        assertThat(interactor.state.value.isSaveButtonEnabled).isFalse()
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `saving an unselected card with changed billing address does not update tax`() = runScenario(
        selectedSelection = PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT),
    ) {
        changeBillingPostalCode()
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        assertThat(repository.updateRequests.awaitItem().params.billingDetails?.address?.postalCode)
            .isEqualTo("10001")
        assertThat(customerStateHolder.paymentMethods.value.single()).isEqualTo(UPDATED_PAYMENT_METHOD)
        assertThat(selectionHolder.selection.value)
            .isEqualTo(PaymentSelection.Saved(PaymentMethodFixtures.US_BANK_ACCOUNT))
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `saving a changed billing address with no selection does not update tax`() = runScenario(
        selectedSelection = null,
    ) {
        changeBillingPostalCode()
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        assertThat(repository.updateRequests.awaitItem().params.billingDetails?.address?.postalCode)
            .isEqualTo("10001")
        assertThat(selectionHolder.selection.value).isNull()
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `saving an expiry change with unchanged billing address does not update tax`() = runScenario(
        updatePaymentMethodResult = Result.success(PAYMENT_METHOD),
    ) {
        interactor.editCardDetailsInteractor.handleViewAction(
            EditCardDetailsInteractor.ViewAction.DateChanged("1230")
        )
        testScope.advanceUntilIdle()

        assertThat(interactor.state.value.isSaveButtonEnabled).isTrue()
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val updateRequest = repository.updateRequests.awaitItem()
        val updateParams = updateRequest.params as PaymentMethodUpdateParams.Card
        assertThat(updateParams.expiryMonth).isEqualTo(12)
        assertThat(updateParams.expiryYear).isEqualTo(2030)
        assertThat(updateParams.billingDetails).isNull()
        assertThat(customerStateHolder.paymentMethods.value.single().billingDetails?.address)
            .isEqualTo(PAYMENT_METHOD.billingDetails?.address)
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `failed payment method update does not attempt tax update`() = runScenario(
        updatePaymentMethodResult = Result.failure(IllegalStateException("Payment method update failed")),
    ) {
        changeBillingPostalCode()
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val updateRequest = repository.updateRequests.awaitItem()
        assertThat(updateRequest.params.billingDetails?.address?.postalCode).isEqualTo("10001")
        assertThat(customerStateHolder.paymentMethods.value.single()).isEqualTo(PAYMENT_METHOD)
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(PAYMENT_METHOD))
        savedPaymentMethodSelector.selectCalls.expectNoEvents()
        assertThat(eventReporter.updatePaymentMethodFailedCalls.awaitItem().selectedBrand).isNull()
        assertThat(interactor.state.value.error)
            .isEqualTo(updateCardBrandErrorMessage)
        navigatorResultCalls.expectNoEvents()
    }

    private fun runScenario(
        selectedSelection: PaymentSelection? = PaymentSelection.Saved(PAYMENT_METHOD),
        updatePaymentMethodResult: Result<PaymentMethod> = Result.success(UPDATED_PAYMENT_METHOD),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            canUpdateCardExpiryAndBillingDetails = true,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(selectedSelection)
        }
        val customerStateHolder = DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(paymentMethodMetadata.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
        ).apply {
            setCustomerState(
                PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                    paymentMethods = listOf(PAYMENT_METHOD),
                )
            )
        }
        val eventReporter = FakeEventReporter()
        val navigator = EmbeddedNavigator(
            coroutineScope = this,
            initialScreen = EmbeddedNavigator.Screen.ManageUpdate(FakeUpdatePaymentMethodInteractor()),
            eventReporter = eventReporter,
        )
        eventReporter.showEditablePaymentOptionCalls.awaitItem()
        val navigatorResultCalls = Turbine<Boolean?>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            navigator.result.collect { result ->
                eventReporter.hideEditablePaymentOptionCalls.awaitItem()
                navigatorResultCalls.add(result)
            }
        }
        val repository = FakeSavedPaymentMethodRepository(
            paymentMethods = listOf(PAYMENT_METHOD),
            onUpdatePaymentMethod = { updatePaymentMethodResult },
        )
        val savedPaymentMethodMutator = SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(paymentMethodMetadata),
            eventReporter = eventReporter,
            coroutineScope = backgroundScope,
            workContext = coroutineContext,
            uiContext = coroutineContext,
            savedPaymentMethodRepository = repository,
            selection = selectionHolder.selection,
            setSelection = selectionHolder::setSelection,
            customerStateHolder = customerStateHolder,
            prePaymentMethodRemoveActions = {},
            postPaymentMethodRemoveActions = {},
            onUpdatePaymentMethod = { _, _, _, _, _ -> error("Not expected") },
            isLinkEnabled = stateFlowOf(false),
            isNotPaymentFlow = false,
            linkAccount = stateFlowOf(null),
        )
        val savedPaymentMethodSelector = FakeManageScreenSavedPaymentMethodSelector(
            setSelection = selectionHolder::setSelection,
        )
        val factory = DefaultEmbeddedUpdateScreenInteractorFactory(
            savedPaymentMethodMutatorProvider = Provider { savedPaymentMethodMutator },
            paymentMethodMetadata = paymentMethodMetadata,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodSelector = savedPaymentMethodSelector,
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { navigator },
            autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory(),
        )
        val interactor = factory.createUpdateScreenInteractor(
            PAYMENT_METHOD.toDisplayableSavedPaymentMethod()
        ) as DefaultUpdatePaymentMethodInteractor

        Scenario(
            testScope = this,
            interactor = interactor,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            repository = repository,
            eventReporter = eventReporter,
            savedPaymentMethodSelector = savedPaymentMethodSelector,
            navigatorResultCalls = navigatorResultCalls,
        ).block()

        interactor.close()
        savedPaymentMethodSelector.ensureAllEventsConsumed()
        navigatorResultCalls.ensureAllEventsConsumed()
        repository.validate()
        eventReporter.validate()
    }

    private fun Scenario.changeBillingPostalCode() {
        interactor.editCardDetailsInteractor.handleViewAction(
            EditCardDetailsInteractor.ViewAction.BillingDetailsChanged(
                PaymentSheetFixtures.billingDetailsFormState(
                    postalCode = FormFieldEntry("10001", isComplete = true),
                )
            )
        )
        testScope.advanceUntilIdle()
    }

    private data class Scenario(
        val testScope: TestScope,
        val interactor: DefaultUpdatePaymentMethodInteractor,
        val selectionHolder: DefaultEmbeddedSelectionHolder,
        val customerStateHolder: DefaultCustomerStateHolder,
        val repository: FakeSavedPaymentMethodRepository,
        val eventReporter: FakeEventReporter,
        val savedPaymentMethodSelector: FakeManageScreenSavedPaymentMethodSelector,
        val navigatorResultCalls: ReceiveTurbine<Boolean?>,
    )

    private companion object {
        val PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val UPDATED_PAYMENT_METHOD = PAYMENT_METHOD.copy(
            billingDetails = PAYMENT_METHOD.billingDetails?.copy(
                address = PAYMENT_METHOD.billingDetails?.address?.copy(postalCode = "10001"),
            ),
        )
    }
}

internal class FakeManageScreenSavedPaymentMethodSelector(
    private val setSelection: (PaymentSelection?) -> Unit,
) : ManageScreenSavedPaymentMethodSelector {
    private val _selectCalls = Turbine<SelectCall>()
    val selectCalls: ReceiveTurbine<SelectCall> = _selectCalls

    private val _clearErrorCalls = Turbine<Unit>()

    var onSelect: suspend (PaymentSelection.Saved) -> Result<Unit> = { Result.success(Unit) }

    override val selectionState: StateFlow<SavedPaymentMethodSelectionState> =
        stateFlowOf(SavedPaymentMethodSelectionState.Idle)
    override val checkoutSessionResponse: CheckoutSessionResponse? = null

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        _selectCalls.add(SelectCall(selection))
        return onSelect(selection).onSuccess {
            setSelection(selection)
        }
    }

    override fun clearError() {
        _clearErrorCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        _selectCalls.ensureAllEventsConsumed()
        _clearErrorCalls.ensureAllEventsConsumed()
    }

    data class SelectCall(val selection: PaymentSelection.Saved)
}
