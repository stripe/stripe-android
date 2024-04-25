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
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave.RequestReuse
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.paymentsheet.ui.EditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentMethodRemovalDelayMillis
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.Closeable
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

    internal val customerConfig = config.customer
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

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: StateFlow<List<PaymentMethod>?> = savedStateHandle
        .getStateFlow(SAVE_PAYMENT_METHODS, null)

    protected val backStack = MutableStateFlow<List<PaymentSheetScreen>>(
        value = listOf(PaymentSheetScreen.Loading),
    )

    val currentScreen: StateFlow<PaymentSheetScreen> = backStack
        .map { it.last() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PaymentSheetScreen.Loading,
        )

    abstract val walletsState: StateFlow<WalletsState?>
    abstract val walletsProcessingState: StateFlow<WalletsProcessingState?>

    internal val headerText: StateFlow<Int?> by lazy {
        combineAsStateFlow(
            currentScreen,
            walletsState,
            supportedPaymentMethodsFlow,
        ) { screen, walletsState, supportedPaymentMethods ->
            mapToHeaderTextResource(screen, walletsState, supportedPaymentMethods)
        }
    }

    internal val selection: StateFlow<PaymentSelection?> = savedStateHandle
        .getStateFlow<PaymentSelection?>(SAVE_SELECTION, null)

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

    protected val linkEmailFlow: StateFlow<String?> = linkConfigurationCoordinator.emailFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = null,
    )

    private var previouslySentDeepLinkEvent: Boolean
        get() = savedStateHandle[PREVIOUSLY_SENT_DEEP_LINK_EVENT] ?: false
        set(value) {
            savedStateHandle[PREVIOUSLY_SENT_DEEP_LINK_EVENT] = value
        }

    private var previouslyShownForm: PaymentMethodCode?
        get() = savedStateHandle[PREVIOUSLY_SHOWN_PAYMENT_FORM]
        set(value) {
            savedStateHandle[PREVIOUSLY_SHOWN_PAYMENT_FORM] = value
        }

    private var previouslyInteractedForm: PaymentMethodCode?
        get() = savedStateHandle[PREVIOUSLY_INTERACTION_PAYMENT_FORM]
        set(value) {
            savedStateHandle[PREVIOUSLY_INTERACTION_PAYMENT_FORM] = value
        }

    /**
     * This should be initialized from the starter args, and then from that point forward it will be
     * the last valid new payment method entered by the user.
     * In contrast to selection, this field will not be updated by the list fragment. It is used to
     * save a new payment method that is added so that the payment data entered is recovered when
     * the user returns to that payment method type.
     */
    abstract var newPaymentSelection: PaymentSelection.New?

    abstract fun onFatal(throwable: Throwable)

    protected val buttonsEnabled = combineAsStateFlow(
        processing,
        editing,
    ) { isProcessing, isEditing ->
        !isProcessing && !isEditing
    }

    private val paymentOptionsStateMapper: PaymentOptionsStateMapper by lazy {
        PaymentOptionsStateMapper(
            paymentMethods = paymentMethods,
            currentSelection = selection,
            googlePayState = googlePayState,
            isLinkEnabled = linkHandler.isLinkEnabled,
            isNotPaymentFlow = this is PaymentOptionsViewModel,
            nameProvider = ::providePaymentMethodName,
            isCbcEligible = { paymentMethodMetadata.value?.cbcEligibility is CardBrandChoiceEligibility.Eligible }
        )
    }

    private fun providePaymentMethodName(code: PaymentMethodCode?): String {
        return code?.let {
            paymentMethodMetadata.value?.supportedPaymentMethodForCode(code)
        }?.displayName?.resolve(getApplication()).orEmpty()
    }

    val paymentOptionsState: StateFlow<PaymentOptionsState> = paymentOptionsStateMapper()
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PaymentOptionsState(),
        )

    private val canEdit: StateFlow<Boolean> = paymentOptionsState.mapAsStateFlow { state ->
        val paymentMethods = state.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>()
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
        currentScreen,
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
        get() = when (val selection = newPaymentSelection) {
            is PaymentSelection.New.LinkInline -> PaymentMethod.Type.Card.code
            is PaymentSelection.New.Card,
            is PaymentSelection.New.USBankAccount,
            is PaymentSelection.New.GenericPaymentMethod -> selection.paymentMethodCreateParams.typeCode
            else -> _supportedPaymentMethodsFlow.value.first()
        }

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
                if (paymentMethods.isNullOrEmpty() && editing.value) {
                    toggleEditing()
                }
            }
        }

        viewModelScope.launch {
            currentScreen.collectLatest { screen ->
                when (screen) {
                    is AddFirstPaymentMethod, AddAnotherPaymentMethod -> {
                        reportFormShown(initiallySelectedPaymentMethodType)
                    }
                    is PaymentSheetScreen.EditPaymentMethod,
                    is PaymentSheetScreen.Loading,
                    is PaymentSheetScreen.SelectSavedPaymentMethods -> {
                        previouslyShownForm = null
                        previouslyInteractedForm = null
                    }
                }
            }
        }

        viewModelScope.launch {
            // If the currently selected payment option has been removed, we set it to the one
            // determined in the payment options state.
            paymentOptionsState
                .mapNotNull {
                    it.selectedItem?.toPaymentSelection()
                }
                .filter {
                    it != selection.value
                }
                .collect { updateSelection(it) }
        }
    }

    protected fun transitionToFirstScreen() {
        val initialBackStack = determineInitialBackStack()
        resetTo(initialBackStack)
        reportPaymentSheetShown(initialBackStack.last())
    }

    abstract fun determineInitialBackStack(): List<PaymentSheetScreen>

    fun transitionToAddPaymentScreen() {
        transitionTo(AddAnotherPaymentMethod)
    }

    private fun transitionTo(target: PaymentSheetScreen) {
        clearErrorMessages()
        backStack.update { (it - PaymentSheetScreen.Loading) + target }
    }

    private fun reportPaymentSheetShown(currentScreen: PaymentSheetScreen) {
        when (currentScreen) {
            is PaymentSheetScreen.Loading, is PaymentSheetScreen.EditPaymentMethod -> {
                // Nothing to do here
            }
            is PaymentSheetScreen.SelectSavedPaymentMethods -> {
                eventReporter.onShowExistingPaymentOptions()
            }
            is AddFirstPaymentMethod, is AddAnotherPaymentMethod -> {
                eventReporter.onShowNewPaymentOptionForm()
            }
        }
    }

    private fun reportPaymentSheetHidden(hiddenScreen: PaymentSheetScreen) {
        when (hiddenScreen) {
            is PaymentSheetScreen.EditPaymentMethod -> {
                eventReporter.onHideEditablePaymentOption()
            }
            else -> {
                // Events for hiding other screens not supported
            }
        }
    }

    protected fun reportConfirmButtonPressed() {
        eventReporter.onPressConfirmButton(selection.value)
    }

    protected fun setPaymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata?) {
        _paymentMethodMetadata.value = paymentMethodMetadata
        supportedPaymentMethods = paymentMethodMetadata?.sortedSupportedPaymentMethods() ?: emptyList()
    }

    protected fun reportDismiss() {
        eventReporter.onDismiss()
    }

    fun reportPaymentMethodTypeSelected(code: PaymentMethodCode) {
        eventReporter.onSelectPaymentMethod(code)
        reportFormShown(code)
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

    fun resetUSBankPrimaryButton() {
        customPrimaryButtonUiState.value = null
    }

    fun updatePrimaryButtonState(state: PrimaryButton.State) {
        _primaryButtonState.value = state
    }

    fun updateMandateText(mandateText: String?, showAbove: Boolean) {
        _mandateText.value = if (mandateText != null) MandateText(mandateText, showAbove) else null
    }

    abstract fun handlePaymentMethodSelected(selection: PaymentSelection?)

    abstract fun handleConfirmUSBankAccount(paymentSelection: PaymentSelection.New.USBankAccount)

    fun updateSelection(selection: PaymentSelection?) {
        if (selection is PaymentSelection.New) {
            newPaymentSelection = selection
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

    fun cannotProperlyReturnFromLinkAndOtherLPMs() {
        if (!previouslySentDeepLinkEvent) {
            eventReporter.onCannotProperlyReturnFromLinkAndOtherLPMs()

            previouslySentDeepLinkEvent = true
        }
    }

    private suspend fun removePaymentMethodInternal(paymentMethodId: String): Result<PaymentMethod> {
        if (customerConfig == null) {
            // TODO(samer-stripe): Send 'unexpected_error' here
            return Result.failure(
                IllegalStateException(
                    "Could not remove payment method because CustomerConfiguration was not found! Make sure it is " +
                        "provided as part of PaymentSheet.Configuration"
                )
            )
        }

        val currentSelection = (selection.value as? PaymentSelection.Saved)?.paymentMethod?.id
        val didRemoveSelectedItem = currentSelection == paymentMethodId

        if (didRemoveSelectedItem) {
            // Remove the current selection. The new selection will be set when we're computing
            // the next PaymentOptionsState.
            updateSelection(null)
        }

        return customerRepository.detachPaymentMethod(
            CustomerRepository.CustomerInfo(
                id = customerConfig.id,
                ephemeralKeySecret = customerConfig.ephemeralKeySecret
            ),
            paymentMethodId
        )
    }

    private fun removeDeletedPaymentMethodFromState(paymentMethodId: String) {
        savedStateHandle[SAVE_PAYMENT_METHODS] = paymentMethods.value?.filter {
            it.id != paymentMethodId
        }

        val shouldResetToAddPaymentMethodForm = paymentMethods.value.isNullOrEmpty() &&
            currentScreen.value is PaymentSheetScreen.SelectSavedPaymentMethods

        if (shouldResetToAddPaymentMethodForm) {
            resetTo(listOf(AddFirstPaymentMethod))
        }
    }

    fun modifyPaymentMethod(paymentMethod: PaymentMethod) {
        eventReporter.onShowEditablePaymentOption()

        val canRemove = if (config.allowsRemovalOfLastSavedPaymentMethod) {
            true
        } else {
            paymentOptionsState.value.items.filterIsInstance<PaymentOptionsItem.SavedPaymentMethod>().size > 1
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
        if (customerConfig == null) {
            // TODO(samer-stripe): Send 'unexpected_error' here
            return Result.failure(
                IllegalStateException(
                    "Could not update payment method because CustomerConfiguration was not found! Make sure it is " +
                        "provided as part of PaymentSheet.Configuration"
                )
            )
        }

        return customerRepository.updatePaymentMethod(
            customerInfo = CustomerRepository.CustomerInfo(
                id = customerConfig.id,
                ephemeralKeySecret = customerConfig.ephemeralKeySecret
            ),
            paymentMethodId = paymentMethod.id!!,
            params = PaymentMethodUpdateParams.createCard(
                networks = PaymentMethodUpdateParams.Card.Networks(
                    preferred = brand.code
                ),
                productUsageTokens = setOf("PaymentSheet"),
            )
        ).onSuccess { updatedMethod ->
            savedStateHandle[SAVE_PAYMENT_METHODS] = paymentMethods
                .value
                ?.let { savedPaymentMethods ->
                    savedPaymentMethods.map { savedMethod ->
                        val savedId = savedMethod.id
                        val updatedId = updatedMethod.id

                        if (updatedId != null && savedId != null && updatedId == savedId) {
                            updatedMethod
                        } else {
                            savedMethod
                        }
                    }
                }

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
    ): Int? {
        return headerTextFactory.create(
            screen = screen,
            isWalletEnabled = walletsState != null,
            types = supportedPaymentMethods,
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

    fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(
            paymentMethodMetadata.value?.supportedPaymentMethodForCode(
                code = code,
            )
        )
    }

    fun formElementsForCode(code: String): List<FormElement> {
        val currentSelection = newPaymentSelection?.takeIf { it.paymentMethodCreateParams.typeCode == code }

        return paymentMethodMetadata.value?.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                paymentMethodCreateParams = currentSelection?.paymentMethodCreateParams,
                paymentMethodExtraParams = currentSelection?.paymentMethodExtraParams,
            ),
        ) ?: emptyList()
    }

    fun createFormArguments(
        selectedItem: SupportedPaymentMethod,
    ): FormArguments {
        val metadata = requireNotNull(paymentMethodMetadata.value)
        return FormArgumentsFactory.create(
            paymentMethod = selectedItem,
            metadata = metadata,
        )
    }

    fun handleBackPressed() {
        if (processing.value) {
            return
        }
        if (backStack.value.size > 1) {
            onUserBack()
        } else {
            onUserCancel()
        }
    }

    abstract fun onUserCancel()

    private fun onUserBack() {
        clearErrorMessages()
        backStack.update { screens ->
            val modifiableScreens = screens.toMutableList()

            val lastScreen = modifiableScreens.removeLast()

            lastScreen.onClose()

            reportPaymentSheetHidden(lastScreen)

            modifiableScreens.toList()
        }

        // Reset the selection to the one from before opening the add payment method screen
        val paymentOptionsState = paymentOptionsState.value
        updateSelection(paymentOptionsState.selectedItem?.toPaymentSelection())
    }

    private fun resetTo(screens: List<PaymentSheetScreen>) {
        val previousBackStack = backStack.value

        backStack.value = screens

        previousBackStack.forEach { oldScreen ->
            if (oldScreen !in screens) {
                oldScreen.onClose()
            }
        }
    }

    private fun PaymentSheetScreen.onClose() {
        when (this) {
            is Closeable -> close()
            else -> Unit
        }
    }

    fun reportFieldInteraction(code: PaymentMethodCode) {
        /*
         * Prevents this event from being reported multiple times on field interactions
         * on the same payment form. We should have one field interaction event for
         * every form shown event triggered.
         */
        if (previouslyInteractedForm != code) {
            eventReporter.onPaymentMethodFormInteraction(code)
            previouslyInteractedForm = code
        }
    }

    fun reportAutofillEvent(type: String) {
        eventReporter.onAutofill(type)
    }

    fun reportCardNumberCompleted() {
        eventReporter.onCardNumberCompleted()
    }

    private fun reportFormShown(code: String) {
        /*
         * Prevents this event from being reported multiple times on the same payment form after process death. We
         * should only trigger a form shown event when initially shown in the add payment method screen or the user
         * navigates to a different form.
         */
        if (previouslyShownForm != code) {
            eventReporter.onPaymentMethodFormShown(code)
            previouslyShownForm = code
        }
    }

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onFinish()

    abstract fun onError(@StringRes error: Int? = null)

    abstract fun onError(error: String? = null)

    data class UserErrorMessage(val message: String)

    companion object {
        internal const val SAVE_PAYMENT_METHODS = "customer_payment_methods"
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVE_PROCESSING = "processing"
        internal const val SAVE_GOOGLE_PAY_STATE = "google_pay_state"
        internal const val PREVIOUSLY_SHOWN_PAYMENT_FORM = "previously_shown_payment_form"
        internal const val PREVIOUSLY_INTERACTION_PAYMENT_FORM = "previously_interacted_payment_form"
        internal const val PREVIOUSLY_SENT_DEEP_LINK_EVENT = "previously_sent_deep_link_event"
    }
}
