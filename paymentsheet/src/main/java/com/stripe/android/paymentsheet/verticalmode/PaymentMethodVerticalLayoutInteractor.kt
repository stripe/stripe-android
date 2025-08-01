package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.viewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.ui.LinkButtonState
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.mandateTextFromPaymentMethodMetadata
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import com.stripe.android.R as PaymentsCoreR

internal interface PaymentMethodVerticalLayoutInteractor {
    val isLiveMode: Boolean

    val state: StateFlow<State>

    val showsWalletsHeader: StateFlow<Boolean>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val displayablePaymentMethods: List<DisplayablePaymentMethod>,
        val isProcessing: Boolean,
        val selection: Selection?,
        val displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
        val availableSavedPaymentMethodAction: SavedPaymentMethodAction,
        val mandate: ResolvableString?,
    )

    sealed interface Selection {
        val isSaved: Boolean
            get() = this == Saved

        object Saved : Selection
        data class New(
            val code: PaymentMethodCode,
            val changeDetails: String? = null,
            val canBeChanged: Boolean = false,
        ) : Selection
    }

    sealed interface ViewAction {
        data object TransitionToManageSavedPaymentMethods : ViewAction
        data class OnManageOneSavedPaymentMethod(val savedPaymentMethod: DisplayableSavedPaymentMethod) : ViewAction
        data class PaymentMethodSelected(val selectedPaymentMethodCode: String) : ViewAction
        data class SavedPaymentMethodSelected(val savedPaymentMethod: PaymentMethod) : ViewAction
    }

    enum class SavedPaymentMethodAction {
        NONE,
        MANAGE_ONE,
        MANAGE_ALL,
    }
}

