package com.stripe.android.paymentsheet

import androidx.lifecycle.viewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemovalDelayMillis
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.paymentsheet.viewmodels.PaymentOptionsItemsMapper
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class SavedPaymentMethodMutator(
    private val paymentMethodMetadataFlow: StateFlow<PaymentMethodMetadata?>,
    private val eventReporter: EventReporter,
    private val coroutineScope: CoroutineScope,
    private val workContext: CoroutineContext,
    private val customerRepository: CustomerRepository,
    private val selection: StateFlow<PaymentSelection?>,
    private val clearSelection: () -> Unit,
    private val customerStateHolder: CustomerStateHolder,
    private val onPaymentMethodRemoved: () -> Unit,
    private val onUpdatePaymentMethod: (
        DisplayableSavedPaymentMethod,
        canRemove: Boolean,
        performRemove: suspend () -> Throwable?,
        updateExecutor: suspend (brand: CardBrand) -> Result<PaymentMethod>,
    ) -> Unit,
    private val navigationPop: () -> Unit,
    isLinkEnabled: StateFlow<Boolean?>,
    isNotPaymentFlow: Boolean,
) {
    val defaultPaymentMethodId: StateFlow<String?> = customerStateHolder.customer.mapAsStateFlow { customerState ->
        customerState?.defaultPaymentMethodId
    }

    val providePaymentMethodName: (code: String?) -> ResolvableString = { code ->
        code?.let {
            paymentMethodMetadataFlow.value?.supportedPaymentMethodForCode(code)
        }?.displayName.orEmpty()
    }

    private val paymentOptionsItemsMapper: PaymentOptionsItemsMapper by lazy {
        PaymentOptionsItemsMapper(
            customerState = customerStateHolder.customer,
            isGooglePayReady = paymentMethodMetadataFlow.mapAsStateFlow { it?.isGooglePayReady == true },
            isLinkEnabled = isLinkEnabled,
            isNotPaymentFlow = isNotPaymentFlow,
            nameProvider = providePaymentMethodName,
            isCbcEligible = { paymentMethodMetadataFlow.value?.cbcEligibility is CardBrandChoiceEligibility.Eligible },
        )
    }

    val paymentOptionsItems: StateFlow<List<PaymentOptionsItem>> = paymentOptionsItemsMapper()

    val canEdit: StateFlow<Boolean> = combineAsStateFlow(
        customerStateHolder.canRemove,
        paymentOptionsItems
    ) { canRemove, items ->
        canRemove || items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>().any { item ->
            item.isModifiable
        }
    }

    private val _editing = MutableStateFlow(false)
    internal val editing: StateFlow<Boolean> = _editing

    init {
        coroutineScope.launch {
            selection.collect { selection ->
                if (selection is PaymentSelection.Saved) {
                    customerStateHolder.updateMostRecentlySelectedSavedPaymentMethod(selection.paymentMethod)
                }
            }
        }

        coroutineScope.launch {
            canEdit.collect { canEdit ->
                if (!canEdit && editing.value) {
                    _editing.value = false
                }
            }
        }

        coroutineScope.launch {
            customerStateHolder.paymentMethods.collect { paymentMethods ->
                if (paymentMethods.isEmpty() && editing.value) {
                    _editing.value = false
                }
            }
        }
    }

    fun toggleEditing() {
        _editing.update { !it }
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) {
        val paymentMethodId = paymentMethod.id ?: return

        coroutineScope.launch(workContext) {
            removeDeletedPaymentMethodFromState(paymentMethodId)
            removePaymentMethodInternal(paymentMethodId)
        }
    }

    private suspend fun removePaymentMethodInternal(paymentMethodId: String): Result<PaymentMethod> {
        // TODO(samer-stripe): Send 'unexpected_error' here
        val currentCustomer = customerStateHolder.customer.value ?: return Result.failure(
            IllegalStateException(
                "Could not remove payment method because CustomerConfiguration was not found! Make sure it is " +
                    "provided as part of PaymentSheet.Configuration"
            )
        )

        val currentSelection = (selection.value as? PaymentSelection.Saved)?.paymentMethod?.id
        val didRemoveSelectedItem = currentSelection == paymentMethodId

        if (didRemoveSelectedItem) {
            // Remove the current selection. The new selection will be set when we're computing
            // the next PaymentOptionsState.
            clearSelection()
        }

        return customerRepository.detachPaymentMethod(
            customerInfo = CustomerRepository.CustomerInfo(
                id = currentCustomer.id,
                ephemeralKeySecret = currentCustomer.ephemeralKeySecret,
                customerSessionClientSecret = currentCustomer.customerSessionClientSecret,
            ),
            paymentMethodId = paymentMethodId,
            canRemoveDuplicates = currentCustomer.permissions.canRemoveDuplicates,
        )
    }

    private fun removeDeletedPaymentMethodFromState(paymentMethodId: String) {
        val currentCustomer = customerStateHolder.customer.value ?: return

        customerStateHolder.setCustomerState(
            currentCustomer.copy(
                paymentMethods = currentCustomer.paymentMethods.filter {
                    it.id != paymentMethodId
                }
            )
        )

        if (customerStateHolder.mostRecentlySelectedSavedPaymentMethod.value?.id == paymentMethodId) {
            customerStateHolder.updateMostRecentlySelectedSavedPaymentMethod(null)
        }

        if ((selection.value as? PaymentSelection.Saved)?.paymentMethod?.id == paymentMethodId) {
            clearSelection()
        }

        onPaymentMethodRemoved()
    }

    fun updatePaymentMethod(displayableSavedPaymentMethod: DisplayableSavedPaymentMethod) {
        val paymentMethod = displayableSavedPaymentMethod.paymentMethod
        onUpdatePaymentMethod(
            displayableSavedPaymentMethod,
            customerStateHolder.canRemove.value,
            {
                removePaymentMethodInEditScreen(paymentMethod)
            },
            { cardBrand ->
                modifyCardPaymentMethod(paymentMethod, cardBrand)
            }
        )
    }

    private suspend fun removePaymentMethodInEditScreen(paymentMethod: PaymentMethod): Throwable? {
        val paymentMethodId = paymentMethod.id!!
        val result = removePaymentMethodInternal(paymentMethodId)

        if (result.isSuccess) {
            coroutineScope.launch(workContext) {
                navigationPop()
                delay(PaymentMethodRemovalDelayMillis)
                removeDeletedPaymentMethodFromState(paymentMethodId = paymentMethodId)
            }
        }

        return result.exceptionOrNull()
    }

    private suspend fun modifyCardPaymentMethod(
        paymentMethod: PaymentMethod,
        brand: CardBrand,
    ): Result<PaymentMethod> {
        // TODO(samer-stripe): Send 'unexpected_error' here
        val currentCustomer = customerStateHolder.customer.value ?: return Result.failure(
            IllegalStateException(
                "Could not update payment method because CustomerConfiguration was not found! Make sure it is " +
                    "provided as part of PaymentSheet.Configuration"
            )
        )

        return customerRepository.updatePaymentMethod(
            customerInfo = CustomerRepository.CustomerInfo(
                id = currentCustomer.id,
                ephemeralKeySecret = currentCustomer.ephemeralKeySecret,
                customerSessionClientSecret = currentCustomer.customerSessionClientSecret,
            ),
            paymentMethodId = paymentMethod.id!!,
            params = PaymentMethodUpdateParams.createCard(
                networks = PaymentMethodUpdateParams.Card.Networks(
                    preferred = brand.code
                ),
                productUsageTokens = setOf("PaymentSheet"),
            )
        ).onSuccess { updatedMethod ->
            customerStateHolder.updateMostRecentlySelectedSavedPaymentMethod(updatedMethod)
            customerStateHolder.setCustomerState(
                currentCustomer.copy(
                    paymentMethods = currentCustomer.paymentMethods.map { savedMethod ->
                        val savedId = savedMethod.id
                        val updatedId = updatedMethod.id

                        if (updatedId != null && savedId != null && updatedId == savedId) {
                            updatedMethod
                        } else {
                            savedMethod
                        }
                    }
                )
            )

            navigationPop()

            eventReporter.onUpdatePaymentMethodSucceeded(
                selectedBrand = brand
            )
        }.onFailure { error ->
            eventReporter.onUpdatePaymentMethodFailed(
                selectedBrand = brand,
                error = error,
            )
        }
    }

    companion object {
        private fun onPaymentMethodRemoved(viewModel: BaseSheetViewModel) {
            val currentScreen = viewModel.navigationHandler.currentScreen.value
            val shouldResetToAddPaymentMethodForm =
                viewModel.customerStateHolder.paymentMethods.value.isEmpty() &&
                    currentScreen is PaymentSheetScreen.SelectSavedPaymentMethods

            if (shouldResetToAddPaymentMethodForm) {
                val interactor = DefaultAddPaymentMethodInteractor.create(
                    viewModel = viewModel,
                    paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
                )
                val screen = PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                viewModel.navigationHandler.resetTo(listOf(screen))
            }
        }

        private fun onUpdatePaymentMethod(
            viewModel: BaseSheetViewModel,
            displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
            canRemove: Boolean,
            performRemove: suspend () -> Throwable?,
            updateExecutor: suspend (brand: CardBrand) -> Result<PaymentMethod>,
        ) {
            if (displayableSavedPaymentMethod.savedPaymentMethod != SavedPaymentMethod.Unexpected) {
                val isLiveMode = requireNotNull(viewModel.paymentMethodMetadata.value).stripeIntent.isLiveMode
                viewModel.navigationHandler.transitionTo(
                    PaymentSheetScreen.UpdatePaymentMethod(
                        DefaultUpdatePaymentMethodInteractor(
                            isLiveMode = isLiveMode,
                            canRemove = canRemove,
                            displayableSavedPaymentMethod,
                            cardBrandFilter = PaymentSheetCardBrandFilter(viewModel.config.cardBrandAcceptance),
                            removeExecutor = { method ->
                                performRemove()
                            },
                            updateExecutor = { method, brand ->
                                updateExecutor(brand)
                            },
                            onBrandChoiceOptionsShown = {
                                viewModel.eventReporter.onShowPaymentOptionBrands(
                                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                                    selectedBrand = it
                                )
                            },
                            onBrandChoiceOptionsDismissed = {
                                viewModel.eventReporter.onHidePaymentOptionBrands(
                                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                                    selectedBrand = it
                                )
                            },
                        )
                    )
                )
            }
        }

        fun create(viewModel: BaseSheetViewModel): SavedPaymentMethodMutator {
            return SavedPaymentMethodMutator(
                paymentMethodMetadataFlow = viewModel.paymentMethodMetadata,
                eventReporter = viewModel.eventReporter,
                coroutineScope = viewModel.viewModelScope,
                workContext = viewModel.workContext,
                customerRepository = viewModel.customerRepository,
                selection = viewModel.selection,
                customerStateHolder = viewModel.customerStateHolder,
                clearSelection = { viewModel.updateSelection(null) },
                onPaymentMethodRemoved = {
                    onPaymentMethodRemoved(viewModel)
                },
                onUpdatePaymentMethod = { displayableSavedPaymentMethod, canRemove, performRemove, updateExecutor ->
                    onUpdatePaymentMethod(
                        viewModel = viewModel,
                        displayableSavedPaymentMethod = displayableSavedPaymentMethod,
                        canRemove = canRemove,
                        performRemove = performRemove,
                        updateExecutor = updateExecutor,
                    )
                },
                navigationPop = viewModel.navigationHandler::pop,
                isLinkEnabled = viewModel.linkHandler.isLinkEnabled,
                isNotPaymentFlow = !viewModel.isCompleteFlow,
            ).apply {
                viewModel.viewModelScope.launch {
                    viewModel.navigationHandler.currentScreen.collect { currentScreen ->
                        when (currentScreen) {
                            is PaymentSheetScreen.VerticalMode -> {
                                // When returning to the vertical mode screen, reset editing to false.
                                _editing.value = false
                            }
                            else -> {
                                // Do nothing.
                            }
                        }
                    }
                }
            }
        }
    }
}
