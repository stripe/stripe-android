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
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
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
        val savedPaymentMethodSelector = FakeManageScreenSavedPaymentMethodSelector()
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
        val taxUpdate = CompletableDeferred<Result<CheckoutSessionResponse?>>()
        savedPaymentMethodSelector.onSyncBillingAfterEdit = { _, _ ->
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

        assertThat(savedPaymentMethodSelector.syncBillingAfterEditCalls.awaitItem()).isEqualTo(
            FakeManageScreenSavedPaymentMethodSelector.SyncBillingCall(PAYMENT_METHOD, UPDATED_PAYMENT_METHOD)
        )
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        navigatorResultCalls.expectNoEvents()

        savedPaymentMethodSelector.updateCheckoutSessionResponseCalls.expectNoEvents()
        val response = CheckoutSessionResponseFactory.create(id = "edit_response")
        taxUpdate.complete(Result.success(response))
        testScope.advanceUntilIdle()

        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Idle)
        assertThat(interactor.state.value.error).isNull()
        assertThat(interactor.state.value.isSaveButtonEnabled).isFalse()
        assertThat(savedPaymentMethodSelector.updateCheckoutSessionResponseCalls.awaitItem()).isEqualTo(response)
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `tax failure retains the updated card and retry repeats payment and tax updates`() = runScenario {
        val taxError = IllegalStateException("Tax update failed")
        savedPaymentMethodSelector.onSyncBillingAfterEdit = { _, _ -> Result.failure(taxError) }
        changeBillingPostalCode()

        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val firstUpdate = repository.updateRequests.awaitItem()
        assertThat(firstUpdate.params.billingDetails?.address?.postalCode).isEqualTo("10001")
        assertThat(customerStateHolder.paymentMethods.value.single()).isEqualTo(UPDATED_PAYMENT_METHOD)
        assertThat(selectionHolder.selection.value).isEqualTo(PaymentSelection.Saved(UPDATED_PAYMENT_METHOD))
        assertThat(savedPaymentMethodSelector.syncBillingAfterEditCalls.awaitItem()).isEqualTo(
            FakeManageScreenSavedPaymentMethodSelector.SyncBillingCall(PAYMENT_METHOD, UPDATED_PAYMENT_METHOD)
        )
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Idle)
        assertThat(interactor.state.value.error)
            .isEqualTo(updateCardBrandErrorMessage)
        assertThat(interactor.state.value.isSaveButtonEnabled).isTrue()
        navigatorResultCalls.expectNoEvents()

        savedPaymentMethodSelector.updateCheckoutSessionResponseCalls.expectNoEvents()
        val response = CheckoutSessionResponseFactory.create(id = "retry_response")
        savedPaymentMethodSelector.onSyncBillingAfterEdit = { _, _ -> Result.success(response) }
        interactor.handleViewAction(
            UpdatePaymentMethodInteractor.ViewAction.SaveButtonPressed
        )
        testScope.advanceUntilIdle()

        val retryUpdate = repository.updateRequests.awaitItem()
        assertThat(retryUpdate.paymentMethodId).isEqualTo(firstUpdate.paymentMethodId)
        assertThat(retryUpdate.params).isEqualTo(firstUpdate.params)
        assertThat(savedPaymentMethodSelector.syncBillingAfterEditCalls.awaitItem()).isEqualTo(
            FakeManageScreenSavedPaymentMethodSelector.SyncBillingCall(PAYMENT_METHOD, UPDATED_PAYMENT_METHOD)
        )
        assertThat(eventReporter.updatePaymentMethodSucceededCalls.awaitItem().selectedBrand).isNull()
        assertThat(interactor.state.value.status)
            .isEqualTo(UpdatePaymentMethodInteractor.Status.Idle)
        assertThat(interactor.state.value.error).isNull()
        assertThat(interactor.state.value.isSaveButtonEnabled).isFalse()
        assertThat(savedPaymentMethodSelector.updateCheckoutSessionResponseCalls.awaitItem()).isEqualTo(response)
        assertThat(navigatorResultCalls.awaitItem()).isNull()
        navigatorResultCalls.expectNoEvents()
    }

    @Test
    fun `saving an expiry change delegates billing synchronization with the returned card`() = runScenario(
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
        assertThat(savedPaymentMethodSelector.syncBillingAfterEditCalls.awaitItem()).isEqualTo(
            FakeManageScreenSavedPaymentMethodSelector.SyncBillingCall(PAYMENT_METHOD, PAYMENT_METHOD)
        )
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
        savedPaymentMethodSelector.syncBillingAfterEditCalls.expectNoEvents()
        assertThat(eventReporter.updatePaymentMethodFailedCalls.awaitItem().selectedBrand).isNull()
        assertThat(interactor.state.value.error)
            .isEqualTo(updateCardBrandErrorMessage)
        navigatorResultCalls.expectNoEvents()
    }

    private fun runScenario(
        updatePaymentMethodResult: Result<PaymentMethod> = Result.success(UPDATED_PAYMENT_METHOD),
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val selectionHolder = DefaultEmbeddedSelectionHolder(SavedStateHandle()).apply {
            setSelection(PaymentSelection.Saved(PAYMENT_METHOD))
        }
        val customerStateHolder = createCustomerStateHolder(selectionHolder)
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
        val savedPaymentMethodMutator = createSavedPaymentMethodMutator(
            eventReporter = eventReporter,
            selectionHolder = selectionHolder,
            customerStateHolder = customerStateHolder,
            repository = repository,
        )
        val savedPaymentMethodSelector = FakeManageScreenSavedPaymentMethodSelector()
        val interactor = DefaultEmbeddedUpdateScreenInteractorFactory(
            savedPaymentMethodMutatorProvider = Provider { savedPaymentMethodMutator },
            paymentMethodMetadata = PAYMENT_METHOD_METADATA,
            customerStateHolder = customerStateHolder,
            selectionHolder = selectionHolder,
            savedPaymentMethodSelector = savedPaymentMethodSelector,
            eventReporter = eventReporter,
            embeddedNavigatorProvider = Provider { navigator },
            autocompleteAddressInteractorFactory = TestAutocompleteAddressInteractor.noOpFactory(),
        ).createUpdateScreenInteractor(
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

    private fun createCustomerStateHolder(
        selectionHolder: DefaultEmbeddedSelectionHolder,
    ): DefaultCustomerStateHolder {
        return DefaultCustomerStateHolder(
            customerMetadata = stateFlowOf(PAYMENT_METHOD_METADATA.customerMetadata),
            paymentMethodMetadataFlow = stateFlowOf(PAYMENT_METHOD_METADATA),
            savedStateHandle = SavedStateHandle(),
            selection = selectionHolder.selection,
        ).apply {
            setCustomerState(
                PaymentSheetFixtures.EMPTY_CUSTOMER_STATE.copy(
                    paymentMethods = listOf(PAYMENT_METHOD),
                )
            )
        }
    }

    private fun TestScope.createSavedPaymentMethodMutator(
        eventReporter: FakeEventReporter,
        selectionHolder: DefaultEmbeddedSelectionHolder,
        customerStateHolder: DefaultCustomerStateHolder,
        repository: FakeSavedPaymentMethodRepository,
    ): SavedPaymentMethodMutator {
        return SavedPaymentMethodMutator(
            paymentMethodMetadataFlow = stateFlowOf(PAYMENT_METHOD_METADATA),
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
        val PAYMENT_METHOD_METADATA = PaymentMethodMetadataFactory.create(
            hasCustomerConfiguration = true,
            canUpdateCardExpiryAndBillingDetails = true,
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            ),
        )
        val PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val UPDATED_PAYMENT_METHOD = PAYMENT_METHOD.copy(
            billingDetails = PAYMENT_METHOD.billingDetails?.toBuilder()
                ?.setAddress(PAYMENT_METHOD.billingDetails?.address?.copy(postalCode = "10001"))
                ?.build(),
        )
    }
}

internal class FakeManageScreenSavedPaymentMethodSelector : ManageScreenSavedPaymentMethodSelector {
    private val _selectCalls = Turbine<SelectCall>()
    val selectCalls: ReceiveTurbine<SelectCall> = _selectCalls

    private val _syncBillingAfterEditCalls = Turbine<SyncBillingCall>()
    val syncBillingAfterEditCalls: ReceiveTurbine<SyncBillingCall> = _syncBillingAfterEditCalls

    private val _updateCheckoutSessionResponseCalls = Turbine<CheckoutSessionResponse>()
    val updateCheckoutSessionResponseCalls: ReceiveTurbine<CheckoutSessionResponse> =
        _updateCheckoutSessionResponseCalls

    private val _clearErrorCalls = Turbine<Unit>()

    var onSelect: suspend (PaymentSelection.Saved) -> Result<Unit> = { Result.success(Unit) }
    var onSyncBillingAfterEdit: suspend (PaymentMethod, PaymentMethod) -> Result<CheckoutSessionResponse?> = { _, _ ->
        Result.success(null)
    }

    override val selectionState: StateFlow<SavedPaymentMethodSelectionState> =
        stateFlowOf(SavedPaymentMethodSelectionState.Idle)
    override val checkoutSessionResponse: CheckoutSessionResponse? = null

    override suspend fun select(selection: PaymentSelection.Saved): Result<Unit> {
        _selectCalls.add(SelectCall(selection))
        return onSelect(selection)
    }

    override suspend fun syncBillingAfterEdit(
        original: PaymentMethod,
        updated: PaymentMethod,
    ): Result<CheckoutSessionResponse?> {
        _syncBillingAfterEditCalls.add(SyncBillingCall(original, updated))
        return onSyncBillingAfterEdit(original, updated)
    }

    override fun updateCheckoutSessionResponse(response: CheckoutSessionResponse) {
        _updateCheckoutSessionResponseCalls.add(response)
    }

    override fun clearError() {
        _clearErrorCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        _selectCalls.ensureAllEventsConsumed()
        _syncBillingAfterEditCalls.ensureAllEventsConsumed()
        _updateCheckoutSessionResponseCalls.ensureAllEventsConsumed()
        _clearErrorCalls.ensureAllEventsConsumed()
    }

    data class SelectCall(val selection: PaymentSelection.Saved)
    data class SyncBillingCall(val original: PaymentMethod, val updated: PaymentMethod)
}
