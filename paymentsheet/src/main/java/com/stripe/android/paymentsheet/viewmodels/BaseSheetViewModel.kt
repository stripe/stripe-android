package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.NonFallbackInjector
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.ui.inline.UserInput
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.PaymentOptionsActivity
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetActivity
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.model.getPMsToAdd
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddFirstPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.PaymentSheetData
import com.stripe.android.paymentsheet.toPaymentSelection
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.PaymentSheetMode
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.ui.core.forms.resources.ResourceRepository
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
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
    internal val config: PaymentSheet.Configuration?,
    internal val eventReporter: EventReporter,
    protected val customerRepository: CustomerRepository,
    protected val prefsRepository: PrefsRepository,
    protected val workContext: CoroutineContext = Dispatchers.IO,
    protected val logger: Logger,
    @InjectorKey val injectorKey: String,
    val lpmResourceRepository: ResourceRepository<LpmRepository>,
    val addressResourceRepository: ResourceRepository<AddressRepository>,
    val savedStateHandle: SavedStateHandle,
    val linkHandler: LinkHandler,
    private val headerTextFactory: HeaderTextFactory,
) : AndroidViewModel(application) {
    /**
     * This ViewModel exists during the whole user flow, and needs to share the Dagger dependencies
     * with the other, screen-specific ViewModels. So it holds a reference to the injector which is
     * passed as a parameter to the other ViewModel factories.
     */
    lateinit var injector: NonFallbackInjector

    internal val customerConfig = config?.customer
    internal val merchantName = config?.merchantDisplayName
        ?: application.applicationInfo.loadLabel(application.packageManager).toString()

    protected var mostRecentError: Throwable? = null

    internal val googlePayState: StateFlow<GooglePayState> = savedStateHandle
        .getStateFlow(SAVE_GOOGLE_PAY_STATE, GooglePayState.Indeterminate)

    // Don't save the resource repository state because it must be re-initialized
    // with the save server specs when reconstructed.
    private val _isResourceRepositoryReady = MutableStateFlow(false)
    internal val isResourceRepositoryReady: StateFlow<Boolean> = _isResourceRepositoryReady

    private val _paymentSheetData = MutableStateFlow<PaymentSheetData?>(null)
    internal val paymentSheetData: StateFlow<PaymentSheetData?> = _paymentSheetData

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

    /**
     * Request to retrieve the value from the repository happens when initialize any fragment
     * and any fragment will re-update when the result comes back.
     * Represents what the user last selects (add or buy) on the
     * [PaymentOptionsActivity]/[PaymentSheetActivity], and saved/restored from the preferences.
     */
    private val savedSelection: StateFlow<SavedSelection?> = savedStateHandle
        .getStateFlow<SavedSelection?>(SAVE_SAVED_SELECTION, null)

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
        paymentSheetData.filterNotNull(),
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

    /**
     * Use this to override the current UI state of the primary button. The UI state is reset every
     * time the payment selection is changed.
     */
    private val _primaryButtonUIState = MutableStateFlow<PrimaryButton.UIState?>(null)
    val primaryButtonUIState: StateFlow<PrimaryButton.UIState?> = _primaryButtonUIState

    private val _primaryButtonState = MutableStateFlow<PrimaryButton.State?>(null)
    val primaryButtonState: StateFlow<PrimaryButton.State?> = _primaryButtonState

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

    val buttonsEnabled = combine(
        processing,
        editing
    ) { isProcessing, isEditing ->
        !isProcessing && !isEditing
    }.distinctUntilChanged()

    val isPrimaryButtonEnabled = combine(
        primaryButtonUIState,
        buttonsEnabled,
        selection,
    ) { uiState, buttonsEnabled, selection ->
        if (uiState != null) {
            uiState.enabled && buttonsEnabled
        } else {
            buttonsEnabled && selection != null
        }
    }.distinctUntilChanged()

    internal var lpmServerSpec: String? = null

    private val paymentOptionsStateMapper: PaymentOptionsStateMapper by lazy {
        PaymentOptionsStateMapper(
            paymentMethods = paymentMethods,
            currentSelection = selection,
            googlePayState = googlePayState,
            isLinkEnabled = linkHandler.isLinkEnabled,
            initialSelection = savedSelection,
            isNotPaymentFlow = this is PaymentOptionsViewModel,
            nameProvider = { code ->
                val paymentMethod = lpmResourceRepository.getRepository().fromCode(code)
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

    init {
        viewModelScope.launch {
            paymentMethods.onEach { paymentMethods ->
                if (paymentMethods.isNullOrEmpty() && editing.value) {
                    toggleEditing()
                }
            }.collect()
        }

        if (!_isResourceRepositoryReady.value) {
            viewModelScope.launch {
                // This work should be done on the background
                CoroutineScope(workContext).launch {
                    // If we have been killed and are being restored then we need to re-populate
                    // the lpm repository
                    paymentSheetData.value?.let { data ->
                        lpmResourceRepository.getRepository().apply {
                            val params = LpmRepository.UpdateParams(
                                mode = data.mode,
                                setupFutureUse = data.setupFutureUse,
                                expectedLpms = data.paymentMethodTypes,
                                serverLpmSpecs = lpmServerSpec,
                            )
                            update(params)
                        }
                    }

                    lpmResourceRepository.waitUntilLoaded()
                    addressResourceRepository.waitUntilLoaded()
                    _isResourceRepositoryReady.value = true
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

    protected fun transitionToFirstScreenWhenReady() {
        viewModelScope.launch {
            awaitRepositoriesReady()
            transitionToFirstScreen()
        }
    }

    private suspend fun awaitRepositoriesReady() {
        isResourceRepositoryReady.filter { it }.first()
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
                    activeLinkSession = linkHandler.activeLinkSession.value,
                    currency = paymentSheetData.value?.currency
                )
            }
            AddFirstPaymentMethod,
            AddAnotherPaymentMethod -> {
                eventReporter.onShowNewPaymentOptionForm(
                    linkEnabled = linkHandler.isLinkEnabled.value == true,
                    activeLinkSession = linkHandler.activeLinkSession.value,
                    currency = paymentSheetData.value?.currency
                )
            }
        }
    }

    protected fun setPaymentSheetData(data: PaymentSheetData?) {
        _paymentSheetData.value = data

        if (data != null) {
            val pmsToAdd = getPMsToAdd(data, config, lpmResourceRepository.getRepository())
            supportedPaymentMethods = pmsToAdd
        }

        if (data != null && supportedPaymentMethods.isEmpty()) {
            onFatal(
                IllegalArgumentException(
                    "None of the requested payment methods" +
                        " (${data.paymentMethodTypes})" +
                        " match the supported payment types" +
                        " (${
                        lpmResourceRepository.getRepository().values()
                            .map { it.code }.toList()
                        })"
                )
            )
        }

        val mode = data?.mode

        if (mode is PaymentSheetMode.Payment) {
            runCatching {
                _amount.value = Amount(
                    requireNotNull(mode.amount),
                    requireNotNull(mode.currency)
                )
                // Reset the primary button state to display the amount
                _primaryButtonUIState.value = null
            }.onFailure {
                onFatal(
                    IllegalStateException("PaymentIntent must contain amount and currency.")
                )
            }
        }

        if (data != null) {
            warnUnactivatedIfNeeded(data.unactivatedPaymentMethods)
        }
    }

    abstract fun clearErrorMessages()

    private fun warnUnactivatedIfNeeded(unactivatedPaymentMethodTypes: List<String>) {
        if (unactivatedPaymentMethodTypes.isEmpty()) {
            return
        }

        val message = "[Stripe SDK] Warning: Your Intent contains the following payment method " +
            "types which are activated for test mode but not activated for " +
            "live mode: $unactivatedPaymentMethodTypes. These payment method types will not be " +
            "displayed in live mode until they are activated. To activate these payment method " +
            "types visit your Stripe dashboard." +
            "More information: https://support.stripe.com/questions/activate-a-new-payment-method"

        logger.warning(message)
    }

    fun updatePrimaryButtonUIState(state: PrimaryButton.UIState?) {
        _primaryButtonUIState.value = state
    }

    fun updatePrimaryButtonState(state: PrimaryButton.State) {
        _primaryButtonState.value = state
    }

    fun updateBelowButtonText(text: String?) {
        _notesText.value = text
    }

    abstract fun handlePaymentMethodSelected(selection: PaymentSelection?)

    open fun updateSelection(selection: PaymentSelection?) {
        if (selection is PaymentSelection.New) {
            newPaymentSelection = selection
        }

        savedStateHandle[SAVE_SELECTION] = selection

        updateBelowButtonText(null)
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
                savedStateHandle[SAVE_SELECTION] = null
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

            val hasNoBankAccounts = paymentMethods.value.orEmpty().all { it.type != USBankAccount }
            if (hasNoBankAccounts) {
                updatePrimaryButtonUIState(
                    primaryButtonUIState.value?.copy(
                        visible = false
                    )
                )
                updateBelowButtonText(null)
            }
        }
    }

    private fun mapToHeaderTextResource(
        screen: PaymentSheetScreen?,
        isLinkAvailable: Boolean,
        googlePayState: GooglePayState,
        data: PaymentSheetData,
    ): Int? {
        return if (screen != null) {
            headerTextFactory.create(
                screen = screen,
                isWalletEnabled = isLinkAvailable || googlePayState is GooglePayState.Available,
                isPaymentIntent = data.mode is PaymentSheetMode.Payment,
                types = data.paymentMethodTypes,
            )
        } else {
            null
        }
    }

    abstract val shouldCompleteLinkFlowInline: Boolean

    fun payWithLinkInline(linkConfig: LinkPaymentLauncher.Configuration, userInput: UserInput?) {
        viewModelScope.launch {
            linkHandler.payWithLinkInline(
                configuration = linkConfig,
                userInput = userInput,
                paymentSelection = selection.value,
                shouldCompleteLinkInlineFlow = shouldCompleteLinkFlowInline,
            )
        }
    }

    fun createFormArguments(
        selectedItem: LpmRepository.SupportedPaymentMethod,
        showLinkInlineSignup: Boolean
    ): FormArguments = FormArgumentsFactory.create(
        paymentMethod = selectedItem,
        data = requireNotNull(paymentSheetData.value),
        config = config,
        merchantName = merchantName,
        amount = amount.value,
        newLpm = newPaymentSelection,
        isShowingLinkInlineSignup = showLinkInlineSignup
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

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onFinish()

    abstract fun onError(@StringRes error: Int? = null)

    abstract fun onError(error: String? = null)

    data class UserErrorMessage(val message: String)

    companion object {
        internal const val SAVE_PAYMENT_METHODS = "customer_payment_methods"
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVE_SAVED_SELECTION = "saved_selection"
        internal const val SAVE_PROCESSING = "processing"
        internal const val SAVE_GOOGLE_PAY_STATE = "google_pay_state"
    }
}
