package com.stripe.android.paymentsheet.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.SetupIntent
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheet.PaymentMethodLayout
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetAnalyticsListener
import com.stripe.android.paymentsheet.model.MandateText
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.CustomerRequestedSave.RequestReuse
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen.AddAnotherPaymentMethod
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.DefaultAddPaymentMethodInteractor
import com.stripe.android.paymentsheet.ui.HeaderTextFactory
import com.stripe.android.paymentsheet.ui.ModifiableEditPaymentMethodViewInteractor
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarState
import com.stripe.android.paymentsheet.ui.PaymentSheetTopBarStateFactory
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Base `ViewModel` for activities that use `BottomSheet`.
 */
@Suppress("TooManyFunctions")
internal abstract class BaseSheetViewModel(
    application: Application,
    val config: PaymentSheet.Configuration,
    val eventReporter: EventReporter,
    val customerRepository: CustomerRepository,
    val workContext: CoroutineContext = Dispatchers.IO,
    val savedStateHandle: SavedStateHandle,
    val linkHandler: LinkHandler,
    val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val headerTextFactory: HeaderTextFactory,
    val editInteractorFactory: ModifiableEditPaymentMethodViewInteractor.Factory
) : AndroidViewModel(application) {

    protected var mostRecentError: Throwable? = null

    internal val googlePayState: StateFlow<GooglePayState> = savedStateHandle
        .getStateFlow(SAVE_GOOGLE_PAY_STATE, GooglePayState.Indeterminate)

    private val _paymentMethodMetadata = MutableStateFlow<PaymentMethodMetadata?>(null)
    internal val paymentMethodMetadata: StateFlow<PaymentMethodMetadata?> = _paymentMethodMetadata

    private val _supportedPaymentMethodsFlow = MutableStateFlow<List<PaymentMethodCode>>(emptyList())
    val supportedPaymentMethodsFlow: StateFlow<List<PaymentMethodCode>> = _supportedPaymentMethodsFlow

    val navigationHandler: NavigationHandler = NavigationHandler { poppedScreen ->
        analyticsListener.reportPaymentSheetHidden(poppedScreen)
    }

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

    val customPrimaryButtonUiState = MutableStateFlow<PrimaryButton.UIState?>(null)

    abstract val primaryButtonUiState: StateFlow<PrimaryButton.UIState?>
    abstract val error: StateFlow<String?>

    private val _mandateText = MutableStateFlow<MandateText?>(null)
    internal val mandateText: StateFlow<MandateText?> = _mandateText

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

    protected val buttonsEnabled = combineAsStateFlow(
        processing,
        editing,
    ) { isProcessing, isEditing ->
        !isProcessing && !isEditing
    }

    val savedPaymentMethodMutator: SavedPaymentMethodMutator = SavedPaymentMethodMutator.create(this)

    val topBarState: StateFlow<PaymentSheetTopBarState> = combineAsStateFlow(
        navigationHandler.currentScreen.mapAsStateFlow { it.sheetScreen },
        navigationHandler.currentScreen.mapAsStateFlow { it.canNavigateBack },
        paymentMethodMetadata.mapAsStateFlow { it?.stripeIntent?.isLiveMode ?: true },
        processing,
        editing,
        savedPaymentMethodMutator.canEdit,
        PaymentSheetTopBarStateFactory::create,
    )

    val initiallySelectedPaymentMethodType: PaymentMethodCode
        get() = newPaymentSelection?.getPaymentMethodCode() ?: supportedPaymentMethodsFlow.value.first()

    init {
        viewModelScope.launch {
            savedPaymentMethodMutator.canEdit.collect { canEdit ->
                if (!canEdit && editing.value) {
                    toggleEditing()
                }
            }
        }

        viewModelScope.launch {
            savedPaymentMethodMutator.paymentMethods.collect { paymentMethods ->
                if (paymentMethods.isEmpty() && editing.value) {
                    toggleEditing()
                }
            }
        }

        viewModelScope.launch {
            // Drop the first item, since we don't need to clear errors/mandates when there aren't any.
            navigationHandler.currentScreen.drop(1).collect {
                clearErrorMessages()
                _mandateText.value = null
            }
        }
    }

    internal fun providePaymentMethodName(code: PaymentMethodCode?): String {
        return code?.let {
            paymentMethodMetadata.value?.supportedPaymentMethodForCode(code)
        }?.displayName?.resolve(getApplication()).orEmpty()
    }

    protected fun transitionToFirstScreen() {
        val initialBackStack = determineInitialBackStack()
        navigationHandler.resetTo(initialBackStack)
    }

    abstract fun determineInitialBackStack(): List<PaymentSheetScreen>

    fun transitionToAddPaymentScreen() {
        navigationHandler.transitionTo(
            AddAnotherPaymentMethod(interactor = DefaultAddPaymentMethodInteractor.create(this))
        )
    }

    protected fun setPaymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata?) {
        _paymentMethodMetadata.value = paymentMethodMetadata
        _supportedPaymentMethodsFlow.value = paymentMethodMetadata?.supportedPaymentMethodTypes() ?: emptyList()
    }

    abstract fun clearErrorMessages()

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
            merchantName = config.merchantDisplayName,
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

    fun handleBackPressed() {
        if (processing.value) {
            return
        }
        if (navigationHandler.canGoBack) {
            navigationHandler.pop()
        } else {
            onUserCancel()
        }
    }

    abstract fun onUserCancel()

    abstract fun onPaymentResult(paymentResult: PaymentResult)

    abstract fun onError(error: String? = null)

    companion object {
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVED_PM_SELECTION = "saved_selection"
        internal const val SAVE_PROCESSING = "processing"
        internal const val SAVE_GOOGLE_PAY_STATE = "google_pay_state"
    }
}
