package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface PaymentMethodVerticalLayoutInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val displayablePaymentMethods: List<DisplayablePaymentMethod>,
        val isProcessing: Boolean,
        val selection: PaymentSelection?,
        val displayedSavedPaymentMethod: DisplayableSavedPaymentMethod?,
    )

    sealed interface ViewAction {
        data object TransitionToManageSavedPaymentMethods : ViewAction
        data class PaymentMethodSelected(val selectedPaymentMethodCode: String) : ViewAction
        data class SavedPaymentMethodSelected(val savedPaymentMethod: DisplayableSavedPaymentMethod) : ViewAction
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
    private val formScreenFactory: (selectedPaymentMethodCode: String) -> PaymentSheetScreen,
    paymentMethods: StateFlow<List<PaymentMethod>?>,
    private val mostRecentlySelectedSavedPaymentMethod: StateFlow<PaymentMethod?>,
    private val providePaymentMethodName: (PaymentMethodCode?) -> String,
    private val onSelectSavedPaymentMethod: (DisplayableSavedPaymentMethod) -> Unit,
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
        onSelectSavedPaymentMethod = {
            viewModel.handlePaymentMethodSelected(PaymentSelection.Saved(it.paymentMethod))
        }
    )

    private val supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()

    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = combineAsStateFlow(
        processing,
        selection,
        paymentMethods,
        mostRecentlySelectedSavedPaymentMethod,
    ) { isProcessing, selection, paymentMethods, mostRecentlySelectedSavedPaymentMethod ->
        PaymentMethodVerticalLayoutInteractor.State(
            displayablePaymentMethods = getDisplayablePaymentMethods(),
            isProcessing = isProcessing,
            selection = selection,
            displayedSavedPaymentMethod = getDisplayedSavedPaymentMethod(
                paymentMethods,
                paymentMethodMetadata,
                mostRecentlySelectedSavedPaymentMethod
            ),
        )
    }

    private fun getDisplayablePaymentMethods(): List<DisplayablePaymentMethod> {
        return supportedPaymentMethods.map { supportedPaymentMethod ->
            supportedPaymentMethod.asDisplayablePaymentMethod {
                handleViewAction(ViewAction.PaymentMethodSelected(supportedPaymentMethod.code))
            }
        }
    }

    private fun getDisplayedSavedPaymentMethod(
        paymentMethods: List<PaymentMethod>?,
        paymentMethodMetadata: PaymentMethodMetadata,
        mostRecentlySelectedSavedPaymentMethod: PaymentMethod?,
    ): DisplayableSavedPaymentMethod? {
        val paymentMethodToDisplay = mostRecentlySelectedSavedPaymentMethod ?: paymentMethods?.firstOrNull()
        return paymentMethodToDisplay?.toDisplayableSavedPaymentMethod(providePaymentMethodName, paymentMethodMetadata)
    }

    override fun handleViewAction(viewAction: ViewAction) {
        when (viewAction) {
            is ViewAction.PaymentMethodSelected -> {
                if (requiresFormScreen(viewAction.selectedPaymentMethodCode)) {
                    transitionTo(formScreenFactory(viewAction.selectedPaymentMethodCode))
                } else {
                    updateSelectedPaymentMethod(viewAction.selectedPaymentMethodCode)
                }
            }
            is ViewAction.SavedPaymentMethodSelected -> {
                onSelectSavedPaymentMethod(viewAction.savedPaymentMethod)
            }
            ViewAction.TransitionToManageSavedPaymentMethods -> {
                transitionTo(manageScreenFactory())
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
