package com.stripe.android.paymentsheet

import androidx.lifecycle.viewModelScope
import com.stripe.android.CardBrandFilter
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
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
    private val editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory,
    private val eventReporter: EventReporter,
    private val coroutineScope: CoroutineScope,
    private val workContext: CoroutineContext,
    private val navigationHandler: NavigationHandler,
    private val customerRepository: CustomerRepository,
    private val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    private val selection: StateFlow<PaymentSelection?>,
    val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString,
    private val addFirstPaymentMethodScreenFactory: () -> PaymentSheetScreen,
    private val clearSelection: () -> Unit,
    private val isLiveModeProvider: () -> Boolean,
    private val customerStateHolder: CustomerStateHolder,
    private val currentScreen: StateFlow<PaymentSheetScreen>,
    private val cardBrandFilter: CardBrandFilter,
    isCbcEligible: () -> Boolean,
    isGooglePayReady: StateFlow<Boolean>,
    isLinkEnabled: StateFlow<Boolean?>,
    isNotPaymentFlow: Boolean,
) {
    val canRemove: StateFlow<Boolean> = customerStateHolder.customer.mapAsStateFlow { customerState ->
        customerState?.run {
            val hasRemovePermissions = customerState.permissions.canRemovePaymentMethods

            when (paymentMethods.size) {
                0 -> false
                1 -> allowsRemovalOfLastSavedPaymentMethod && hasRemovePermissions
                else -> hasRemovePermissions
            }
        } ?: false
    }

    private val paymentOptionsItemsMapper: PaymentOptionsItemsMapper by lazy {
        PaymentOptionsItemsMapper(
            customerState = customerStateHolder.customer,
            isGooglePayReady = isGooglePayReady,
            isLinkEnabled = isLinkEnabled,
            isNotPaymentFlow = isNotPaymentFlow,
            nameProvider = providePaymentMethodName,
            isCbcEligible = isCbcEligible,
            canRemovePaymentMethods = canRemove,
        )
    }

    val paymentOptionsItems: StateFlow<List<PaymentOptionsItem>> = paymentOptionsItemsMapper()

    val canEdit: StateFlow<Boolean> = combineAsStateFlow(
        canRemove,
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

        coroutineScope.launch {
            currentScreen.collect { currentScreen ->
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
            CustomerRepository.CustomerInfo(
                id = currentCustomer.id,
                ephemeralKeySecret = currentCustomer.ephemeralKeySecret
            ),
            paymentMethodId,
            currentCustomer.permissions.canRemoveDuplicates,
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

        val shouldResetToAddPaymentMethodForm = customerStateHolder.paymentMethods.value.isEmpty() &&
            navigationHandler.currentScreen.value is PaymentSheetScreen.SelectSavedPaymentMethods

        if (shouldResetToAddPaymentMethodForm) {
            navigationHandler.resetTo(listOf(addFirstPaymentMethodScreenFactory()))
        }
    }

    fun modifyPaymentMethod(paymentMethod: PaymentMethod) {
        navigationHandler.transitionTo(
            PaymentSheetScreen.EditPaymentMethod(
                editInteractorFactory.create(
                    initialPaymentMethod = paymentMethod,
                    eventHandler = { event ->
                        when (event) {
                            is EditPaymentMethodViewInteractor.Event.ShowBrands -> {
                                eventReporter.onShowPaymentOptionBrands(
                                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                                    selectedBrand = event.brand
                                )
                            }
                            is EditPaymentMethodViewInteractor.Event.HideBrands -> {
                                eventReporter.onHidePaymentOptionBrands(
                                    source = EventReporter.CardBrandChoiceEventSource.Edit,
                                    selectedBrand = event.brand
                                )
                            }
                        }
                    },
                    displayName = providePaymentMethodName(paymentMethod.type?.code),
                    removeExecutor = { method ->
                        removePaymentMethodInEditScreen(method)
                    },
                    updateExecutor = { method, brand ->
                        modifyCardPaymentMethod(method, brand)
                    },
                    canRemove = canRemove.value,
                    isLiveMode = isLiveModeProvider(),
                    cardBrandFilter = cardBrandFilter
                ),
            )
        )
    }

    fun updatePaymentMethod(displayableSavedPaymentMethod: DisplayableSavedPaymentMethod) {
        displayableSavedPaymentMethod.paymentMethod.card?.let {
            navigationHandler.transitionTo(
                PaymentSheetScreen.UpdatePaymentMethod(
                    DefaultUpdatePaymentMethodInteractor(
                        isLiveMode = isLiveModeProvider(),
                        displayableSavedPaymentMethod,
                        card = it,
                    )
                )
            )
        }
    }

    private suspend fun removePaymentMethodInEditScreen(paymentMethod: PaymentMethod): Throwable? {
        val paymentMethodId = paymentMethod.id!!
        val result = removePaymentMethodInternal(paymentMethodId)

        if (result.isSuccess) {
            coroutineScope.launch(workContext) {
                navigationHandler.pop()
                delay(PaymentMethodRemovalDelayMillis)
                removeDeletedPaymentMethodFromState(paymentMethodId = paymentMethodId)
            }
        }

        return result.exceptionOrNull()
    }

    private suspend fun modifyCardPaymentMethod(
        paymentMethod: PaymentMethod,
        brand: CardBrand
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
                ephemeralKeySecret = currentCustomer.ephemeralKeySecret
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

            navigationHandler.pop()

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
        fun create(viewModel: BaseSheetViewModel): SavedPaymentMethodMutator {
            return SavedPaymentMethodMutator(
                editInteractorFactory = viewModel.editInteractorFactory,
                eventReporter = viewModel.eventReporter,
                coroutineScope = viewModel.viewModelScope,
                workContext = viewModel.workContext,
                navigationHandler = viewModel.navigationHandler,
                customerRepository = viewModel.customerRepository,
                allowsRemovalOfLastSavedPaymentMethod = viewModel.config.allowsRemovalOfLastSavedPaymentMethod,
                selection = viewModel.selection,
                providePaymentMethodName = { code ->
                    code?.let {
                        viewModel.paymentMethodMetadata.value?.supportedPaymentMethodForCode(code)
                    }?.displayName.orEmpty()
                },
                customerStateHolder = viewModel.customerStateHolder,
                addFirstPaymentMethodScreenFactory = {
                    val interactor = DefaultAddPaymentMethodInteractor.create(
                        viewModel = viewModel,
                        paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
                    )
                    PaymentSheetScreen.AddFirstPaymentMethod(interactor)
                },
                clearSelection = { viewModel.updateSelection(null) },
                isCbcEligible = {
                    viewModel.paymentMethodMetadata.value?.cbcEligibility is CardBrandChoiceEligibility.Eligible
                },
                isGooglePayReady = viewModel.paymentMethodMetadata.mapAsStateFlow { it?.isGooglePayReady == true },
                isLinkEnabled = viewModel.linkHandler.isLinkEnabled,
                isNotPaymentFlow = !viewModel.isCompleteFlow,
                isLiveModeProvider = { requireNotNull(viewModel.paymentMethodMetadata.value).stripeIntent.isLiveMode },
                currentScreen = viewModel.navigationHandler.currentScreen,
                cardBrandFilter = PaymentSheetCardBrandFilter(viewModel.config.cardBrandAcceptance)
            )
        }
    }
}
