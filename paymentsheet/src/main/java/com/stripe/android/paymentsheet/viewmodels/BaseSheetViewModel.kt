package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetViewModel
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave.RequestReuse
import com.stripe.android.paymentsheet.model.currency
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.paymentsheet.utils.combineStateFlows
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.forms.resources.LpmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
@Suppress("TooManyFunctions")
internal abstract class BaseSheetViewModel(
    application: Application,
    internal val config: PaymentSheet.Configuration?,
    internal val eventReporter: EventReporter,
    protected val customerRepository: CustomerRepository,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO,
    protected val logger: Logger,
    val lpmRepository: LpmRepository,
    val savedStateHandle: SavedStateHandle,
    val linkHandler: LinkHandler,
    val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val headerTextFactory: HeaderTextFactory,
    val formViewModelSubComponentBuilderProvider: Provider<FormViewModelSubcomponent.Builder>,
) : AndroidViewModel(application) {

    internal val customerConfig = config?.customer
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    protected var mostRecentError: Throwable? = null

    internal val googlePayState: StateFlow<GooglePayState> = savedStateHandle
        .getStateFlow(SAVE_GOOGLE_PAY_STATE, GooglePayState.Indeterminate)

    private val _stripeIntent = MutableStateFlow<StripeIntent?>(null)
    internal val stripeIntent: StateFlow<StripeIntent?> = _stripeIntent

    internal var supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod> = emptyList()
        set(value) {
            field = value
            _supportedPaymentMethodsFlow.tryEmit(value.map { it.code })
        }

    private val _supportedPaymentMethodsFlow =
        MutableStateFlow<List<PaymentMethodCode>>(emptyList())
    protected val supportedPaymentMethodsFlow: StateFlow<List<PaymentMethodCode>> =
        _supportedPaymentMethodsFlow

    /**
     * The list of saved payment methods for the current customer.
     * Value is null until it's loaded, and non-null (could be empty) after that.
     */
    internal val paymentMethods: StateFlow<List<PaymentMethod>?> = savedStateHandle
        .getStateFlow(SAVE_PAYMENT_METHODS, null)

    private val _amount = MutableStateFlow<Amount?>(null)
    internal val amount: StateFlow<Amount?> = _amount

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

    internal val headerText: Flow<Int?> = combine(
        currentScreen,
        linkHandler.isLinkEnabled.filterNotNull(),
        googlePayState,
        stripeIntent.filterNotNull(),
    ) { screen, isLinkAvailable, googlePay, intent ->
        mapToHeaderTextResource(screen, isLinkAvailable, googlePay, intent)
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

    private val _notesText = MutableStateFlow<String?>(null)
    internal val notesText: StateFlow<String?> = _notesText

    /**
     * This should be initialized from the starter args, and then from that point forward it will be
     * the last valid new payment method entered by the user.
     * In contrast to selection, this field will not be updated by the list fragment. It is used to
     * save a new payment method that is added so that the payment data entered is recovered when
     * the user returns to that payment method type.
     */
    abstract var newPaymentSelection: PaymentSelection.New?

    abstract fun onFatal(throwable: Throwable)

    protected val buttonsEnabled = combineStateFlows(
        processing,
        editing,
    ) { isProcessing, isEditing ->
        !isProcessing && !isEditing
    }

    internal var lpmServerSpec: String? = null

    private val paymentOptionsStateMapper: PaymentOptionsStateMapper by lazy {
        PaymentOptionsStateMapper(
            paymentMethods = paymentMethods,
            currentSelection = selection,
            googlePayState = googlePayState,
            isLinkEnabled = linkHandler.isLinkEnabled,
            isNotPaymentFlow = this is PaymentOptionsViewModel,
            nameProvider = { code ->
                val paymentMethod = lpmRepository.fromCode(code)
                paymentMethod?.displayNameResource?.let {
                    application.getString(it)
                }.orEmpty()
            }
        )
    }

    val paymentOptionsState: StateFlow<PaymentOptionsState> = paymentOptionsStateMapper()
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PaymentOptionsState(),
        )

    val topBarState: StateFlow<PaymentSheetTopBarState> = combine(
        currentScreen,
        paymentMethods,
        stripeIntent.map { it?.isLiveMode ?: true },
        processing,
        editing,
        PaymentSheetTopBarStateFactory::create,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = PaymentSheetTopBarStateFactory.createDefault(),
    )

    init {
        viewModelScope.launch {
            paymentMethods.onEach { paymentMethods ->
                if (paymentMethods.isNullOrEmpty() && editing.value) {
                    toggleEditing()
                }
            }.collect()
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

    abstract fun transitionToFirstScreen()

    protected fun transitionTo(target: PaymentSheetScreen) {
        clearErrorMessages()
        backStack.update { (it - PaymentSheetScreen.Loading) + target }
        reportNavigationEvent(target)
    }

    fun transitionToAddPaymentScreen() {
        transitionTo(AddAnotherPaymentMethod)
    }

    protected fun reportNavigationEvent(currentScreen: PaymentSheetScreen) {
        when (currentScreen) {
            PaymentSheetScreen.Loading -> {
                // Nothing to do here
            }
            PaymentSheetScreen.SelectSavedPaymentMethods -> {
                eventReporter.onShowExistingPaymentOptions(
                    linkEnabled = linkHandler.isLinkEnabled.value == true,
                    currency = stripeIntent.value?.currency,
                    isDecoupling = stripeIntent.value?.clientSecret == null,
                )
            }
            AddFirstPaymentMethod,
            AddAnotherPaymentMethod -> {
                eventReporter.onShowNewPaymentOptionForm(
                    linkEnabled = linkHandler.isLinkEnabled.value == true,
                    currency = stripeIntent.value?.currency,
                    isDecoupling = stripeIntent.value?.clientSecret == null,
                )
            }
        }
    }

    protected fun setStripeIntent(stripeIntent: StripeIntent?) {
        _stripeIntent.value = stripeIntent
        supportedPaymentMethods = getPMsToAdd(stripeIntent, config, lpmRepository)

        if (stripeIntent is PaymentIntent) {
            _amount.value = Amount(
                requireNotNull(stripeIntent.amount),
                requireNotNull(stripeIntent.currency)
            )
        }
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

    fun updateBelowButtonText(text: String?) {
        _notesText.value = text
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
        )

        updateBelowButtonText(mandateText)
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

        viewModelScope.launch {
            val currentSelection = (selection.value as? PaymentSelection.Saved)?.paymentMethod?.id
            val didRemoveSelectedItem = currentSelection == paymentMethodId

            if (didRemoveSelectedItem) {
                // Remove the current selection. The new selection will be set when we're computing
                // the next PaymentOptionsState.
                updateSelection(null)
            }

            savedStateHandle[SAVE_PAYMENT_METHODS] = paymentMethods.value?.filter {
                it.id != paymentMethodId
            }

            customerConfig?.let {
                customerRepository.detachPaymentMethod(
                    it,
                    paymentMethodId
                )
            }

            val shouldResetToAddPaymentMethodForm = paymentMethods.value.isNullOrEmpty() &&
                currentScreen.value is PaymentSheetScreen.SelectSavedPaymentMethods

            if (shouldResetToAddPaymentMethodForm) {
                backStack.value = listOf(AddFirstPaymentMethod)
            }
        }
    }

    private fun mapToHeaderTextResource(
        screen: PaymentSheetScreen?,
        isLinkAvailable: Boolean,
        googlePayState: GooglePayState,
        stripeIntent: StripeIntent,
    ): Int? {
        return if (screen != null) {
            headerTextFactory.create(
                screen = screen,
                isWalletEnabled = isLinkAvailable || googlePayState is GooglePayState.Available,
                types = stripeIntent.paymentMethodTypes,
            )
        } else {
            null
        }
    }

    abstract val shouldCompleteLinkFlowInline: Boolean

    private fun payWithLinkInline(userInput: UserInput?) {
        viewModelScope.launch {
            linkHandler.payWithLinkInline(
                userInput = userInput,
                paymentSelection = selection.value,
                shouldCompleteLinkInlineFlow = shouldCompleteLinkFlowInline,
            )
        }
    }

    fun createFormArguments(
        selectedItem: LpmRepository.SupportedPaymentMethod,
    ): FormArguments = FormArgumentsFactory.create(
        paymentMethod = selectedItem,
        stripeIntent = requireNotNull(stripeIntent.value),
        config = config,
        merchantName = merchantName,
        amount = amount.value,
        newLpm = newPaymentSelection,
    )

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
        backStack.update { it.dropLast(1) }

        // Reset the selection to the one from before opening the add payment method screen
        val paymentOptionsState = paymentOptionsState.value
        updateSelection(paymentOptionsState.selectedItem?.toPaymentSelection())
    }

    fun reportAutofillEvent(type: String) {
        eventReporter.onAutofill(type, isDecoupling = stripeIntent.value?.clientSecret == null)
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
    }
}
