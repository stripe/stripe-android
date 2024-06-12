package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface VerticalModeFormInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val selectedPaymentMethodCode: String,
        val isProcessing: Boolean,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val formArguments: FormArguments,
        val formElements: List<FormElement>,
        val linkSignupMode: LinkSignupMode?,
        val linkConfigurationCoordinator: LinkConfigurationCoordinator,
        val headerInformation: FormHeaderInformation?,
    )

    sealed interface ViewAction {
        data object FieldInteraction : ViewAction
        data class FormFieldValuesChanged(val formValues: FormFieldValues?) : ViewAction
        data class LinkSignupStateChanged(val linkInlineSignupViewState: InlineSignupViewState) : ViewAction
    }
}

internal class DefaultVerticalModeFormInteractor(
    private val selectedPaymentMethodCode: String,
    private val viewModel: BaseSheetViewModel,
) : VerticalModeFormInteractor {
    private val formArguments: FormArguments = viewModel.createFormArguments(selectedPaymentMethodCode)
    private val formElements: List<FormElement> = viewModel.formElementsForCode(selectedPaymentMethodCode)
    private val usBankAccountArguments: USBankAccountFormArguments =
        USBankAccountFormArguments.create(viewModel, selectedPaymentMethodCode)

    override val state: StateFlow<VerticalModeFormInteractor.State> = combineAsStateFlow(
        viewModel.processing,
        viewModel.linkSignupMode,
        viewModel.paymentMethodMetadata,
    ) { isProcessing, linkSignupMode, paymentMethodMetadata ->
        VerticalModeFormInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            isProcessing = isProcessing,
            usBankAccountFormArguments = usBankAccountArguments,
            formArguments = formArguments,
            formElements = formElements,
            linkSignupMode = linkSignupMode.takeIf { selectedPaymentMethodCode == PaymentMethod.Type.Card.code },
            linkConfigurationCoordinator = viewModel.linkConfigurationCoordinator,
            headerInformation = paymentMethodMetadata?.formHeaderInformationForCode(selectedPaymentMethodCode),
        )
    }

    override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
        when (viewAction) {
            VerticalModeFormInteractor.ViewAction.FieldInteraction -> {
                viewModel.reportFieldInteraction(selectedPaymentMethodCode)
            }
            is VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged -> {
                viewModel.onFormFieldValuesChanged(viewAction.formValues, selectedPaymentMethodCode)
            }
            is VerticalModeFormInteractor.ViewAction.LinkSignupStateChanged -> {
                viewModel.onLinkSignUpStateUpdated(viewAction.linkInlineSignupViewState)
            }
        }
    }
}
