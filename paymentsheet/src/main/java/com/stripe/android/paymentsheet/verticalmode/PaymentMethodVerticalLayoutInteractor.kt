package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsViewModel
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.state.WalletsState
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.stripe.android.R as PaymentsCoreR

internal interface PaymentMethodVerticalLayoutInteractor {
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
        data object TransitionToManageOneSavedPaymentMethod : ViewAction
        data class PaymentMethodSelected(val selectedPaymentMethodCode: String) : ViewAction
        data class SavedPaymentMethodSelected(val savedPaymentMethod: PaymentMethod) : ViewAction
        data class EditPaymentMethod(val savedPaymentMethod: DisplayableSavedPaymentMethod) : ViewAction
    }

    enum class SavedPaymentMethodAction {
        NONE,
        EDIT_CARD_BRAND,
        MANAGE_ONE,
        MANAGE_ALL,
    }
}

internal class DefaultPaymentMethodVerticalLayoutInteractor(
    paymentMethodMetadata: PaymentMethodMetadata,
    processing: StateFlow<Boolean>,
    selection: StateFlow<PaymentSelection?>,
    private val formElementsForCode: (code: String) -> List<FormElement>,
    private val transitionTo: (screen: PaymentSheetScreen) -> Unit,
    private val onFormFieldValuesChanged: (formValues: FormFieldValues, selectedPaymentMethodCode: String) -> Unit,
    private val manageScreenFactory: () -> PaymentSheetScreen,
    private val manageOneSavedPaymentMethodFactory: () -> PaymentSheetScreen,
    private val formScreenFactory: (selectedPaymentMethodCode: String) -> PaymentSheetScreen,
    paymentMethods: StateFlow<List<PaymentMethod>?>,
    private val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>,
    private val providePaymentMethodName: (PaymentMethodCode?) -> String,
    private val allowsRemovalOfLastSavedPaymentMethod: Boolean,
    private val onEditPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
    private val onSelectSavedPaymentMethod: (PaymentMethod) -> Unit,
    private val walletsState: StateFlow<WalletsState?>,
    private val isFlowController: Boolean,
    private val onWalletSelected: (PaymentSelection) -> Unit,
    private val onMandateTextUpdated: (ResolvableString?) -> Unit,
) : PaymentMethodVerticalLayoutInteractor {
    constructor(viewModel: BaseSheetViewModel) : this(
        paymentMethodMetadata = requireNotNull(viewModel.paymentMethodMetadata.value),
        processing = viewModel.processing,
        selection = viewModel.selection,
        formElementsForCode = viewModel::formElementsForCode,
        transitionTo = viewModel::transitionTo,
        onFormFieldValuesChanged = viewModel::onFormFieldValuesChanged,
        manageScreenFactory = {
            PaymentSheetScreen.ManageSavedPaymentMethods(
                interactor = DefaultManageScreenInteractor(
                    viewModel
                )
            )
        },
        manageOneSavedPaymentMethodFactory = {
            PaymentSheetScreen.ManageOneSavedPaymentMethod(
                interactor = DefaultManageOneSavedPaymentMethodInteractor(viewModel)
            )
        },
        formScreenFactory = { selectedPaymentMethodCode ->
            PaymentSheetScreen.Form(
                DefaultVerticalModeFormInteractor(
                    selectedPaymentMethodCode,
                    viewModel
                )
            )
        },
        paymentMethods = viewModel.paymentMethods,
        mostRecentlySelectedSavedPaymentMethod = viewModel.mostRecentlySelectedSavedPaymentMethod,
        providePaymentMethodName = viewModel::providePaymentMethodName,
        allowsRemovalOfLastSavedPaymentMethod = viewModel.config.allowsRemovalOfLastSavedPaymentMethod,
        onEditPaymentMethod = { viewModel.modifyPaymentMethod(it.paymentMethod) },
        onSelectSavedPaymentMethod = {
            viewModel.handlePaymentMethodSelected(PaymentSelection.Saved(it))
        },
        walletsState = viewModel.walletsState,
        isFlowController = viewModel is PaymentOptionsViewModel,
        onWalletSelected = viewModel::updateSelection,
        onMandateTextUpdated = {
            viewModel.updateMandateText(it?.resolve(viewModel.getApplication()), true)
        },
    )

    private val supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()

    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = combineAsStateFlow(
        processing,
        selection,
        paymentMethods,
        mostRecentlySelectedSavedPaymentMethod,
        walletsState,
    ) { isProcessing, selection, paymentMethods, mostRecentlySelectedSavedPaymentMethod, walletsState ->
        val displayedSavedPaymentMethod = getDisplayedSavedPaymentMethod(
            paymentMethods,
            paymentMethodMetadata,
            mostRecentlySelectedSavedPaymentMethod
        )

        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = getDisplayablePaymentMethods(walletsState),
            isProcessing = isProcessing,
            selection = selection,
            displayedSavedPaymentMethod = displayedSavedPaymentMethod,
            availableSavedPaymentMethodAction = getAvailableSavedPaymentMethodAction(
                paymentMethods,
                displayedSavedPaymentMethod,
                allowsRemovalOfLastSavedPaymentMethod
            )
        )
    }

    override val showsWalletsHeader: StateFlow<Boolean> = walletsState.mapAsStateFlow { walletsState ->
        !showsWalletsInline(walletsState)
    }

    private fun getDisplayablePaymentMethods(walletsState: WalletsState?): List<DisplayablePaymentMethod> {
        val lpms = supportedPaymentMethods.map { supportedPaymentMethod ->
            supportedPaymentMethod.asDisplayablePaymentMethod {
                handleViewAction(ViewAction.PaymentMethodSelected(supportedPaymentMethod.code))
            }
        }

        val wallets = mutableListOf<DisplayablePaymentMethod>()
        if (showsWalletsInline(walletsState)) {
            walletsState?.link?.let {
                wallets += DisplayablePaymentMethod(
                    code = PaymentMethod.Type.Link.code,
                    displayName = resolvableString(PaymentsCoreR.string.stripe_link),
                    iconResource = R.drawable.stripe_ic_paymentsheet_link,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    subtitle = null,
                    onClick = {
                        onWalletSelected(PaymentSelection.Link)
                    },
                )
            }

            walletsState?.googlePay?.let {
                wallets += DisplayablePaymentMethod(
                    code = "google_pay",
                    displayName = resolvableString(PaymentsCoreR.string.stripe_google_pay),
                    iconResource = PaymentsCoreR.drawable.stripe_google_pay_mark,
                    lightThemeIconUrl = null,
                    darkThemeIconUrl = null,
                    iconRequiresTinting = false,
                    subtitle = null,
                    onClick = {
                        onWalletSelected(PaymentSelection.GooglePay)
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
        return isFlowController && walletsState != null && walletsState.googlePay != null
    }

    private fun getDisplayedSavedPaymentMethod(
        paymentMethods: List<PaymentMethod>?,
        paymentMethodMetadata: PaymentMethodMetadata,
        mostRecentlySelectedSavedPaymentMethod: PaymentMethod?,
    ): DisplayableSavedPaymentMethod? {
        val paymentMethodToDisplay = mostRecentlySelectedSavedPaymentMethod ?: paymentMethods?.firstOrNull()
        return paymentMethodToDisplay?.toDisplayableSavedPaymentMethod(providePaymentMethodName, paymentMethodMetadata)
    }

    private fun getAvailableSavedPaymentMethodAction(
        paymentMethods: List<PaymentMethod>?,
        savedPaymentMethod: DisplayableSavedPaymentMethod?,
        allowsRemovalOfLastSavedPaymentMethod: Boolean,
    ): PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction {
        if (paymentMethods == null || savedPaymentMethod == null) {
            return PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
        }

        return when (paymentMethods.size) {
            0 -> PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
            1 -> {
                getSavedPaymentMethodActionForOnePaymentMethod(
                    savedPaymentMethod,
                    allowsRemovalOfLastSavedPaymentMethod
                )
            }
            else ->
                PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ALL
        }
    }

    private fun getSavedPaymentMethodActionForOnePaymentMethod(
        paymentMethod: DisplayableSavedPaymentMethod,
        allowsRemovalOfLastSavedPaymentMethod: Boolean
    ): PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction {
        return if (allowsRemovalOfLastSavedPaymentMethod) {
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.MANAGE_ONE
        } else if (paymentMethod.isModifiable()) {
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.EDIT_CARD_BRAND
        } else {
            PaymentMethodVerticalLayoutInteractor.SavedPaymentMethodAction.NONE
        }
    }

    override fun handleViewAction(viewAction: ViewAction) {
        when (viewAction) {
            is ViewAction.PaymentMethodSelected -> {
                if (requiresFormScreen(viewAction.selectedPaymentMethodCode)) {
                    transitionTo(formScreenFactory(viewAction.selectedPaymentMethodCode))
                } else {
                    updateSelectedPaymentMethod(viewAction.selectedPaymentMethodCode)

                    formElementsForCode(viewAction.selectedPaymentMethodCode)
                        .firstNotNullOfOrNull { it.mandateText }?.let {
                            onMandateTextUpdated(it)
                        }
                }
            }
            is ViewAction.SavedPaymentMethodSelected -> {
                onSelectSavedPaymentMethod(viewAction.savedPaymentMethod)
            }
            ViewAction.TransitionToManageSavedPaymentMethods -> {
                transitionTo(manageScreenFactory())
            }
            ViewAction.TransitionToManageOneSavedPaymentMethod -> {
                transitionTo(manageOneSavedPaymentMethodFactory())
            }
            is ViewAction.EditPaymentMethod -> {
                onEditPaymentMethod(viewAction.savedPaymentMethod)
            }
        }
    }

    private fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean {
        val userInteractionAllowed = formElementsForCode(selectedPaymentMethodCode).any { it.allowsUserInteraction }
        return userInteractionAllowed ||
            selectedPaymentMethodCode == PaymentMethod.Type.USBankAccount.code ||
            selectedPaymentMethodCode == PaymentMethod.Type.Link.code
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
