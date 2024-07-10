package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetAnalyticsListener
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave.RequestReuse
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemovalDelayMillis
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combine
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
@Suppress("TooManyFunctions")
internal abstract class BaseSheetViewModel(
    application: Application,
    internal val config: PaymentSheet.Configuration,
    internal val eventReporter: EventReporter,
    protected val customerRepository: CustomerRepository,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO,
    protected val logger: Logger,
    val savedStateHandle: SavedStateHandle,
    val linkHandler: LinkHandler,
    val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val headerTextFactory: HeaderTextFactory,
    private val editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory
) : AndroidViewModel(application) {

    private val cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(application)

    internal val merchantName = config.merchantDisplayName

    protected var mostRecentError: Throwable? = null

    internal val googlePayState: StateFlow<GooglePayState> = savedStateHandle
        .getStateFlow(SAVE_GOOGLE_PAY_STATE, GooglePayState.Indeterminate)

    private val _paymentMethodMetadata = MutableStateFlow<PaymentMethodMetadata?>(null)
    internal val paymentMethodMetadata: StateFlow<PaymentMethodMetadata?> = _paymentMethodMetadata

    internal var supportedPaymentMethods: List<SupportedPaymentMethod> = emptyList()
        set(value) {
            field = value
            _supportedPaymentMethodsFlow.tryEmit(value.map { it.code })
        }

    private val _supportedPaymentMethodsFlow = MutableStateFlow<List<PaymentMethodCode>>(emptyList())
    val supportedPaymentMethodsFlow: StateFlow<List<PaymentMethodCode>> = _supportedPaymentMethodsFlow

    protected var customer: CustomerState?
        get() = savedStateHandle[SAVED_CUSTOMER]
        set(value) {
            savedStateHandle[SAVED_CUSTOMER] = value
        }

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: StateFlow<List<PaymentMethod>> = savedStateHandle
        .getStateFlow<CustomerState?>(SAVED_CUSTOMER, null)
        .mapAsStateFlow { state ->
            state?.paymentMethods ?: emptyList()
        }

    val navigationHandler: NavigationHandler = NavigationHandler()

    abstract val walletsState: StateFlow<WalletsState?>
    abstract val walletsProcessingState: StateFlow<WalletsProcessingState?>

    internal val headerText: StateFlow<Int?> by lazy {
        combineAsStateFlow(
            navigationHandler.currentScreen,
            walletsState,
            supportedPaymentMethodsFlow,
            editing,
        ) { screen, walletsState, supportedPaymentMethods, editing ->
            mapToHeaderTextResource(screen, walletsState, supportedPaymentMethods, editing)
        }
    }

    internal val selection: StateFlow<PaymentSelection?> = savedStateHandle
        .getStateFlow<PaymentSelection?>(SAVE_SELECTION, null)

    internal val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?> = savedStateHandle.getStateFlow(
        SAVED_PM_SELECTION,
        initialValue = (selection.value as? PaymentSelection.Saved)?.paymentMethod
    )

    private val _editing = MutableStateFlow(false)
    internal val editing: StateFlow<Boolean> = _editing

    val processing: StateFlow<Boolean> = savedStateHandle
        .getStateFlow(SAVE_PROCESSING, false)

    private val _contentVisible = MutableStateFlow(true)
    internal val contentVisible: StateFlow<Boolean> = _contentVisible

    private val _primaryButtonState = MutableStateFlow<PrimaryButton.State?>(null)
    val primaryButtonState: StateFlow<PrimaryButton.State?> = _primaryButtonState

    protected val customPrimaryButtonUiState = MutableStateFlow<PrimaryButton.UIState?>(null)

    abstract val primaryButtonUiState: StateFlow<PrimaryButton.UIState?>
    abstract val error: StateFlow<String?>

    private val _mandateText = MutableStateFlow<MandateText?>(null)
    internal val mandateText: StateFlow<MandateText?> = _mandateText

    private val linkInlineSignUpState = MutableStateFlow<InlineSignupViewState?>(null)
    protected val linkEmailFlow: StateFlow<String?> = linkConfigurationCoordinator.emailFlow

    private val _cvcControllerFlow = MutableStateFlow(CvcController(CvcConfig(), stateFlowOf(CardBrand.Unknown)))
    internal val cvcControllerFlow: StateFlow<CvcController> = _cvcControllerFlow

    private val _cvcRecollectionCompleteFlow = MutableStateFlow(true)
    internal val cvcRecollectionCompleteFlow: StateFlow<Boolean> = _cvcRecollectionCompleteFlow

    val analyticsListener: PaymentSheetAnalyticsListener = PaymentSheetAnalyticsListener(
        savedStateHandle = savedStateHandle,
        eventReporter = eventReporter,
        currentScreen = navigationHandler.currentScreen,
        coroutineScope = viewModelScope,
        currentPaymentMethodTypeProvider = { initiallySelectedPaymentMethodType },
    )

    /**
     * This should be initialized from the starter args, and then from that point forward it will be
     * the last valid new payment method entered by the user.
     * In contrast to selection, this field will not be updated by the list fragment. It is used to
     * save a new payment method that is added so that the payment data entered is recovered when
     * the user returns to that payment method type.
     */
    abstract var newPaymentSelection: NewOrExternalPaymentSelection?

    abstract fun onFatal(throwable: Throwable)

    protected val buttonsEnabled = combineAsStateFlow(
        processing,
        editing,
    ) { isProcessing, isEditing ->
        !isProcessing && !isEditing
    }

    private val paymentOptionsItemsMapper: PaymentOptionsItemsMapper by lazy {
        PaymentOptionsItemsMapper(
            paymentMethods = paymentMethods,
            googlePayState = googlePayState,
            isLinkEnabled = linkHandler.isLinkEnabled,
            isNotPaymentFlow = this is PaymentOptionsViewModel,
            nameProvider = ::providePaymentMethodName,
            isCbcEligible = { paymentMethodMetadata.value?.cbcEligibility is CardBrandChoiceEligibility.Eligible }
        )
    }

    internal fun providePaymentMethodName(code: PaymentMethodCode?): String {
        return code?.let {
            paymentMethodMetadata.value?.supportedPaymentMethodForCode(code)
        }?.displayName?.resolve(getApplication()).orEmpty()
    }

    val paymentOptionsItems: StateFlow<List<PaymentOptionsItem>> = paymentOptionsItemsMapper()

    private val canEdit: StateFlow<Boolean> = paymentOptionsItems.mapAsStateFlow { items ->
        val paymentMethods = items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
        if (config.allowsRemovalOfLastSavedPaymentMethod) {
            paymentMethods.isNotEmpty()
        } else {
            if (paymentMethods.size == 1) {
                // We will allow them to change card brand, but not delete.
                paymentMethods.first().isModifiable
            } else {
                paymentMethods.size > 1
            }
        }
    }

    val topBarState: StateFlow<PaymentSheetTopBarState> = combineAsStateFlow(
        navigationHandler.currentScreen.mapAsStateFlow { it.sheetScreen },
        navigationHandler.currentScreen.mapAsStateFlow { it.canNavigateBack },
        paymentMethodMetadata.mapAsStateFlow { it?.stripeIntent?.isLiveMode ?: true },
        processing,
        editing,
        canEdit,
        PaymentSheetTopBarStateFactory::create,
    )

    val linkSignupMode: StateFlow<LinkSignupMode?> = linkHandler.linkSignupMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val initiallySelectedPaymentMethodType: PaymentMethodCode
        get() = newPaymentSelection?.getPaymentMethodCode() ?: _supportedPaymentMethodsFlow.value.first()

    init {
        viewModelScope.launch {
            canEdit.collect { canEdit ->
                if (!canEdit && editing.value) {
                    toggleEditing()
                }
            }
        }

        viewModelScope.launch {
            paymentMethods.collect { paymentMethods ->
                if (paymentMethods.isEmpty() && editing.value) {
                    toggleEditing()
                }
            }
        }

        viewModelScope.launch {
            var inLinkSignUpMode = false

            combine(
                linkHandler.linkInlineSelection,
                selection,
                linkInlineSignUpState
            ).collectLatest { (linkInlineSelection, paymentSelection, linkInlineSignUpState) ->
                // Only reset custom primary button state if we haven't already
                if (paymentSelection !is PaymentSelection.New.Card) {
                    if (inLinkSignUpMode) {
                        // US bank account will update the custom primary state on its own
                        if (paymentSelection !is PaymentSelection.New.USBankAccount) {
                            updateLinkPrimaryButtonUiState(null)
                        }

                        inLinkSignUpMode = false
                    }

                    return@collectLatest
                }

                inLinkSignUpMode = true

                if (linkInlineSignUpState != null) {
                    updatePrimaryButtonForLinkSignup(linkInlineSignUpState)
                } else if (linkInlineSelection != null) {
                    updatePrimaryButtonForLinkInline()
                }
            }
        }
    }

    protected fun transitionToFirstScreen() {
        val initialBackStack = determineInitialBackStack()
        navigationHandler.resetTo(initialBackStack)
    }

    abstract fun determineInitialBackStack(): List<PaymentSheetScreen>

    fun transitionToAddPaymentScreen() {
        transitionTo(AddAnotherPaymentMethod(interactor = DefaultAddPaymentMethodInteractor(this)))
    }

    fun transitionTo(target: PaymentSheetScreen) {
        clearErrorMessages()
        navigationHandler.transitionTo(target)
    }

    protected fun setPaymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata?) {
        _paymentMethodMetadata.value = paymentMethodMetadata
        supportedPaymentMethods = paymentMethodMetadata?.sortedSupportedPaymentMethods() ?: emptyList()
    }

    abstract fun clearErrorMessages()

    fun updatePrimaryButtonForLinkSignup(viewState: InlineSignupViewState) {
        val uiState = primaryButtonUiState.value ?: return

        updateLinkPrimaryButtonUiState(
            if (viewState.useLink) {
                val userInput = viewState.userInput
                val paymentSelection = selection.value

                if (userInput != null && paymentSelection != null) {
                    PrimaryButton.UIState(
                        label = uiState.label,
                        onClick = { payWithLinkInline(userInput) },
                        enabled = true,
                        lockVisible = this is PaymentSheetViewModel,
                    )
                } else {
                    PrimaryButton.UIState(
                        label = uiState.label,
                        onClick = {},
                        enabled = false,
                        lockVisible = this is PaymentSheetViewModel,
                    )
                }
            } else {
                null
            }
        )
    }

    fun updatePrimaryButtonForLinkInline() {
        val uiState = primaryButtonUiState.value ?: return
        updateLinkPrimaryButtonUiState(
            PrimaryButton.UIState(
                label = uiState.label,
                onClick = { payWithLinkInline(userInput = null) },
                enabled = true,
                lockVisible = this is PaymentSheetViewModel,
            )
        )
    }

    private fun updateLinkPrimaryButtonUiState(state: PrimaryButton.UIState?) {
        customPrimaryButtonUiState.value = state
    }

    fun updateCustomPrimaryButtonUiState(block: (PrimaryButton.UIState?) -> PrimaryButton.UIState?) {
        customPrimaryButtonUiState.update(block)
    }

    fun updatePrimaryButtonState(state: PrimaryButton.State) {
        _primaryButtonState.value = state
    }

    fun updateMandateText(mandateText: String?, showAbove: Boolean) {
        _mandateText.value = if (mandateText != null) {
            MandateText(
                text = mandateText,
                showAbovePrimaryButton = showAbove || config.paymentMethodLayout == PaymentMethodLayout.Vertical
            )
        } else {
            null
        }
    }

    abstract fun handlePaymentMethodSelected(selection: PaymentSelection?)

    abstract fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount)

    fun updateSelection(selection: PaymentSelection?) {
        when (selection) {
            is PaymentSelection.New -> newPaymentSelection = NewOrExternalPaymentSelection.New(selection)
            is PaymentSelection.ExternalPaymentMethod ->
                newPaymentSelection = NewOrExternalPaymentSelection.External(selection)
            is PaymentSelection.Saved -> savedStateHandle[SAVED_PM_SELECTION] = selection.paymentMethod
            else -> Unit
        }

        savedStateHandle[SAVE_SELECTION] = selection

        val isRequestingReuse = if (selection is PaymentSelection.New) {
            selection.customerRequestedSave == RequestReuse
        } else {
            false
        }

        val mandateText = selection?.mandateText(
            context = getApplication(),
            merchantName = merchantName,
            isSaveForFutureUseSelected = isRequestingReuse,
            isSetupFlow = paymentMethodMetadata.value?.stripeIntent is SetupIntent,
        )

        val showAbove = (selection as? PaymentSelection.Saved?)
            ?.showMandateAbovePrimaryButton == true

        updateCvcFlows(selection)
        updateMandateText(mandateText = mandateText, showAbove = showAbove)
        clearErrorMessages()
    }

    fun toggleEditing() {
        _editing.value = !editing.value
    }

    fun setContentVisible(visible: Boolean) {
        _contentVisible.value = visible
    }

    fun removePaymentMethod(paymentMethod: PaymentMethod) {
        val paymentMethodId = paymentMethod.id ?: return

        viewModelScope.launch(workContext) {
            removeDeletedPaymentMethodFromState(paymentMethodId)
            removePaymentMethodInternal(paymentMethodId)
        }
    }

    private fun updateCvcFlows(selection: PaymentSelection?) {
        if (selection is PaymentSelection.Saved && selection.paymentMethod.type == PaymentMethod.Type.Card) {
            _cvcControllerFlow.value = CvcController(
                CvcConfig(),
                stateFlowOf(selection.paymentMethod.card?.brand ?: CardBrand.Unknown)
            )
            viewModelScope.launch {
                cvcControllerFlow.value.isComplete.collect {
                    _cvcRecollectionCompleteFlow.value = it
                }
            }
        }
    }

    private suspend fun removePaymentMethodInternal(paymentMethodId: String): Result<PaymentMethod> {
        // TODO(samer-stripe): Send 'unexpected_error' here
        val currentCustomer = customer ?: return Result.failure(
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
            updateSelection(null)
        }

        return customerRepository.detachPaymentMethod(
            CustomerRepository.CustomerInfo(
                id = currentCustomer.id,
                ephemeralKeySecret = currentCustomer.ephemeralKeySecret
            ),
            paymentMethodId
        )
    }

    private fun removeDeletedPaymentMethodFromState(paymentMethodId: String) {
        val currentCustomer = customer ?: return

        customer = currentCustomer.copy(
            paymentMethods = currentCustomer.paymentMethods.filter {
                it.id != paymentMethodId
            }
        )

        if (mostRecentlySelectedSavedPaymentMethod.value?.id == paymentMethodId) {
            savedStateHandle[SAVED_PM_SELECTION] = null
        }

        if ((selection.value as? PaymentSelection.Saved)?.paymentMethod?.id == paymentMethodId) {
            savedStateHandle[SAVE_SELECTION] = null
        }

        val shouldResetToAddPaymentMethodForm = paymentMethods.value.isEmpty() &&
            navigationHandler.currentScreen.value is PaymentSheetScreen.SelectSavedPaymentMethods

        if (shouldResetToAddPaymentMethodForm) {
            navigationHandler.resetTo(
                listOf(AddFirstPaymentMethod(interactor = DefaultAddPaymentMethodInteractor(this)))
            )
        }
    }

    fun modifyPaymentMethod(paymentMethod: PaymentMethod) {
        val canRemove = if (config.allowsRemovalOfLastSavedPaymentMethod) {
            true
        } else {
            paymentOptionsItems.value.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>().size > 1
        }

        transitionTo(
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
                    canRemove = canRemove,
                    coroutineScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob()),
                )
            )
        )
    }

    private suspend fun removePaymentMethodInEditScreen(paymentMethod: PaymentMethod): Throwable? {
        val paymentMethodId = paymentMethod.id!!
        val result = removePaymentMethodInternal(paymentMethodId)

        if (result.isSuccess) {
            viewModelScope.launch(workContext) {
                onUserBack()
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
        val currentCustomer = customer ?: return Result.failure(
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
            customer = currentCustomer.copy(
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

            handleBackPressed()

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

    private fun mapToHeaderTextResource(
        screen: PaymentSheetScreen?,
        walletsState: WalletsState?,
        supportedPaymentMethods: List<PaymentMethodCode>,
        editing: Boolean,
    ): Int? {
        return headerTextFactory.create(
            screen = screen,
            isWalletEnabled = walletsState != null,
            types = supportedPaymentMethods,
            isEditing = editing,
        )
    }

    abstract val shouldCompleteLinkFlowInline: Boolean

    private fun payWithLinkInline(userInput: UserInput?) {
        viewModelScope.launch(workContext) {
            linkHandler.payWithLinkInline(
                userInput = userInput,
                paymentSelection = selection.value,
                shouldCompleteLinkInlineFlow = shouldCompleteLinkFlowInline,
            )
        }
    }

    fun onLinkSignUpStateUpdated(state: InlineSignupViewState) {
        linkInlineSignUpState.value = state
    }

    private fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(
            paymentMethodMetadata.value?.supportedPaymentMethodForCode(
                code = code,
            )
        )
    }

    fun formElementsForCode(code: String): List<FormElement> {
        val currentSelection = newPaymentSelection?.takeIf { it.getType() == code }

        return paymentMethodMetadata.value?.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                paymentMethodCreateParams = currentSelection?.getPaymentMethodCreateParams(),
                paymentMethodExtraParams = currentSelection?.getPaymentMethodExtraParams(),
            ),
        ) ?: emptyList()
    }

    fun createFormArguments(
        paymentMethodCode: PaymentMethodCode,
    ): FormArguments {
        val metadata = requireNotNull(paymentMethodMetadata.value)
        return FormArgumentsFactory.create(
            paymentMethodCode = paymentMethodCode,
            metadata = metadata,
        )
    }

    fun handleBackPressed() {
        if (processing.value) {
            return
        }
        if (navigationHandler.canGoBack) {
            onUserBack()
        } else {
            onUserCancel()
        }
    }

    abstract fun onUserCancel()

    private fun onUserBack() {
        clearErrorMessages()
        _mandateText.value = null

        navigationHandler.pop { poppedScreen ->
            analyticsListener.reportPaymentSheetHidden(poppedScreen)
        }
    }

    fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        paymentMethodMetadata.value?.let { paymentMethodMetadata ->
            val newSelection = formValues?.transformToPaymentSelection(
                context = getApplication(),
                paymentMethod = supportedPaymentMethodForCode(selectedPaymentMethodCode),
                paymentMethodMetadata = paymentMethodMetadata,
            )
            updateSelection(newSelection)
        }
    }

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onFinish()

    abstract fun onError(@StringRes error: Int? = null)

    abstract fun onError(error: String? = null)

    data class UserErrorMessage(val message: String)

    internal sealed interface NewOrExternalPaymentSelection {

        val paymentSelection: PaymentSelection

        fun getPaymentMethodCode(): PaymentMethodCode

        fun getType(): String

        fun getPaymentMethodCreateParams(): PaymentMethodCreateParams?

        fun getPaymentMethodExtraParams(): PaymentMethodExtraParams?

        data class New(override val paymentSelection: PaymentSelection.New) : NewOrExternalPaymentSelection {

            override fun getPaymentMethodCode(): PaymentMethodCode {
                return when (paymentSelection) {
                    is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
                    is PaymentSelection.New.Card,
                    is PaymentSelection.New.USBankAccount,
                    is PaymentSelection.New.GenericPaymentMethod -> paymentSelection.paymentMethodCreateParams.typeCode
                }
            }

            override fun getType(): String = paymentSelection.paymentMethodCreateParams.typeCode

            override fun getPaymentMethodCreateParams(): PaymentMethodCreateParams =
                paymentSelection.paymentMethodCreateParams

            override fun getPaymentMethodExtraParams(): PaymentMethodExtraParams? =
                paymentSelection.paymentMethodExtraParams
        }

        data class External(override val paymentSelection: PaymentSelection.ExternalPaymentMethod) :
            NewOrExternalPaymentSelection {

            override fun getPaymentMethodCode(): PaymentMethodCode = paymentSelection.type

            override fun getType(): String = paymentSelection.type

            override fun getPaymentMethodCreateParams(): PaymentMethodCreateParams? = null

            override fun getPaymentMethodExtraParams(): PaymentMethodExtraParams? = null
        }
    }

    companion object {
        internal const val SAVED_CUSTOMER = "customer_info"
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVED_PM_SELECTION = "saved_selection"
        internal const val SAVE_PROCESSING = "processing"
        internal const val SAVE_GOOGLE_PAY_STATE = "google_pay_state"
    }
}
