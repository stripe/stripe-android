package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.code
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface PaymentMethodVerticalLayoutInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val isProcessing: Boolean,
        val selectedPaymentMethodIndex: Int,
    )

    sealed interface ViewAction {
        data object TransitionToManageSavedPaymentMethods : ViewAction
        data class PaymentMethodSelected(val selectedPaymentMethodCode: String) : ViewAction
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
    )

    private val supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods()

    override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = combineAsStateFlow(
        processing,
        selection,
    ) { isProcessing, selection ->
        PaymentMethodVerticalLayoutInteractor.State(
            supportedPaymentMethods = supportedPaymentMethods,
            isProcessing = isProcessing,
            selectedPaymentMethodIndex = selectedPaymentMethodIndex(selection),
        )
    }

    private fun selectedPaymentMethodIndex(
        selection: PaymentSelection?,
    ): Int {
        if (selection is PaymentSelection.Saved) {
            return -1
        }
        val selectedPaymentMethodCode = selection.code()
        return supportedPaymentMethods.indexOfFirst { it.code == selectedPaymentMethodCode }
    }

    override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
        when (viewAction) {
            is PaymentMethodVerticalLayoutInteractor.ViewAction.PaymentMethodSelected -> {
                if (requiresFormScreen(viewAction.selectedPaymentMethodCode)) {
                    transitionTo(formScreenFactory(viewAction.selectedPaymentMethodCode))
                } else {
                    updateSelectedPaymentMethod(viewAction.selectedPaymentMethodCode)
                }
            }
            PaymentMethodVerticalLayoutInteractor.ViewAction.TransitionToManageSavedPaymentMethods -> {
                transitionTo(manageScreenFactory())
            }
        }
    }

    private fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean {
        val formElements = formElementsForCode(selectedPaymentMethodCode)
        val userInteractionFormElements = formElements.filter {
            it.allowsUserInteraction
        }
        return userInteractionFormElements.isNotEmpty() ||
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
