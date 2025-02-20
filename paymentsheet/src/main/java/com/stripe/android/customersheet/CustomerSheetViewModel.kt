package com.stripe.android.customersheet

import android.app.Application
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.stripe.android.PaymentConfiguration
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.common.coroutines.Single
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.requireApplication
import com.stripe.android.customersheet.analytics.CustomerSheetEventReporter
import com.stripe.android.customersheet.data.CustomerSheetDataResult
import com.stripe.android.customersheet.data.CustomerSheetIntentDataSource
import com.stripe.android.customersheet.data.CustomerSheetPaymentMethodDataSource
import com.stripe.android.customersheet.data.CustomerSheetSavedSelectionDataSource
import com.stripe.android.customersheet.data.failureOrNull
import com.stripe.android.customersheet.data.mapCatching
import com.stripe.android.customersheet.data.onFailure
import com.stripe.android.customersheet.data.onSuccess
import com.stripe.android.customersheet.injection.CustomerSheetViewModelScope
import com.stripe.android.customersheet.injection.DaggerCustomerSheetViewModelComponent
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.customersheet.util.isUnverifiedUSBankAccount
import com.stripe.android.customersheet.util.sortPaymentMethods
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardBrandFilter
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.PaymentMethodConfirmationOption
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.toSavedSelection
import com.stripe.android.paymentsheet.parseAppearance
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.ui.DefaultUpdatePaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemovalDelayMillis
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext
import com.stripe.android.ui.core.R as UiCoreR

