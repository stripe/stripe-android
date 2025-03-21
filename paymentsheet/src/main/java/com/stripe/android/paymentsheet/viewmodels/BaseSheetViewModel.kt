package com.stripe.android.paymentsheet.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.MandateHandler
import com.stripe.android.paymentsheet.NewOrExternalPaymentSelection
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.SavedPaymentMethodMutator
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.analytics.PaymentSheetAnalyticsListener
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.WalletsProcessingState
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.ui.PrimaryButton
import com.stripe.android.ui.core.elements.CvcConfig
import com.stripe.android.ui.core.elements.CvcController
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
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
    val config: PaymentSheet.Configuration,
    val eventReporter: EventReporter,
    val customerRepository: CustomerRepository,
    val workContext: CoroutineContext = Dispatchers.IO,
    val savedStateHandle: SavedStateHandle,
    val linkHandler: LinkHandler,
    val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    val isCompleteFlow: Boolean,
) : ViewModel() {
    private val _paymentMethodMetadata = MutableStateFlow<PaymentMethodMetadata?>(null)
    internal val paymentMethodMetadata: StateFlow<PaymentMethodMetadata?> = _paymentMethodMetadata

    val navigationHandler: NavigationHandler<PaymentSheetScreen> = NavigationHandler(
        coroutineScope = viewModelScope,
        initialScreen = PaymentSheetScreen.Loading,
    ) { poppedScreen ->
        analyticsListener.reportPaymentSheetHidden(poppedScreen)
    }

    abstract val walletsState: StateFlow<WalletsState?>
    abstract val walletsProcessingState: StateFlow<WalletsProcessingState?>

    internal val selection: StateFlow<PaymentSelection?> = savedStateHandle
        .getStateFlow<PaymentSelection?>(SAVE_SELECTION, null)

    val processing: StateFlow<Boolean> = savedStateHandle
        .getStateFlow(SAVE_PROCESSING, false)

    private val _primaryButtonState = MutableStateFlow<PrimaryButton.State?>(null)
    val primaryButtonState: StateFlow<PrimaryButton.State?> = _primaryButtonState

    val customPrimaryButtonUiState = MutableStateFlow<PrimaryButton.UIState?>(null)

    abstract val primaryButtonUiState: StateFlow<PrimaryButton.UIState?>
    abstract val error: StateFlow<ResolvableString?>

    val mandateHandler = MandateHandler.create(this)

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

    val customerStateHolder: CustomerStateHolder = CustomerStateHolder.create(this)
    val savedPaymentMethodMutator: SavedPaymentMethodMutator = SavedPaymentMethodMutator.create(this)

    protected val buttonsEnabled = combineAsStateFlow(
        processing,
        navigationHandler.currentScreen.flatMapLatestAsStateFlow { currentScreen ->
            currentScreen.topBarState().mapAsStateFlow { topBarState ->
                topBarState?.isEditing == true
            }
        },
    ) { isProcessing, isEditing ->
        !isProcessing && !isEditing
    }

    val initiallySelectedPaymentMethodType: PaymentMethodCode
        get() = newPaymentSelection?.getPaymentMethodCode()
            ?: paymentMethodMetadata.value!!.supportedPaymentMethodTypes().first()

    init {
        viewModelScope.launch {
            // Drop the first item, since we don't need to clear errors/mandates when there aren't any.
            navigationHandler.currentScreen.drop(1).collect {
                clearErrorMessages()
            }
        }
    }

    protected fun setPaymentMethodMetadata(paymentMethodMetadata: PaymentMethodMetadata?) {
        _paymentMethodMetadata.value = paymentMethodMetadata
    }

    abstract fun clearErrorMessages()

    fun updatePrimaryButtonState(state: PrimaryButton.State) {
        _primaryButtonState.value = state
    }

    abstract fun handlePaymentMethodSelected(selection: PaymentSelection?)

    fun updateSelection(selection: PaymentSelection?) {
        when (selection) {
            is PaymentSelection.New -> newPaymentSelection = NewOrExternalPaymentSelection.New(selection)
            is PaymentSelection.ExternalPaymentMethod ->
                newPaymentSelection = NewOrExternalPaymentSelection.External(selection)
            else -> Unit
        }

        savedStateHandle[SAVE_SELECTION] = selection

        updateCvcFlows(selection)
        clearErrorMessages()
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

    abstract fun onError(error: ResolvableString? = null)

    companion object {
        internal const val SAVE_SELECTION = "selection"
        internal const val SAVE_PROCESSING = "processing"
    }
}
