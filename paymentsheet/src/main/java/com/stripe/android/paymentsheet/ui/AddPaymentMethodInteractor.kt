package com.stripe.android.paymentsheet.ui

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal interface AddPaymentMethodInteractor {
    val isLiveMode: Boolean

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
            val selectedPaymentMethodCode: PaymentMethodCode
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
    private val coroutineScope: CoroutineScope,
    override val isLiveMode: Boolean,
) : AddPaymentMethodInteractor {

    companion object {
        fun create(sheetViewModel: BaseSheetViewModel): AddPaymentMethodInteractor {
            val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val formHelper = FormHelper.create(sheetViewModel)
            val paymentMethodMetadata = requireNotNull(sheetViewModel.paymentMethodMetadata.value)
            val linkInlineHandler = LinkInlineHandler.create(sheetViewModel, coroutineScope)
            val linkSignupMode = sheetViewModel.linkHandler.linkSignupMode.stateIn(
                scope = coroutineScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )
            return DefaultAddPaymentMethodInteractor(
                initiallySelectedPaymentMethodType = sheetViewModel.initiallySelectedPaymentMethodType,
                linkConfigurationCoordinator = sheetViewModel.linkConfigurationCoordinator,
                selection = sheetViewModel.selection,
                linkSignupMode = linkSignupMode,
                processing = sheetViewModel.processing,
                supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
                createFormArguments = formHelper::createFormArguments,
                formElementsForCode = formHelper::formElementsForCode,
                clearErrorMessages = sheetViewModel::clearErrorMessages,
                onLinkSignUpStateUpdated = linkInlineHandler::onStateUpdated,
                reportFieldInteraction = sheetViewModel.analyticsListener::reportFieldInteraction,
                onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
                reportPaymentMethodTypeSelected = sheetViewModel.eventReporter::onSelectPaymentMethod,
                createUSBankAccountFormArguments = {
                    USBankAccountFormArguments.create(
                        viewModel = sheetViewModel,
                        hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
                        selectedPaymentMethodCode = it
                    )
                },
                coroutineScope = coroutineScope,
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
            )
        }
    }

    private val _selectedPaymentMethodCode: MutableStateFlow<String> =
        MutableStateFlow(initiallySelectedPaymentMethodType)
    private val selectedPaymentMethodCode: StateFlow<String> = _selectedPaymentMethodCode

    private val _state: MutableStateFlow<AddPaymentMethodInteractor.State> = MutableStateFlow(
        getInitialState()
    )
    override val state: StateFlow<AddPaymentMethodInteractor.State> = _state

    private fun getInitialState(): AddPaymentMethodInteractor.State {
        val selectedPaymentMethodCode = selectedPaymentMethodCode.value
        val linkSignupMode = linkSignupMode.value

        return AddPaymentMethodInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            supportedPaymentMethods = supportedPaymentMethods,
            arguments = createFormArguments(selectedPaymentMethodCode),
            formElements = formElementsForCode(selectedPaymentMethodCode),
            paymentSelection = selection.value,
            linkSignupMode = linkSignupMode,
            linkInlineSignupMode = linkSignupMode.takeIf {
                shouldHaveLinkInlineSignup(selectedPaymentMethodCode)
            },
            processing = processing.value,
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

        coroutineScope.launch {
            selectedPaymentMethodCode.collect { newSelectedPaymentMethodCode ->
                val newFormArguments = createFormArguments(newSelectedPaymentMethodCode)
                val newFormElements = formElementsForCode(newSelectedPaymentMethodCode)
                val newLinkInlineSignupMode = linkSignupMode.value.takeIf {
                    shouldHaveLinkInlineSignup(newSelectedPaymentMethodCode)
                }
                val newUsBankAccountFormArguments = createUSBankAccountFormArguments(newSelectedPaymentMethodCode)

                _state.value = _state.value.copy(
                    selectedPaymentMethodCode = newSelectedPaymentMethodCode,
                    arguments = newFormArguments,
                    formElements = newFormElements,
                    linkInlineSignupMode = newLinkInlineSignupMode,
                    usBankAccountFormArguments = newUsBankAccountFormArguments
                )
            }
        }

        coroutineScope.launch {
            selection.collect {
                _state.value = _state.value.copy(
                    paymentSelection = it
                )
            }
        }

        coroutineScope.launch {
            linkSignupMode.collect {
                _state.value = _state.value.copy(
                    linkSignupMode = it,
                    linkInlineSignupMode = it.takeIf {
                        shouldHaveLinkInlineSignup(_state.value.selectedPaymentMethodCode)
                    }
                )
            }
        }

        coroutineScope.launch {
            processing.collect {
                _state.value = _state.value.copy(
                    processing = it
                )
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

    private fun shouldHaveLinkInlineSignup(selectedPaymentMethodCode: PaymentMethodCode): Boolean {
        return selectedPaymentMethodCode == PaymentMethod.Type.Card.code
    }
}
