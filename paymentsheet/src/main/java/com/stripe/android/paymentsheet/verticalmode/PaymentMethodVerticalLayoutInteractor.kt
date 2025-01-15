package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.viewModelScope
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
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
        val selection: PaymentSelection?,
        val displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
        val availableSavedPaymentMethodAction: SavedPaymentMethodAction,
    )

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
    paymentMethodMetadata: PaymentMethodMetadata,
    processing: StateFlow<Boolean>,
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
    private val onSelectSavedPaymentMethod: (PaymentMethod) -> Unit,
    private val walletsState: StateFlow<WalletsState?>,
    private val canShowWalletsInline: Boolean,
    private val canShowWalletButtons: Boolean,
    private val onMandateTextUpdated: (ResolvableString?) -> Unit,
    private val updateSelection: (PaymentSelection?) -> Unit,
    private val isCurrentScreen: StateFlow<Boolean>,
    private val reportPaymentMethodTypeSelected: (PaymentMethodCode) -> Unit,
    private val reportFormShown: (PaymentMethodCode) -> Unit,
    private val onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    override val isLiveMode: Boolean,
    dispatcher: CoroutineContext = Dispatchers.Default,
) : PaymentMethodVerticalLayoutInteractor {

    sealed interface FormType {
        object Empty : FormType
        data class MandateOnly(val mandate: ResolvableString) : FormType
        object UserInteractionRequired : FormType
    }

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            bankFormInteractor: BankFormInteractor,
        ): PaymentMethodVerticalLayoutInteractor {
            val linkInlineHandler = LinkInlineHandler.create(viewModel, viewModel.viewModelScope)
            val formHelper = DefaultFormHelper.create(viewModel, linkInlineHandler, paymentMethodMetadata)
            return DefaultPaymentMethodVerticalLayoutInteractor(
                paymentMethodMetadata = paymentMethodMetadata,
                processing = viewModel.processing,
                selection = viewModel.selection,
                paymentMethodIncentiveInteractor = bankFormInteractor.paymentMethodIncentiveInteractor,
                formTypeForCode = { code ->
                    if (formHelper.requiresFormScreen(code)) {
                        FormType.UserInteractionRequired
                    } else {
                        val mandate = formHelper.formElementsForCode(code).firstNotNullOfOrNull { it.mandateText }
                        if (mandate == null) {
                            FormType.Empty
                        } else {
                            FormType.MandateOnly(mandate)
                        }
                    }
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
                onSelectSavedPaymentMethod = {
                    viewModel.handlePaymentMethodSelected(PaymentSelection.Saved(it))
                },
                onUpdatePaymentMethod = { viewModel.savedPaymentMethodMutator.updatePaymentMethod(it) },
                walletsState = viewModel.walletsState,
                canShowWalletsInline = !viewModel.isCompleteFlow,
                canShowWalletButtons = true,
                updateSelection = viewModel::updateSelection,
                isCurrentScreen = viewModel.navigationHandler.currentScreen.mapAsStateFlow {
                    it is PaymentSheetScreen.VerticalMode
                },
                onMandateTextUpdated = {
                    viewModel.mandateHandler.updateMandateText(mandateText = it, showAbove = true)
                },
                reportPaymentMethodTypeSelected = viewModel.eventReporter::onSelectPaymentMethod,
                reportFormShown = viewModel.eventReporter::onPaymentMethodFormShown,
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode
            )
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
    ) { paymentMethods, displayedSavedPaymentMethod, canRemove ->
        getAvailableSavedPaymentMethodAction(
            paymentMethods = paymentMethods,
            savedPaymentMethod = displayedSavedPaymentMethod,
            canRemove = canRemove
        )
    }

    private val displayablePaymentMethods = combineAsStateFlow(
        paymentMethods,
        walletsState,
        paymentMethodIncentiveInteractor.displayedIncentive,
    ) { paymentMethods, walletsState, incentive ->
        getDisplayablePaymentMethods(paymentMethods, walletsState, incentive)
    }

    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = combineAsStateFlow(
        displayablePaymentMethods,
        processing,
        verticalModeScreenSelection,
        displayedSavedPaymentMethod,
        availableSavedPaymentMethodAction,
    ) { displayablePaymentMethods, isProcessing, mostRecentSelection, displayedSavedPaymentMethod, action ->
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = displayablePaymentMethods,
            isProcessing = isProcessing,
            selection = mostRecentSelection,
            displayedSavedPaymentMethod = displayedSavedPaymentMethod,
            availableSavedPaymentMethodAction = action,
        )
    }

    override val showsWalletsHeader: StateFlow<Boolean> = walletsState.mapAsStateFlow { walletsState ->
        !showsWalletsInline(walletsState)
    }

    init {
        coroutineScope.launch {
            selection.collect {
                if (it == null && !isCurrentScreen.value) {
                    return@collect
                }

                val paymentMethodCode = (it as? PaymentSelection.New).code()
                    ?: (it as? PaymentSelection.ExternalPaymentMethod).code()
                val requiresFormScreen = paymentMethodCode != null &&
                    formTypeForCode(paymentMethodCode) == FormType.UserInteractionRequired
                if (!requiresFormScreen) {
                    _verticalModeScreenSelection.value = it
                }
            }
        }

        coroutineScope.launch {
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

        coroutineScope.launch {
            isCurrentScreen.collect { isCurrentScreen ->
                if (isCurrentScreen) {
                    updateSelection(verticalModeScreenSelection.value)
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
                wallets += DisplayablePaymentMethod(
                    code = PaymentMethod.Type.Link.code,
                    displayName = PaymentsCoreR.string.stripe_link.resolvableString,
                    iconResource = R.drawable.stripe_ic_paymentsheet_link_arrow,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    subtitle = PaymentsCoreR.string.stripe_link_simple_secure_payments.resolvableString,
                    onClick = {
                        updateSelection(PaymentSelection.Link)
                    },
                )
            }

            walletsState?.googlePay?.let {
                wallets += DisplayablePaymentMethod(
                    code = "google_pay",
                    displayName = PaymentsCoreR.string.stripe_google_pay.resolvableString,
                    iconResource = PaymentsCoreR.drawable.stripe_google_pay_mark,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    subtitle = null,
                    onClick = {
                        updateSelection(PaymentSelection.GooglePay)
                    },
                )
            }
        }

        val cardIndex = lpms.indexOfFirst { it.code == PaymentMethod.Type.Card.code }
        val result = lpms.toMutableList()
        // Add wallets after cards, if cards don't exist, add wallets first.
        result.addAll(cardIndex + 1, wallets)
        return result
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
                )
            }
            else ->
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL
        }
    }

    private fun getSavedPaymentMethodActionForOnePaymentMethod(
        canRemove: Boolean,
        savedPaymentMethod: DisplayableSavedPaymentMethod?,
    ): PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction {
        return if (savedPaymentMethod?.isModifiable() == true || canRemove) {
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
                if (formType == FormType.UserInteractionRequired) {
                    reportFormShown(viewAction.selectedPaymentMethodCode)
                    transitionToFormScreen(viewAction.selectedPaymentMethodCode)
                } else {
                    updateSelectedPaymentMethod(viewAction.selectedPaymentMethodCode)

                    if (formType is FormType.MandateOnly) {
                        onMandateTextUpdated(formType.mandate)
                    }
                }
            }
            is ViewAction.SavedPaymentMethodSelected -> {
                reportPaymentMethodTypeSelected("saved")
                onSelectSavedPaymentMethod(viewAction.savedPaymentMethod)
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
        onFormFieldValuesChanged(
            FormFieldValues(
                // userRequestedReuse only changes based on `SaveForFutureUse`, which won't ever hit this
                // code path.
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest
            ),
            selectedPaymentMethodCode,
        )
    }
}
