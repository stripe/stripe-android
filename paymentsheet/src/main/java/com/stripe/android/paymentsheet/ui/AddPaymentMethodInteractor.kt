package com.stripe.android.paymentsheet.ui

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal interface AddPaymentMethodInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

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
        data class OnPaymentMethodSelected(val code: PaymentMethodCode) : ViewAction()
        data class OnLinkSignUpStateUpdated(val state: InlineSignupViewState) : ViewAction()
        data class OnFormFieldValuesChanged(
            val formValues: FormFieldValues?,
            val selectedPaymentMethodCode: String
        ) : ViewAction()

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
    dispatcher: CoroutineContext = Dispatchers.Default,
) : AddPaymentMethodInteractor {

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

    private val coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    private val _selectedPaymentMethodCode: MutableStateFlow<String> =
        MutableStateFlow(initiallySelectedPaymentMethodType)
    private val selectedPaymentMethodCode: StateFlow<String> = _selectedPaymentMethodCode

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

    init {
        coroutineScope.launch {
            selection.collect {
                clearErrorMessages()
            }
        }
    }

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        when (viewAction) {
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

    override fun close() {
        coroutineScope.cancel()
    }
}

internal class UnsupportedAddPaymentMethodInteractor(private val errorReporter: ErrorReporter) :
    AddPaymentMethodInteractor {

    private val errorFieldFunctionAccessed = "function_accessed"

    override val state: StateFlow<AddPaymentMethodInteractor.State>
        get() {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.UNSUPPORTED_ADD_PAYMENT_METHOD_INTERACTOR_USED,
                additionalNonPiiParams = mapOf(errorFieldFunctionAccessed to "state")
            )
            throw UnsupportedOperationException("Attempting to use UnsupportedAddPaymentMethodInteractor")
        }

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        errorReporter.report(
            ErrorReporter.UnexpectedErrorEvent.UNSUPPORTED_ADD_PAYMENT_METHOD_INTERACTOR_USED,
            additionalNonPiiParams = mapOf(errorFieldFunctionAccessed to "handleViewAction_$viewAction")
        )
    }

    override fun close() {
        errorReporter.report(
            ErrorReporter.UnexpectedErrorEvent.UNSUPPORTED_ADD_PAYMENT_METHOD_INTERACTOR_USED,
            additionalNonPiiParams = mapOf(errorFieldFunctionAccessed to "close")
        )
    }
}