@CustomerSheetViewModelScope
internal class CustomerSheetViewModel(
    application: Application, // TODO (jameswoo) remove application
    private var originalPaymentSelection: PaymentSelection?,
    private val paymentConfigurationProvider: Provider<PaymentConfiguration>,
    private val paymentMethodDataSourceProvider: Single<CustomerSheetPaymentMethodDataSource>,
    private val intentDataSourceProvider: Single<CustomerSheetIntentDataSource>,
    private val savedSelectionDataSourceProvider: Single<CustomerSheetSavedSelectionDataSource>,
    private val configuration: CustomerSheet.Configuration,
    private val integrationType: CustomerSheetIntegration.Type,
    private val logger: Logger,
    private val stripeRepository: StripeRepository,
    private val eventReporter: CustomerSheetEventReporter,
    private val workContext: CoroutineContext = Dispatchers.IO,
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    confirmationHandlerFactory: ConfirmationHandler.Factory,
    private val customerSheetLoader: CustomerSheetLoader,
    private val errorReporter: ErrorReporter,
) : ViewModel() {

    @Inject constructor(
        application: Application,
        originalPaymentSelection: PaymentSelection?,
        paymentConfigurationProvider: Provider<PaymentConfiguration>,
        configuration: CustomerSheet.Configuration,
        integrationType: CustomerSheetIntegration.Type,
        logger: Logger,
        stripeRepository: StripeRepository,
        eventReporter: CustomerSheetEventReporter,
        workContext: CoroutineContext = Dispatchers.IO,
        @Named(IS_LIVE_MODE) isLiveModeProvider: () -> Boolean,
        confirmationHandlerFactory: ConfirmationHandler.Factory,
        customerSheetLoader: CustomerSheetLoader,
        errorReporter: ErrorReporter,
    ) : this(
        application = application,
        originalPaymentSelection = originalPaymentSelection,
        paymentConfigurationProvider = paymentConfigurationProvider,
        paymentMethodDataSourceProvider = CustomerSheetHacks.paymentMethodDataSource,
        intentDataSourceProvider = CustomerSheetHacks.intentDataSource,
        savedSelectionDataSourceProvider = CustomerSheetHacks.savedSelectionDataSource,
        configuration = configuration,
        integrationType = integrationType,
        logger = logger,
        stripeRepository = stripeRepository,
        eventReporter = eventReporter,
        workContext = workContext,
        isLiveModeProvider = isLiveModeProvider,
        confirmationHandlerFactory = confirmationHandlerFactory,
        customerSheetLoader = customerSheetLoader,
        errorReporter = errorReporter,
    )

    private val cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(application)

    private val backStack = MutableStateFlow<List<CustomerSheetViewState>>(
        listOf(
            CustomerSheetViewState.Loading(
                isLiveMode = isLiveModeProvider()
            )
        )
    )
    val viewState: StateFlow<CustomerSheetViewState> = backStack.mapAsStateFlow { it.last() }

    private val _result = MutableStateFlow<InternalCustomerSheetResult?>(null)
    val result: StateFlow<InternalCustomerSheetResult?> = _result

    private val confirmationHandler = confirmationHandlerFactory.create(
        scope = viewModelScope.plus(workContext)
    )

    private val isEditing = MutableStateFlow(false)
    private val selectionConfirmationState = MutableStateFlow(
        SelectionConfirmationState(
            isConfirming = false,
            error = null,
        )
    )
    private val customerState = MutableStateFlow(
        CustomerState(
            paymentMethods = listOf(),
            configuration = configuration,
            currentSelection = originalPaymentSelection,
            permissions = CustomerPermissions(
                canRemovePaymentMethods = false,
                canRemoveLastPaymentMethod = false,
            ),
            metadata = null,
        )
    )

    private val selectPaymentMethodState = combineAsStateFlow(
        customerState,
        selectionConfirmationState,
        isEditing,
    ) { customerState, selectionConfirmationState, editing ->
        val paymentMethods = customerState.paymentMethods
        val paymentMethodMetadata = customerState.metadata
        val paymentSelection = customerState.currentSelection

        val userCanEditAndIsEditing = editing && customerState.canEdit
        val primaryButtonVisible = !userCanEditAndIsEditing && originalPaymentSelection != paymentSelection

        CustomerSheetViewState.SelectPaymentMethod(
            title = configuration.headerTextForSelectionScreen,
            savedPaymentMethods = paymentMethods,
            paymentSelection = paymentSelection,
            isLiveMode = isLiveModeProvider(),
            canRemovePaymentMethods = customerState.canRemove,
            primaryButtonVisible = primaryButtonVisible,
            showGooglePay = shouldShowGooglePay(paymentMethodMetadata),
            isEditing = userCanEditAndIsEditing,
            isProcessing = selectionConfirmationState.isConfirming,
            errorMessage = selectionConfirmationState.error,
            isCbcEligible = customerState.cbcEligibility is CardBrandChoiceEligibility.Eligible,
            canEdit = customerState.canEdit,
            mandateText = paymentSelection?.mandateText(
                merchantName = configuration.merchantDisplayName,
                isSetupFlow = false,
            )?.takeIf { primaryButtonVisible }
        )
    }

    private var previouslySelectedPaymentMethod: SupportedPaymentMethod? = null
    private var supportedPaymentMethods = mutableListOf<SupportedPaymentMethod>()

    init {
        configuration.appearance.parseAppearance()

        eventReporter.onInit(configuration, integrationType)

        if (viewState.value is CustomerSheetViewState.Loading) {
            viewModelScope.launch(workContext) {
                loadCustomerSheetState()
            }
        }

        viewModelScope.launch {
            selectPaymentMethodState.collectLatest { selectPaymentMethodState ->
                updateViewState<CustomerSheetViewState.SelectPaymentMethod> {
                    selectPaymentMethodState
                }
            }
        }

        viewModelScope.launch {
            customerState.collectLatest { state ->
                if (
                    !state.canShowSavedPaymentMethods &&
                    viewState.value is CustomerSheetViewState.SelectPaymentMethod
                ) {
                    delay(REMOVAL_TRANSITION_DELAY)

                    transitionToAddPaymentMethod(isFirstPaymentMethod = true)

                    selectionConfirmationState.value = SelectionConfirmationState(
                        isConfirming = false,
                        error = null,
                    )
                }
            }
        }

        viewModelScope.launch {
            customerState.collectLatest { state ->
                if (!state.canEdit && isEditing.value) {
                    isEditing.value = false
                }
            }
        }
    }

    fun handleViewAction(viewAction: CustomerSheetViewAction) {
        when (viewAction) {
            is CustomerSheetViewAction.OnDismissed -> onDismissed()
            is CustomerSheetViewAction.OnAddCardPressed -> onAddCardPressed()
            is CustomerSheetViewAction.OnCardNumberInputCompleted -> onCardNumberInputCompleted()
            is CustomerSheetViewAction.OnDisallowedCardBrandEntered -> onDisallowedCardBrandEntered(viewAction.brand)
            is CustomerSheetViewAction.OnBackPressed -> onBackPressed()
            is CustomerSheetViewAction.OnEditPressed -> onEditPressed()
            is CustomerSheetViewAction.OnModifyItem -> onModifyItem(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnItemSelected -> onItemSelected(viewAction.selection)
            is CustomerSheetViewAction.OnPrimaryButtonPressed -> onPrimaryButtonPressed()
            is CustomerSheetViewAction.OnAddPaymentMethodItemChanged ->
                onAddPaymentMethodItemChanged(viewAction.paymentMethod)
            is CustomerSheetViewAction.OnFormFieldValuesCompleted -> {
                onFormFieldValuesCompleted(viewAction.formFieldValues)
            }
            is CustomerSheetViewAction.OnUpdateCustomButtonUIState -> {
                updateCustomButtonUIState(viewAction.callback)
            }
            is CustomerSheetViewAction.OnUpdateMandateText -> {
                updateMandateText(viewAction.mandateText, viewAction.showAbovePrimaryButton)
            }
            is CustomerSheetViewAction.OnBankAccountSelectionChanged -> {
                onCollectUSBankAccountResult(viewAction.paymentSelection)
            }
            is CustomerSheetViewAction.OnFormError -> {
                onFormError(viewAction.error)
            }
            is CustomerSheetViewAction.OnCancelClose -> {
                onCancelCloseForm()
            }
        }
    }

    /**
     * If true, the bottom sheet will be dismissed, otherwise the sheet will stay open
     */
    fun bottomSheetConfirmStateChange(): Boolean {
        val currentViewState = viewState.value
        return if (currentViewState.shouldDisplayDismissConfirmationModal()) {
            updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                it.copy(
                    displayDismissConfirmationModal = true,
                )
            }
            false
        } else {
            true
        }
    }

    fun providePaymentMethodName(code: PaymentMethodCode?): ResolvableString {
        return code?.let {
            customerState.value.metadata?.supportedPaymentMethodForCode(code)
        }?.displayName.orEmpty()
    }

    fun registerFromActivity(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner
    ) {
        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )
    }

    private suspend fun loadCustomerSheetState() {
        val result = withContext(workContext) {
            customerSheetLoader.load(
                configuration = configuration,
            )
        }

        result.fold(
            onSuccess = { state ->
                if (state.validationError != null) {
                    _result.update {
                        InternalCustomerSheetResult.Error(state.validationError)
                    }
                } else {
                    supportedPaymentMethods.clear()
                    supportedPaymentMethods.addAll(state.supportedPaymentMethods)

                    originalPaymentSelection = state.paymentSelection

                    customerState.value = CustomerState(
                        paymentMethods = state.customerPaymentMethods,
                        configuration = configuration,
                        currentSelection = state.paymentSelection,
                        metadata = state.paymentMethodMetadata,
                        permissions = state.customerPermissions,
                    )

                    transitionToInitialScreen()
                }
            },
            onFailure = { cause ->
                _result.update {
                    InternalCustomerSheetResult.Error(exception = cause)
                }
            }
        )
    }

    private fun transitionToInitialScreen() {
        val customerState = customerState.value

        if (customerState.canShowSavedPaymentMethods) {
            transition(
                to = selectPaymentMethodState.value,
                reset = true
            )
        } else {
            transitionToAddPaymentMethod(
                isFirstPaymentMethod = true,
            )
        }
    }

    private fun onAddCardPressed() {
        transitionToAddPaymentMethod(isFirstPaymentMethod = false)
    }

    private fun onDismissed() {
        _result.update {
            InternalCustomerSheetResult.Canceled(originalPaymentSelection)
        }
    }

    private fun onBackPressed() {
        if (backStack.value.size == 1) {
            _result.tryEmit(
                InternalCustomerSheetResult.Canceled(originalPaymentSelection)
            )
        } else {
            backStack.update {
                it.last().eventReporterScreen?.let { screen ->
                    eventReporter.onScreenHidden(screen)
                }

                it.dropLast(1)
            }
        }
    }

    private fun onEditPressed() {
        if (!customerState.value.canEdit) {
            return
        }

        val wasPreviouslyEditing = isEditing.value

        if (wasPreviouslyEditing) {
            eventReporter.onEditCompleted()
        } else {
            eventReporter.onEditTapped()
        }

        isEditing.value = !wasPreviouslyEditing
    }

    private fun onAddPaymentMethodItemChanged(paymentMethod: SupportedPaymentMethod) {
        (viewState.value as? CustomerSheetViewState.AddPaymentMethod)?.let {
            if (it.paymentMethodCode == paymentMethod.code) {
                return
            }
        }

        val customerState = customerState.value
        val paymentMethodMetadata = requireNotNull(customerState.metadata)

        eventReporter.onPaymentMethodSelected(paymentMethod.code)

        previouslySelectedPaymentMethod = paymentMethod

        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                paymentMethodCode = paymentMethod.code,
                formArguments = FormArgumentsFactory.create(
                    paymentMethodCode = paymentMethod.code,
                    metadata = paymentMethodMetadata,
                ),
                formElements = paymentMethodMetadata.formElementsForCode(
                    code = paymentMethod.code,
                    uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                        cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                        /*
                         * `CustomerSheet` does not implement `Link` so we don't need a coordinator or callback.
                         */
                        linkConfigurationCoordinator = null,
                        onLinkInlineSignupStateChanged = {
                            throw IllegalStateException(
                                "`CustomerSheet` does not implement `Link` and should not " +
                                    "receive `InlineSignUpViewState` updates"
                            )
                        }
                    ),
                ) ?: listOf(),
                primaryButtonLabel = if (
                    paymentMethod.code == USBankAccount.code && it.bankAccountSelection == null
                ) {
                    UiCoreR.string.stripe_continue_button_label.resolvableString
                } else {
                    R.string.stripe_paymentsheet_save.resolvableString
                },
                mandateText = it.draftPaymentSelection?.mandateText(
                    merchantName = configuration.merchantDisplayName,
                    isSetupFlow = true,
                ),
                primaryButtonEnabled = it.formFieldValues != null && !it.isProcessing,
            )
        }
    }

    private fun onFormFieldValuesCompleted(formFieldValues: FormFieldValues?) {
        customerState.value.metadata?.let { paymentMethodMetadata ->
            updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                it.copy(
                    formFieldValues = formFieldValues,
                    primaryButtonEnabled = formFieldValues != null && !it.isProcessing,
                    draftPaymentSelection = formFieldValues?.transformToPaymentSelection(
                        paymentMethod = it.supportedPaymentMethods.first { spm -> spm.code == it.paymentMethodCode },
                        paymentMethodMetadata = paymentMethodMetadata
                    )
                )
            }
        }
    }

    private suspend fun removePaymentMethod(paymentMethod: PaymentMethod): CustomerSheetDataResult<PaymentMethod> {
        return awaitPaymentMethodDataSource().detachPaymentMethod(
            paymentMethodId = paymentMethod.id!!,
        ).onSuccess {
            eventReporter.onRemovePaymentMethodSucceeded()
        }.onFailure { cause, _ ->
            eventReporter.onRemovePaymentMethodFailed()
            logger.error(
                msg = "Failed to detach payment method: $paymentMethod",
                t = cause,
            )
        }
    }

    private suspend fun modifyCardPaymentMethod(
        paymentMethod: PaymentMethod,
        brand: CardBrand
    ): CustomerSheetDataResult<PaymentMethod> {
        return awaitPaymentMethodDataSource().updatePaymentMethod(
            paymentMethodId = paymentMethod.id!!,
            params = PaymentMethodUpdateParams.createCard(
                networks = PaymentMethodUpdateParams.Card.Networks(
                    preferred = brand.code
                ),
                productUsageTokens = setOf("CustomerSheet"),
            )
        ).onSuccess { updatedMethod ->
            onBackPressed()
            updatePaymentMethodInState(updatedMethod)

            eventReporter.onUpdatePaymentMethodSucceeded(
                selectedBrand = brand
            )
        }.onFailure { cause, _ ->
            eventReporter.onUpdatePaymentMethodFailed(
                selectedBrand = brand,
                error = cause
            )
        }
    }

    private fun handlePaymentMethodRemovedFromEditScreen(paymentMethod: PaymentMethod) {
        viewModelScope.launch(workContext) {
            delay(PaymentMethodRemovalDelayMillis)
            removePaymentMethodFromState(paymentMethod)
        }
    }

    private fun onModifyItem(paymentMethod: DisplayableSavedPaymentMethod) {
        val customerState = customerState.value

        transition(
            to = CustomerSheetViewState.UpdatePaymentMethod(
                updatePaymentMethodInteractor = DefaultUpdatePaymentMethodInteractor(
                    isLiveMode = isLiveModeProvider(),
                    canRemove = customerState.canRemove,
                    displayableSavedPaymentMethod = paymentMethod,
                    cardBrandFilter = PaymentSheetCardBrandFilter(customerState.configuration.cardBrandAcceptance),
                    removeExecutor = ::removeExecutor,
                    onBrandChoiceOptionsShown = {
                        eventReporter.onShowPaymentOptionBrands(
                            source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
                            selectedBrand = it
                        )
                    },
                    onBrandChoiceOptionsDismissed = {
                        eventReporter.onHidePaymentOptionBrands(
                            source = CustomerSheetEventReporter.CardBrandChoiceEventSource.Edit,
                            selectedBrand = it
                        )
                    },
                    updateCardBrandExecutor = ::updateCardBrandExecutor,
                    workContext = workContext,
                    // This checkbox is never displayed in CustomerSheet.
                    shouldShowSetAsDefaultCheckbox = false,
                ),
                isLiveMode = isLiveModeProvider(),
            )
        )
    }

    private suspend fun removeExecutor(paymentMethod: PaymentMethod): Throwable? {
        return removePaymentMethod(paymentMethod = paymentMethod).onSuccess {
            onBackPressed()
            handlePaymentMethodRemovedFromEditScreen(paymentMethod)
        }.failureOrNull()?.cause
    }

    private suspend fun updateCardBrandExecutor(paymentMethod: PaymentMethod, brand: CardBrand): Result<PaymentMethod> {
        return when (val result = modifyCardPaymentMethod(paymentMethod, brand)) {
            is CustomerSheetDataResult.Success -> Result.success(result.value)
            is CustomerSheetDataResult.Failure -> Result.failure(result.cause)
        }
    }

    private fun removePaymentMethodFromState(paymentMethod: PaymentMethod) {
        val currentCustomerState = customerState.value
        val newSavedPaymentMethods = currentCustomerState.paymentMethods.filter { it.id != paymentMethod.id!! }

        val currentSelection = currentCustomerState.currentSelection
        val originalSelection = originalPaymentSelection

        val didRemoveCurrentSelection = currentSelection is PaymentSelection.Saved &&
            currentSelection.paymentMethod.id == paymentMethod.id

        val didRemoveOriginalSelection = originalSelection is PaymentSelection.Saved &&
            originalSelection.paymentMethod.id == paymentMethod.id

        if (didRemoveOriginalSelection) {
            originalPaymentSelection = null
        }

        customerState.value = currentCustomerState.copy(
            paymentMethods = newSavedPaymentMethods,
            currentSelection = currentSelection.takeUnless {
                didRemoveCurrentSelection
            } ?: originalPaymentSelection,
        )
    }

    private fun updatePaymentMethodInState(updatedMethod: PaymentMethod) {
        viewModelScope.launch {
            val currentCustomerState = customerState.value

            val newSavedPaymentMethods = currentCustomerState.paymentMethods.map { savedMethod ->
                val savedId = savedMethod.id
                val updatedId = updatedMethod.id

                if (updatedId != null && savedId != null && updatedId == savedId) {
                    updatedMethod
                } else {
                    savedMethod
                }
            }

            val originalSelection = originalPaymentSelection
            val currentSelection = currentCustomerState.currentSelection

            originalPaymentSelection = if (
                originalSelection is PaymentSelection.Saved &&
                originalSelection.paymentMethod.id == updatedMethod.id
            ) {
                originalSelection.copy(paymentMethod = updatedMethod)
            } else {
                originalSelection
            }

            val updatedCurrentSelection = if (
                currentSelection is PaymentSelection.Saved &&
                currentSelection.paymentMethod.id == updatedMethod.id
            ) {
                currentSelection.copy(paymentMethod = updatedMethod)
            } else {
                currentSelection
            }

            setCustomerState { state ->
                state.copy(
                    paymentMethods = newSavedPaymentMethods,
                    currentSelection = updatedCurrentSelection,
                )
            }
        }
    }

    private fun onItemSelected(paymentSelection: PaymentSelection?) {
        // TODO (jameswoo) consider clearing the error message onItemSelected, currently the only
        // error source is when the payment methods cannot be loaded
        when (paymentSelection) {
            is PaymentSelection.GooglePay, is PaymentSelection.Saved -> {
                if (isEditing.value) {
                    return
                }

                setCustomerState { state ->
                    state.copy(
                        currentSelection = paymentSelection,
                    )
                }
            }
            else -> error("Unsupported payment selection $paymentSelection")
        }
    }

    private fun onPrimaryButtonPressed() {
        when (val currentViewState = viewState.value) {
            is CustomerSheetViewState.AddPaymentMethod -> {
                if (currentViewState.customPrimaryButtonUiState != null) {
                    currentViewState.customPrimaryButtonUiState.onClick()
                    return
                }

                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = true,
                        primaryButtonEnabled = false,
                        enabled = false,
                    )
                }

                val createParams = if (currentViewState.paymentMethodCode == USBankAccount.code) {
                    currentViewState.bankAccountSelection?.paymentMethodCreateParams
                        ?: error("Invalid bankAccountSelection")
                } else {
                    val formFieldValues = currentViewState.formFieldValues ?: error("completeFormValues cannot be null")
                    formFieldValues.transformToPaymentMethodCreateParams(
                        paymentMethodCode = currentViewState.paymentMethodCode,
                        paymentMethodMetadata = requireNotNull(customerState.value.metadata)
                    )
                }

                createAndAttach(createParams)
            }
            is CustomerSheetViewState.SelectPaymentMethod -> {
                setSelectionConfirmationState { state ->
                    state.copy(
                        isConfirming = true
                    )
                }
                when (val paymentSelection = currentViewState.paymentSelection) {
                    is PaymentSelection.GooglePay -> selectGooglePay()
                    is PaymentSelection.Saved -> selectSavedPaymentMethod(paymentSelection)
                    null -> selectSavedPaymentMethod(null)
                    else -> error("$paymentSelection is not supported")
                }
            }
            else -> error("${viewState.value} is not supported")
        }
    }

    private fun createAndAttach(
        paymentMethodCreateParams: PaymentMethodCreateParams,
    ) {
        viewModelScope.launch(workContext) {
            createPaymentMethod(paymentMethodCreateParams)
                .onSuccess { paymentMethod ->
                    if (paymentMethod.isUnverifiedUSBankAccount()) {
                        _result.tryEmit(
                            InternalCustomerSheetResult.Selected(
                                paymentSelection = PaymentSelection.Saved(paymentMethod)
                            )
                        )
                    } else {
                        attachPaymentMethodToCustomer(paymentMethod)
                    }
                }.onFailure { throwable ->
                    logger.error(
                        msg = "Failed to create payment method for ${paymentMethodCreateParams.typeCode}",
                        t = throwable,
                    )
                    updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                        it.copy(
                            errorMessage = throwable.stripeErrorMessage(),
                            primaryButtonEnabled = it.formFieldValues != null,
                            isProcessing = false,
                        )
                    }
                }
        }
    }

    private fun transitionToAddPaymentMethod(
        isFirstPaymentMethod: Boolean,
    ) {
        val customerState = customerState.value
        val paymentMethodMetadata = requireNotNull(customerState.metadata)

        val paymentMethodCode = previouslySelectedPaymentMethod?.code
            ?: paymentMethodMetadata.supportedPaymentMethodTypes().firstOrNull()
            ?: PaymentMethod.Type.Card.code

        val formArguments = FormArgumentsFactory.create(
            paymentMethodCode = paymentMethodCode,
            metadata = paymentMethodMetadata,
        )

        val selectedPaymentMethod = previouslySelectedPaymentMethod
            ?: requireNotNull(paymentMethodMetadata.supportedPaymentMethodForCode(paymentMethodCode))

        val stripeIntent = paymentMethodMetadata.stripeIntent
        val formElements = paymentMethodMetadata.formElementsForCode(
            code = selectedPaymentMethod.code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                /*
                 * `CustomerSheet` does not implement `Link` so we don't need a coordinator or callback.
                 */
                linkConfigurationCoordinator = null,
                onLinkInlineSignupStateChanged = {
                    throw IllegalStateException(
                        "`CustomerSheet` does not implement `Link` and should not " +
                            "receive `InlineSignUpViewState` updates"
                    )
                }
            )
        ) ?: emptyList()

        transition(
            to = CustomerSheetViewState.AddPaymentMethod(
                paymentMethodCode = paymentMethodCode,
                supportedPaymentMethods = supportedPaymentMethods,
                formFieldValues = null,
                formElements = formElements,
                formArguments = formArguments,
                usBankAccountFormArguments = createDefaultUsBankArguments(stripeIntent),
                draftPaymentSelection = null,
                enabled = true,
                isLiveMode = isLiveModeProvider(),
                isProcessing = false,
                isFirstPaymentMethod = isFirstPaymentMethod,
                primaryButtonLabel = R.string.stripe_paymentsheet_save.resolvableString,
                primaryButtonEnabled = false,
                customPrimaryButtonUiState = null,
                bankAccountSelection = null,
                errorReporter = errorReporter,
            ),
            reset = isFirstPaymentMethod
        )
    }

    private fun createDefaultUsBankArguments(stripeIntent: StripeIntent?): USBankAccountFormArguments {
        return USBankAccountFormArguments(
            instantDebits = false,
            incentive = null,
            linkMode = null,
            showCheckbox = false,
            onBehalfOf = null,
            isCompleteFlow = false,
            isPaymentFlow = false,
            stripeIntentId = stripeIntent?.id,
            clientSecret = stripeIntent?.clientSecret,
            shippingDetails = null,
            draftPaymentSelection = null,
            onMandateTextChanged = { mandate, showAbove ->
                handleViewAction(CustomerSheetViewAction.OnUpdateMandateText(mandate, showAbove))
            },
            onLinkedBankAccountChanged = {
                handleViewAction(CustomerSheetViewAction.OnBankAccountSelectionChanged(it))
            },
            onUpdatePrimaryButtonUIState = {
                handleViewAction(CustomerSheetViewAction.OnUpdateCustomButtonUIState(it))
            },
            hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_CUSTOMER_SHEET,
            onUpdatePrimaryButtonState = { /* no-op, CustomerSheetScreen does not use PrimaryButton.State */ },
            onError = { error ->
                handleViewAction(CustomerSheetViewAction.OnFormError(error))
            }
        )
    }

    private fun updateCustomButtonUIState(callback: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            val uiState = callback(it.customPrimaryButtonUiState)
            if (uiState != null) {
                it.copy(
                    primaryButtonEnabled = uiState.enabled,
                    customPrimaryButtonUiState = uiState,
                )
            } else {
                val enabled = it.paymentMethodCode == USBankAccount.code || it.formFieldValues != null

                it.copy(
                    primaryButtonEnabled = enabled && !it.isProcessing,
                    customPrimaryButtonUiState = null,
                )
            }
        }
    }

    private fun updateMandateText(mandateText: ResolvableString?, showAbove: Boolean) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                mandateText = mandateText,
                showMandateAbovePrimaryButton = showAbove,
            )
        }
    }

    private fun onCollectUSBankAccountResult(
        paymentSelection: PaymentSelection.New.USBankAccount?,
    ) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                bankAccountSelection = paymentSelection,
                primaryButtonLabel = if (paymentSelection != null) {
                    R.string.stripe_paymentsheet_save.resolvableString
                } else {
                    UiCoreR.string.stripe_continue_button_label.resolvableString
                },
            )
        }
    }

    private fun onCardNumberInputCompleted() {
        eventReporter.onCardNumberCompleted()
    }

    private fun onDisallowedCardBrandEntered(brand: CardBrand) {
        eventReporter.onDisallowedCardBrandEntered(brand)
    }

    private fun onFormError(error: ResolvableString?) {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                errorMessage = error
            )
        }
    }

    private fun onCancelCloseForm() {
        updateViewState<CustomerSheetViewState.AddPaymentMethod> {
            it.copy(
                displayDismissConfirmationModal = false,
            )
        }
    }

    private fun setCustomerState(update: (CustomerState) -> CustomerState) {
        customerState.value = update(customerState.value)
    }

    private fun setSelectionConfirmationState(update: (SelectionConfirmationState) -> SelectionConfirmationState) {
        selectionConfirmationState.value = update(selectionConfirmationState.value)
    }

    private suspend fun createPaymentMethod(
        createParams: PaymentMethodCreateParams
    ): Result<PaymentMethod> {
        return stripeRepository.createPaymentMethod(
            paymentMethodCreateParams = createParams,
            options = ApiRequest.Options(
                apiKey = paymentConfigurationProvider.get().publishableKey,
                stripeAccount = paymentConfigurationProvider.get().stripeAccountId,
            )
        )
    }

    private fun attachPaymentMethodToCustomer(paymentMethod: PaymentMethod) {
        viewModelScope.launch(workContext) {
            if (awaitIntentDataSource().canCreateSetupIntents) {
                attachWithSetupIntent(paymentMethod = paymentMethod)
            } else {
                attachPaymentMethod(id = paymentMethod.id!!)
            }
        }
    }

    private suspend fun attachWithSetupIntent(paymentMethod: PaymentMethod) {
        awaitIntentDataSource().retrieveSetupIntentClientSecret()
            .mapCatching { clientSecret ->
                val intent = stripeRepository.retrieveSetupIntent(
                    clientSecret = clientSecret,
                    options = ApiRequest.Options(
                        apiKey = paymentConfigurationProvider.get().publishableKey,
                        stripeAccount = paymentConfigurationProvider.get().stripeAccountId,
                    ),
                ).getOrThrow()

                handleStripeIntent(intent, clientSecret, paymentMethod)
            }.onFailure { cause, displayMessage ->
                eventReporter.onAttachPaymentMethodFailed(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
                )

                logger.error(
                    msg = "Failed to attach payment method to SetupIntent: $paymentMethod",
                    t = cause,
                )

                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        errorMessage = displayMessage?.resolvableString ?: cause.stripeErrorMessage(),
                        enabled = true,
                        primaryButtonEnabled = it.formFieldValues != null && !it.isProcessing,
                        isProcessing = false,
                    )
                }
            }
    }

    private suspend fun handleStripeIntent(
        stripeIntent: StripeIntent,
        clientSecret: String,
        paymentMethod: PaymentMethod
    ) {
        confirmationHandler.start(
            arguments = ConfirmationHandler.Args(
                confirmationOption = PaymentMethodConfirmationOption.Saved(
                    paymentMethod = paymentMethod,
                    optionsParams = null,
                ),
                intent = stripeIntent,
                initializationMode = PaymentElementLoader.InitializationMode.SetupIntent(
                    clientSecret = clientSecret
                ),
                shippingDetails = null,
                appearance = configuration.appearance,
            )
        )

        when (val result = confirmationHandler.awaitResult()) {
            is ConfirmationHandler.Result.Succeeded -> {
                eventReporter.onAttachPaymentMethodSucceeded(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
                )

                refreshAndUpdatePaymentMethods(paymentMethod)
            }
            is ConfirmationHandler.Result.Failed -> {
                eventReporter.onAttachPaymentMethodFailed(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.SetupIntent
                )

                logger.error(
                    msg = "Failed to attach payment method to SetupIntent: $paymentMethod",
                    t = result.cause,
                )

                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        isProcessing = false,
                        primaryButtonEnabled = it.formFieldValues != null,
                        errorMessage = result.message,
                    )
                }
            }
            is ConfirmationHandler.Result.Canceled,
            null -> {
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        enabled = true,
                        isProcessing = false,
                        primaryButtonEnabled = it.formFieldValues != null,
                    )
                }
            }
        }
    }

    private suspend fun attachPaymentMethod(id: String) {
        awaitPaymentMethodDataSource().attachPaymentMethod(id)
            .onSuccess { attachedPaymentMethod ->
                eventReporter.onAttachPaymentMethodSucceeded(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
                )
                refreshAndUpdatePaymentMethods(attachedPaymentMethod)
            }.onFailure { cause, displayMessage ->
                eventReporter.onAttachPaymentMethodFailed(
                    style = CustomerSheetEventReporter.AddPaymentMethodStyle.CreateAttach
                )
                logger.error(
                    msg = "Failed to attach payment method $id to customer",
                    t = cause,
                )
                updateViewState<CustomerSheetViewState.AddPaymentMethod> {
                    it.copy(
                        errorMessage = displayMessage?.resolvableString,
                        primaryButtonEnabled = it.formFieldValues != null,
                        isProcessing = false,
                    )
                }
            }
    }

    private suspend fun refreshAndUpdatePaymentMethods(
        newPaymentMethod: PaymentMethod
    ) {
        awaitPaymentMethodDataSource().retrievePaymentMethods().onSuccess { paymentMethods ->
            errorReporter.report(
                ErrorReporter.SuccessEvent.CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_SUCCESS,
            )

            setCustomerState { state ->
                val selection = paymentMethods.find { paymentMethod ->
                    newPaymentMethod.id == paymentMethod.id
                }?.let {
                    PaymentSelection.Saved(it)
                } ?: state.currentSelection

                state.copy(
                    paymentMethods = sortPaymentMethods(paymentMethods, selection as? PaymentSelection.Saved),
                    currentSelection = selection,
                )
            }

            transition(
                to = selectPaymentMethodState.value,
                reset = true,
            )
        }.onFailure { exception, _ ->
            errorReporter.report(
                ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_REFRESH_FAILURE,
                StripeException.create(exception),
            )

            onDismissed()
        }
    }

    private fun selectSavedPaymentMethod(savedPaymentSelection: PaymentSelection.Saved?) {
        viewModelScope.launch(workContext) {
            awaitSavedSelectionDataSource().setSavedSelection(
                savedPaymentSelection?.toSavedSelection(),
                shouldSyncDefault =
                customerState.value.metadata?.customerMetadata?.isPaymentMethodSetAsDefaultEnabled == true,
            ).onSuccess {
                confirmPaymentSelection(
                    paymentSelection = savedPaymentSelection,
                    type = savedPaymentSelection?.paymentMethod?.type?.code,
                )
            }.onFailure { cause, displayMessage ->
                confirmPaymentSelectionError(
                    paymentSelection = savedPaymentSelection,
                    type = savedPaymentSelection?.paymentMethod?.type?.code,
                    cause = cause,
                    displayMessage = displayMessage,
                )
            }
        }
    }

    private fun selectGooglePay() {
        viewModelScope.launch(workContext) {
            awaitSavedSelectionDataSource().setSavedSelection(SavedSelection.GooglePay, shouldSyncDefault = false)
                .onSuccess {
                    confirmPaymentSelection(
                        paymentSelection = PaymentSelection.GooglePay,
                        type = "google_pay"
                    )
                }.onFailure { cause, displayMessage ->
                    confirmPaymentSelectionError(
                        paymentSelection = PaymentSelection.GooglePay,
                        type = "google_pay",
                        cause = cause,
                        displayMessage = displayMessage,
                    )
                }
        }
    }

    private fun confirmPaymentSelection(paymentSelection: PaymentSelection?, type: String?) {
        type?.let {
            eventReporter.onConfirmPaymentMethodSucceeded(type)
        }
        _result.tryEmit(
            InternalCustomerSheetResult.Selected(
                paymentSelection = paymentSelection,
            )
        )
    }

    private fun confirmPaymentSelectionError(
        paymentSelection: PaymentSelection?,
        type: String?,
        cause: Throwable,
        displayMessage: String?
    ) {
        type?.let {
            eventReporter.onConfirmPaymentMethodFailed(type)
        }
        logger.error(
            msg = "Failed to persist payment selection: $paymentSelection",
            t = cause,
        )
        setSelectionConfirmationState { state ->
            state.copy(
                isConfirming = false,
                error = displayMessage,
            )
        }
    }

    private fun transition(to: CustomerSheetViewState, reset: Boolean = false) {
        when (to) {
            is CustomerSheetViewState.AddPaymentMethod ->
                eventReporter.onScreenPresented(CustomerSheetEventReporter.Screen.AddPaymentMethod)
            is CustomerSheetViewState.SelectPaymentMethod ->
                eventReporter.onScreenPresented(CustomerSheetEventReporter.Screen.SelectPaymentMethod)
            is CustomerSheetViewState.UpdatePaymentMethod ->
                eventReporter.onScreenPresented(CustomerSheetEventReporter.Screen.EditPaymentMethod)
            else -> { }
        }

        backStack.update {
            if (reset) listOf(to) else it + to
        }
    }

    private inline fun <reified T : CustomerSheetViewState> updateViewState(transform: (T) -> T) {
        backStack.update { currentBackStack ->
            currentBackStack.map {
                if (it is T) {
                    transform(it)
                } else {
                    it
                }
            }
        }
    }

    private suspend fun awaitPaymentMethodDataSource(): CustomerSheetPaymentMethodDataSource {
        return paymentMethodDataSourceProvider.await()
    }

    private suspend fun awaitIntentDataSource(): CustomerSheetIntentDataSource {
        return intentDataSourceProvider.await()
    }

    private suspend fun awaitSavedSelectionDataSource(): CustomerSheetSavedSelectionDataSource {
        return savedSelectionDataSourceProvider.await()
    }

    private val CustomerSheetViewState.eventReporterScreen: CustomerSheetEventReporter.Screen?
        get() = when (this) {
            is CustomerSheetViewState.AddPaymentMethod -> CustomerSheetEventReporter.Screen.AddPaymentMethod
            is CustomerSheetViewState.SelectPaymentMethod -> CustomerSheetEventReporter.Screen.SelectPaymentMethod
            is CustomerSheetViewState.UpdatePaymentMethod -> CustomerSheetEventReporter.Screen.EditPaymentMethod
            else -> null
        }

    private data class CustomerState(
        val paymentMethods: List<PaymentMethod>,
        val currentSelection: PaymentSelection?,
        val metadata: PaymentMethodMetadata?,
        val permissions: CustomerPermissions,
        val configuration: CustomerSheet.Configuration,
    ) {
        val canRemove = when (paymentMethods.size) {
            0 -> false
            1 -> permissions.canRemoveLastPaymentMethod && permissions.canRemovePaymentMethods
            else -> permissions.canRemovePaymentMethods
        }

        val cbcEligibility = metadata?.cbcEligibility ?: CardBrandChoiceEligibility.Ineligible

        val canEdit = canRemove || paymentMethods.any { method ->
            isModifiable(method, cbcEligibility)
        }

        val canShowSavedPaymentMethods = paymentMethods.isNotEmpty() || shouldShowGooglePay(metadata)
    }

    private data class SelectionConfirmationState(
        val isConfirming: Boolean,
        val error: String?,
    )

    internal companion object {
        const val REMOVAL_TRANSITION_DELAY = 50L

        fun shouldShowGooglePay(paymentMethodMetadata: PaymentMethodMetadata?): Boolean {
            return paymentMethodMetadata?.isGooglePayReady == true &&
                paymentMethodMetadata.customerMetadata?.isPaymentMethodSetAsDefaultEnabled != true
        }
    }

    class Factory(
        private val args: CustomerSheetContract.Args,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val component = DaggerCustomerSheetViewModelComponent.builder()
                .application(extras.requireApplication())
                .configuration(args.configuration)
                .integrationType(args.integrationType)
                .statusBarColor(args.statusBarColor)
                .savedStateHandle(extras.createSavedStateHandle())
                .build()

            return component.viewModel as T
        }
    }
}
