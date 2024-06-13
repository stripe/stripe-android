package com.stripe.android.paymentsheet.ui

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface AddPaymentMethodInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun createFormArguments(code: PaymentMethodCode): FormArguments

    fun formElementsForCode(code: PaymentMethodCode): List<FormElement>

    fun createUsBankAccountFormElements(code: PaymentMethodCode): USBankAccountFormArguments

    data class State(
        val selectedPaymentMethodCode: String,
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val arguments: FormArguments,
        val paymentSelection: PaymentSelection?,
        val linkSignupMode: LinkSignupMode?,
        val processing: Boolean,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    )

    sealed class ViewAction {
        data object ClearErrorMessages : ViewAction()
        data class ReportPaymentMethodTypeSelected(val code: PaymentMethodCode) : ViewAction()
        data class OnLinkSignUpStateUpdated(val state: InlineSignupViewState) : ViewAction()
        data class OnFormFieldValuesChanged(val formValues: FormFieldValues?, val selectedPaymentMethodCode: String) : ViewAction()
        data class ReportFieldInteraction(val code: PaymentMethodCode) : ViewAction()
    }
}

internal class DefaultAddPaymentMethodInteractor(private val sheetViewModel: BaseSheetViewModel): AddPaymentMethodInteractor {
    override val state = combineAsStateFlow(
       sheetViewModel.selection,
        sheetViewModel.linkSignupMode,
        sheetViewModel.processing,
    ) { selection, linkSignupMode, processing ->
        val selectedPaymentMethodCode = sheetViewModel.initiallySelectedPaymentMethodType

        AddPaymentMethodInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
            arguments = sheetViewModel.createFormArguments(selectedPaymentMethodCode),
            paymentSelection = selection,
            linkSignupMode = linkSignupMode,
            processing = processing,
            usBankAccountFormArguments = USBankAccountFormArguments.create(sheetViewModel, selectedPaymentMethodCode),
            linkConfigurationCoordinator = sheetViewModel.linkConfigurationCoordinator,
        )
    }

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            AddPaymentMethodInteractor.ViewAction.ClearErrorMessages -> sheetViewModel.clearErrorMessages()
            is AddPaymentMethodInteractor.ViewAction.OnLinkSignUpStateUpdated -> sheetViewModel.onLinkSignUpStateUpdated(
                viewAction.state
            )
            is AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction -> sheetViewModel.reportFieldInteraction(
                viewAction.code
            )
            is AddPaymentMethodInteractor.ViewAction.ReportPaymentMethodTypeSelected -> sheetViewModel.reportPaymentMethodTypeSelected(
                viewAction.code
            )
            is AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged -> sheetViewModel.onFormFieldValuesChanged(
                viewAction.formValues,
                viewAction.selectedPaymentMethodCode
            )
        }
    }

    override fun createFormArguments(code: PaymentMethodCode): FormArguments {
       return sheetViewModel.createFormArguments(code)
    }

    override fun formElementsForCode(code: PaymentMethodCode): List<FormElement> {
        return sheetViewModel.formElementsForCode(code)
    }

    override fun createUsBankAccountFormElements(code: PaymentMethodCode): USBankAccountFormArguments {
        return USBankAccountFormArguments.create(sheetViewModel, code)
    }
}