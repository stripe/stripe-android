package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface AddPaymentMethodInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    data class State(
        val selectedPaymentMethodCode: PaymentMethodCode,
        val supportedPaymentMethods: List<SupportedPaymentMethod>,
        val arguments: FormArguments,
        val formElements: List<FormElement>,
        val paymentSelection: PaymentSelection?,
        val linkSignupMode: LinkSignupMode?,
        val linkInlineSignupMode: LinkSignupMode?,
        val processing: Boolean,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    )

    sealed class ViewAction {
        data object ClearErrorMessages : ViewAction()
        data class OnPaymentMethodSelected(val code: PaymentMethodCode) : ViewAction()
        data class OnLinkSignUpStateUpdated(val state: InlineSignupViewState) : ViewAction()
        data class OnFormFieldValuesChanged(val formValues: FormFieldValues?, val selectedPaymentMethodCode: String) : ViewAction()
        data class ReportFieldInteraction(val code: PaymentMethodCode) : ViewAction()
    }
}

internal class DefaultAddPaymentMethodInteractor(
    private val initiallySelectedPaymentMethodType: PaymentMethodCode,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val selection: StateFlow<PaymentSelection?>,
    private val linkSignupMode: StateFlow<LinkSignupMode?>,
    private val processing: StateFlow<Boolean>,
    private val supportedPaymentMethods: List<SupportedPaymentMethod>,
    private val createFormArguments: (PaymentMethodCode) -> FormArguments,
    private val formElementsForCode: (PaymentMethodCode) -> List<FormElement>,
    private val clearErrorMessages: () -> Unit,
    private val onLinkSignUpStateUpdated: (InlineSignupViewState) -> Unit,
    private val reportFieldInteraction: (PaymentMethodCode) -> Unit,
    private val onFormFieldValuesChanged: (FormFieldValues?, String) -> Unit,
    private val reportPaymentMethodTypeSelected: (PaymentMethodCode) -> Unit,
    private val createUSBankAccountFormArguments: (PaymentMethodCode) -> USBankAccountFormArguments,
): AddPaymentMethodInteractor {

    constructor(sheetViewModel: BaseSheetViewModel) : this(
        initiallySelectedPaymentMethodType = sheetViewModel.initiallySelectedPaymentMethodType,
        linkConfigurationCoordinator = sheetViewModel.linkConfigurationCoordinator,
        selection = sheetViewModel.selection,
        linkSignupMode = sheetViewModel.linkSignupMode,
        processing = sheetViewModel.processing,
        supportedPaymentMethods = sheetViewModel.supportedPaymentMethods,
        createFormArguments = sheetViewModel::createFormArguments,
        formElementsForCode = sheetViewModel::formElementsForCode,
        clearErrorMessages = sheetViewModel::clearErrorMessages,
        onLinkSignUpStateUpdated = sheetViewModel::onLinkSignUpStateUpdated,
        reportFieldInteraction = sheetViewModel::reportFieldInteraction,
        onFormFieldValuesChanged = sheetViewModel::onFormFieldValuesChanged,
        reportPaymentMethodTypeSelected = sheetViewModel::reportPaymentMethodTypeSelected,
        createUSBankAccountFormArguments = { USBankAccountFormArguments.create(sheetViewModel, it) }
    )

    // TODO: current issue is that the AddPaymentMethodInteractor is recreated when you click on a form field, which
    // recreates with the original initially selected PM type, which breaks everything.
    private val selectedPaymentMethodCode: String by rememberSaveable {
        mutableStateOf(initiallySelectedPaymentMethodType)
    }

    override val state = combineAsStateFlow(
        selectedPaymentMethodCode,
        selection,
        linkSignupMode,
        processing,
    ) { selectedPaymentMethodCode, selection, linkSignupMode, processing ->

        AddPaymentMethodInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            supportedPaymentMethods = supportedPaymentMethods,
            arguments = createFormArguments(selectedPaymentMethodCode),
            formElements = formElementsForCode(selectedPaymentMethodCode),
            paymentSelection = selection,
            linkSignupMode = linkSignupMode,
            linkInlineSignupMode = linkSignupMode.takeIf { selectedPaymentMethodCode == PaymentMethod.Type.Card.code },
            processing = processing,
            usBankAccountFormArguments = createUSBankAccountFormArguments(selectedPaymentMethodCode),
            linkConfigurationCoordinator = linkConfigurationCoordinator,
        )
    }

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        when (viewAction) {
            AddPaymentMethodInteractor.ViewAction.ClearErrorMessages -> clearErrorMessages()
            is AddPaymentMethodInteractor.ViewAction.OnLinkSignUpStateUpdated -> onLinkSignUpStateUpdated(
                viewAction.state
            )
            is AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction -> reportFieldInteraction(
                viewAction.code
            )
            is AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged -> onFormFieldValuesChanged(
                viewAction.formValues,
                viewAction.selectedPaymentMethodCode
            )
            is AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected -> {
                if (selectedPaymentMethodCode.value != viewAction.code) {
                    _selectedPaymentMethodCode.value = viewAction.code
                    reportPaymentMethodTypeSelected(viewAction.code)
                }
            }
        }
    }
}