internal class DefaultPaymentMethodVerticalLayoutInteractor(
    private val paymentMethodMetadata: PaymentMethodMetadata,
    processing: StateFlow<Boolean>,
    temporarySelection: StateFlow<PaymentMethodCode?>,
    selection: StateFlow<PaymentSelection?>,
    paymentMethodIncentiveInteractor: PaymentMethodIncentiveInteractor,
    private val formTypeForCode: (code: String) -> FormType,
    private val onFormFieldValuesChanged: (formValues: FormFieldValues, selectedPaymentMethodCode: String) -> Unit,
    private val transitionToManageScreen: () -> Unit,
    private val transitionToFormScreen: (selectedPaymentMethodCode: String) -> Unit,
    paymentMethods: StateFlow<List<PaymentMethod>>,
    private val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>,
    private val providePaymentMethodName: (PaymentMethodCode?) -> ResolvableString,
    private val canRemove: StateFlow<Boolean>,
    private val walletsState: StateFlow<WalletsState?>,
    private val canShowWalletsInline: Boolean,
    private val canShowWalletButtons: Boolean,
    private val canUpdateFullPaymentMethodDetails: StateFlow<Boolean>,
    private val updateSelection: (PaymentSelection?, Boolean) -> Unit,
    private val isCurrentScreen: StateFlow<Boolean>,
    private val reportPaymentMethodTypeSelected: (PaymentMethodCode) -> Unit,
    private val reportFormShown: (PaymentMethodCode) -> Unit,
    private val onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val shouldUpdateVerticalModeSelection: (String?) -> Boolean,
    private val invokeRowSelectionCallback: (() -> Unit)? = null,
    private val displaysMandatesInFormScreen: Boolean,
    dispatcher: CoroutineContext = Dispatchers.Default,
    mainDispatcher: CoroutineContext = Dispatchers.Main.immediate,
) : PaymentMethodVerticalLayoutInteractor {

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            bankFormInteractor: BankFormInteractor,
        ): PaymentMethodVerticalLayoutInteractor {
            val formHelper = DefaultFormHelper.create(viewModel, paymentMethodMetadata)
            val isCurrentScreen = viewModel.navigationHandler.currentScreen.mapAsStateFlow {
                it is PaymentSheetScreen.VerticalMode
            }
            return DefaultPaymentMethodVerticalLayoutInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                processing = viewModel.processing,
                temporarySelection = stateFlowOf(null),
                selection = viewModel.selection,
                paymentMethodIncentiveInteractor = bankFormInteractor.paymentMethodIncentiveInteractor,
                formTypeForCode = { code ->
                    formHelper.formTypeForCode(code)
                },
                onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
                transitionToManageScreen = {
                    val interactor = DefaultManageScreenInteractor.create(
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                        customerStateHolder = customerStateHolder,
                        savedPaymentMethodMutator = viewModel.savedPaymentMethodMutator,
                    )
                    val screen = PaymentSheetScreen.ManageSavedPaymentMethods(interactor = interactor)
                    viewModel.navigationHandler.transitionToWithDelay(screen)
                },
                transitionToFormScreen = { selectedPaymentMethodCode ->
                    val interactor = DefaultVerticalModeFormInteractor.create(
                        selectedPaymentMethodCode = selectedPaymentMethodCode,
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                        customerStateHolder = customerStateHolder,
                        bankFormInteractor = bankFormInteractor,
                    )
                    val screen = PaymentSheetScreen.VerticalModeForm(interactor = interactor)
                    viewModel.navigationHandler.transitionToWithDelay(screen)
                },
                paymentMethods = customerStateHolder.paymentMethods,
                mostRecentlySelectedSavedPaymentMethod = customerStateHolder.mostRecentlySelectedSavedPaymentMethod,
                providePaymentMethodName = viewModel.savedPaymentMethodMutator.providePaymentMethodName,
                canRemove = viewModel.customerStateHolder.canRemove,
                onUpdatePaymentMethod = { viewModel.savedPaymentMethodMutator.updatePaymentMethod(it) },
                updateSelection = { selection, isUserInput ->
                    if (isUserInput) {
                        viewModel.handlePaymentMethodSelected(selection)
                    } else {
                        viewModel.updateSelection(selection)
                    }
                },
                walletsState = viewModel.walletsState,
                canShowWalletsInline = !viewModel.isCompleteFlow,
                canShowWalletButtons = true,
                canUpdateFullPaymentMethodDetails = viewModel.customerStateHolder.canUpdateFullPaymentMethodDetails,
                isCurrentScreen = isCurrentScreen,
                reportPaymentMethodTypeSelected = viewModel.eventReporter::onSelectPaymentMethod,
                reportFormShown = viewModel.eventReporter::onPaymentMethodFormShown,
                shouldUpdateVerticalModeSelection = { paymentMethodCode ->
                    val requiresFormScreen = paymentMethodCode != null &&
                        formHelper.formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                    !requiresFormScreen
                },
                displaysMandatesInFormScreen = false,
            ).also { interactor ->
                viewModel.viewModelScope.launch {
                    interactor.state.collect { state ->
                        viewModel.mandateHandler.updateMandateText(
                            mandateText = state.mandate,
                            showAbove = true,
                        )
                    }
                }

                viewModel.viewModelScope.launch {
                    isCurrentScreen.filter { it }.collect {
                        viewModel.mandateHandler.updateMandateText(
                            mandateText = interactor.state.value.mandate,
                            showAbove = true,
                        )
                    }
                }
            }
        }
    }

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val _verticalModeScreenSelection = MutableStateFlow(selection.value)
    private val verticalModeScreenSelection = _verticalModeScreenSelection

    private val supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()

    private val displayedSavedPaymentMethod = combineAsStateFlow(
        paymentMethods,
        mostRecentlySelectedSavedPaymentMethod
    ) { paymentMethods, mostRecentlySelectedSavedPaymentMethod ->
        getDisplayedSavedPaymentMethod(
            paymentMethods = paymentMethods,
            paymentMethodMetadata = paymentMethodMetadata,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod
        )
    }

    private val availableSavedPaymentMethodAction = combineAsStateFlow(
        paymentMethods,
        displayedSavedPaymentMethod,
        canRemove,
        canUpdateFullPaymentMethodDetails,
    ) { paymentMethods, displayedSavedPaymentMethod, canRemove, canUpdateFullPaymentMethodDetails ->
        getAvailableSavedPaymentMethodAction(
            paymentMethods = paymentMethods,
            savedPaymentMethod = displayedSavedPaymentMethod,
            canRemove = canRemove,
            canUpdateFullPaymentMethodDetails = canUpdateFullPaymentMethodDetails,
        )
    }

    private val displayablePaymentMethods = combineAsStateFlow(
        paymentMethods,
        walletsState,
        paymentMethodIncentiveInteractor.displayedIncentive,
    ) { paymentMethods, walletsState, incentive ->
        getDisplayablePaymentMethods(paymentMethods, walletsState, incentive)
    }

    override val isLiveMode: Boolean = paymentMethodMetadata.stripeIntent.isLiveMode

    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = combineAsStateFlow(
        displayablePaymentMethods,
        processing,
        verticalModeScreenSelection,
        displayedSavedPaymentMethod,
        availableSavedPaymentMethodAction,
        temporarySelection,
    ) { displayablePaymentMethods, isProcessing, mostRecentSelection, displayedSavedPaymentMethod, action,
        temporarySelectionCode ->
        val temporarySelection = if (temporarySelectionCode != null) {
            val changeDetails = if (temporarySelectionCode == mostRecentSelection?.code()) {
                (mostRecentSelection as? PaymentSelection.New?)?.changeDetails()
            } else {
                null
            }
            PaymentMethodVerticalLayoutInteractor.Selection.New(
                code = temporarySelectionCode,
                changeDetails = changeDetails,
                canBeChanged = temporarySelectionCode == (mostRecentSelection as? PaymentSelection.New?)?.code(),
            )
        } else {
            null
        }
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = displayablePaymentMethods,
            isProcessing = isProcessing,
            selection = temporarySelection ?: mostRecentSelection?.asVerticalSelection(),
            displayedSavedPaymentMethod = displayedSavedPaymentMethod,
            availableSavedPaymentMethodAction = action,
            mandate = getMandate(temporarySelectionCode, mostRecentSelection),
        )
    }

    override val showsWalletsHeader: StateFlow<Boolean> = walletsState.mapAsStateFlow { walletsState ->
        !showsWalletsInline(walletsState)
    }

    init {
        coroutineScope.launch(mainDispatcher) {
            selection.collect { currentSelection ->
                if (currentSelection == null && !isCurrentScreen.value) {
                    return@collect
                }

                val paymentMethodCode = when (currentSelection) {
                    is PaymentSelection.New,
                    is PaymentSelection.ExternalPaymentMethod,
                    is PaymentSelection.CustomPaymentMethod -> currentSelection.code()
                    else -> null
                }

                if (shouldUpdateVerticalModeSelection(paymentMethodCode)) {
                    _verticalModeScreenSelection.value = currentSelection
                }
            }
        }

        coroutineScope.launch(mainDispatcher) {
            // When PaymentSheet opens with no existing selection, a saved PM will be selected by default, but
            // mostRecentlySelectedSavedPaymentMethod may not have been set. So we drop its first value, to ensure that
            // we correctly set the initial selection.
            mostRecentlySelectedSavedPaymentMethod.drop(1).collect { mostRecentlySelectedSavedPaymentMethod ->
                if (
                    mostRecentlySelectedSavedPaymentMethod == null &&
                    verticalModeScreenSelection.value is PaymentSelection.Saved
                ) {
                    _verticalModeScreenSelection.value = null
                }
            }
        }

        coroutineScope.launch(mainDispatcher) {
            isCurrentScreen.collect { isCurrentScreen ->
                if (isCurrentScreen) {
                    updateSelection(verticalModeScreenSelection.value, false)
                }
            }
        }
    }

    private fun getDisplayablePaymentMethods(
        paymentMethods: List<PaymentMethod>,
        walletsState: WalletsState?,
        incentive: PaymentMethodIncentive?,
    ): List<DisplayablePaymentMethod> {
        val lpms = supportedPaymentMethods.map { supportedPaymentMethod ->
            val paymentMethodIncentive = incentive?.takeIfMatches(supportedPaymentMethod.code)
            supportedPaymentMethod.asDisplayablePaymentMethod(paymentMethods, paymentMethodIncentive) {
                handleViewAction(ViewAction.PaymentMethodSelected(supportedPaymentMethod.code))
            }
        }

        val wallets = mutableListOf<DisplayablePaymentMethod>()
        if (showsWalletsInline(walletsState)) {
            walletsState?.link?.let {
                val subtitle = when (val state = it.state) {
                    is LinkButtonState.Email -> state.email.resolvableString
                    is LinkButtonState.DefaultPayment,
                    is LinkButtonState.Default ->
                        PaymentsCoreR.string.stripe_link_simple_secure_payments.resolvableString
                }

                wallets += DisplayablePaymentMethod(
                    code = PaymentMethod.Type.Link.code,
                    displayName = PaymentsCoreR.string.stripe_link.resolvableString,
                    iconResource = R.drawable.stripe_ic_paymentsheet_link_arrow,
                    iconResourceNight = null,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    subtitle = subtitle,
                    onClick = {
                        updateSelection(PaymentSelection.Link(), false)
                        invokeRowSelectionCallback?.invoke()
                    },
                )
            }

            walletsState?.googlePay?.let {
                wallets += DisplayablePaymentMethod(
                    code = "google_pay",
                    displayName = PaymentsCoreR.string.stripe_google_pay.resolvableString,
                    iconResource = PaymentsCoreR.drawable.stripe_google_pay_mark,
                    iconResourceNight = null,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    subtitle = null,
                    onClick = {
                        updateSelection(PaymentSelection.GooglePay, false)
                        invokeRowSelectionCallback?.invoke()
                    },
                )
            }
        }

        return wallets + lpms
    }

    private fun showsWalletsInline(walletsState: WalletsState?): Boolean {
        return canShowWalletsInline && walletsState != null && (walletsState.googlePay != null || !canShowWalletButtons)
    }

    private fun getDisplayedSavedPaymentMethod(
        paymentMethods: List<PaymentMethod>?,
        paymentMethodMetadata: PaymentMethodMetadata,
        mostRecentlySelectedSavedPaymentMethod: PaymentMethod?,
    ): DisplayableSavedPaymentMethod? {
        val paymentMethodToDisplay = mostRecentlySelectedSavedPaymentMethod ?: paymentMethods?.firstOrNull()
        return paymentMethodToDisplay?.toDisplayableSavedPaymentMethod(
            providePaymentMethodName,
            paymentMethodMetadata,
            defaultPaymentMethodId = null
        )
    }

    private fun getAvailableSavedPaymentMethodAction(
        paymentMethods: List<PaymentMethod>?,
        savedPaymentMethod: DisplayableSavedPaymentMethod?,
        canRemove: Boolean,
        canUpdateFullPaymentMethodDetails: Boolean,
    ): PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction {
        if (paymentMethods == null || savedPaymentMethod == null) {
            return PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
        }

        return when (paymentMethods.size) {
            0 -> PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
            1 -> {
                getSavedPaymentMethodActionForOnePaymentMethod(
                    canRemove = canRemove,
                    savedPaymentMethod = savedPaymentMethod,
                    canUpdateFullPaymentMethodDetails = canUpdateFullPaymentMethodDetails,
                )
            }
            else ->
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL
        }
    }

    private fun getSavedPaymentMethodActionForOnePaymentMethod(
        canRemove: Boolean,
        savedPaymentMethod: DisplayableSavedPaymentMethod?,
        canUpdateFullPaymentMethodDetails: Boolean,
    ): PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction {
        return if (savedPaymentMethod?.isModifiable(canUpdateFullPaymentMethodDetails) == true || canRemove) {
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
        } else {
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
        }
    }

    override fun handleViewAction(viewAction: ViewAction) {
        when (viewAction) {
            is ViewAction.PaymentMethodSelected -> {
                reportPaymentMethodTypeSelected(viewAction.selectedPaymentMethodCode)

                val formType = formTypeForCode(viewAction.selectedPaymentMethodCode)
                val displayFormForMandate = displaysMandatesInFormScreen && formType is FormType.MandateOnly
                if (formType == FormType.UserInteractionRequired || displayFormForMandate) {
                    reportFormShown(viewAction.selectedPaymentMethodCode)
                    transitionToFormScreen(viewAction.selectedPaymentMethodCode)
                } else {
                    updateSelectedPaymentMethod(viewAction.selectedPaymentMethodCode)
                }
            }
            is ViewAction.SavedPaymentMethodSelected -> {
                reportPaymentMethodTypeSelected("saved")
                val selection = PaymentSelection.Saved(viewAction.savedPaymentMethod)
                updateSelection(selection, true)
                invokeRowSelectionCallback?.invoke()
            }
            ViewAction.TransitionToManageSavedPaymentMethods -> {
                transitionToManageScreen()
            }
            is ViewAction.OnManageOneSavedPaymentMethod -> {
                onUpdatePaymentMethod(viewAction.savedPaymentMethod)
            }
        }
    }

    private fun updateSelectedPaymentMethod(selectedPaymentMethodCode: String) {
        val formArguments = FormArgumentsFactory.create(selectedPaymentMethodCode, paymentMethodMetadata)

        onFormFieldValuesChanged(
            formArguments.noUserInteractionFormFieldValues(),
            selectedPaymentMethodCode,
        )
    }

    private fun getMandate(temporarySelectionCode: String?, selection: PaymentSelection?): ResolvableString? {
        val selectionCode = temporarySelectionCode ?: (selection as? PaymentSelection.New)?.code()
        return if (selectionCode != null) {
            if (displaysMandatesInFormScreen) {
                null
            } else {
                (formTypeForCode(selectionCode) as? FormType.MandateOnly)?.mandate
            }
        } else {
            val savedSelection = selection as? PaymentSelection.Saved?
            savedSelection?.mandateTextFromPaymentMethodMetadata(paymentMethodMetadata)
        }
    }

    private fun PaymentSelection.asVerticalSelection(): PaymentMethodVerticalLayoutInteractor.Selection = when (this) {
        is PaymentSelection.Saved -> PaymentMethodVerticalLayoutInteractor.Selection.Saved
        is PaymentSelection.GooglePay -> PaymentMethodVerticalLayoutInteractor.Selection.New("google_pay")
        is PaymentSelection.Link -> PaymentMethodVerticalLayoutInteractor.Selection.New("link")
        is PaymentSelection.ShopPay -> PaymentMethodVerticalLayoutInteractor.Selection.New("shop_pay")
        is PaymentSelection.New -> PaymentMethodVerticalLayoutInteractor.Selection.New(
            code = paymentMethodCreateParams.typeCode,
            changeDetails = changeDetails(),
            canBeChanged = formTypeForCode(paymentMethodCreateParams.typeCode) == FormType.UserInteractionRequired,
        )
        is PaymentSelection.ExternalPaymentMethod -> PaymentMethodVerticalLayoutInteractor.Selection.New(type)
        is PaymentSelection.CustomPaymentMethod -> PaymentMethodVerticalLayoutInteractor.Selection.New(id)
    }

    private fun PaymentSelection.New.changeDetails(): String? = when (this) {
        is PaymentSelection.New.Card -> {
            val cardBrand = brand.displayName.takeIf { brand != CardBrand.Unknown }
            "${cardBrand?.plus(" ").orEmpty()}···· $last4"
        }
        is PaymentSelection.New.USBankAccount -> label
        else -> null
    }
}
