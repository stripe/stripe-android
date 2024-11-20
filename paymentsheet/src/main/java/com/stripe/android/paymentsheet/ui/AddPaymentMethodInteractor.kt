package com.stripe.android.paymentsheet.ui

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        val processing: Boolean,
        val incentive: PaymentMethodIncentive?,
        val usBankAccountFormArguments: USBankAccountFormArguments,
    )

    sealed class ViewAction {
        data class OnPaymentMethodSelected(val code: PaymentMethodCode) : ViewAction()
        data class OnFormFieldValuesChanged(
            val formValues: FormFieldValues?,
            val selectedPaymentMethodCode: PaymentMethodCode
        ) : ViewAction()

        data class ReportFieldInteraction(val code: PaymentMethodCode) : ViewAction()
    }
}

internal class DefaultAddPaymentMethodInteractor(
    private val initiallySelectedPaymentMethodType: PaymentMethodCode,
    private val selection: StateFlow<PaymentSelection?>,
    private val processing: StateFlow<Boolean>,
    private val incentive: StateFlow<PaymentMethodIncentive?>,
    private val supportedPaymentMethods: List<SupportedPaymentMethod>,
    private val createFormArguments: (PaymentMethodCode) -> FormArguments,
    private val formElementsForCode: (PaymentMethodCode) -> List<FormElement>,
    private val clearErrorMessages: () -> Unit,
    private val reportFieldInteraction: (PaymentMethodCode) -> Unit,
    private val onFormFieldValuesChanged: (FormFieldValues?, String) -> Unit,
    private val reportPaymentMethodTypeSelected: (PaymentMethodCode) -> Unit,
    private val createUSBankAccountFormArguments: (PaymentMethodCode) -> USBankAccountFormArguments,
    private val coroutineScope: CoroutineScope,
    override val isLiveMode: Boolean,
) : AddPaymentMethodInteractor {

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): AddPaymentMethodInteractor {
            val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val linkInlineHandler = LinkInlineHandler.create(viewModel, coroutineScope)
            val formHelper = FormHelper.create(
                viewModel = viewModel,
                linkInlineHandler = linkInlineHandler,
                paymentMethodMetadata = paymentMethodMetadata
            )
            val bankFormInteractor = BankFormInteractor.create(viewModel)

            return DefaultAddPaymentMethodInteractor(
                initiallySelectedPaymentMethodType = viewModel.initiallySelectedPaymentMethodType,
                selection = viewModel.selection,
                processing = viewModel.processing,
                incentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
                supportedPaymentMethods = paymentMethodMetadata.sortedSupportedPaymentMethods(),
                createFormArguments = formHelper::createFormArguments,
                formElementsForCode = formHelper::formElementsForCode,
                clearErrorMessages = viewModel::clearErrorMessages,
                reportFieldInteraction = viewModel.analyticsListener::reportFieldInteraction,
                onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
                reportPaymentMethodTypeSelected = viewModel.eventReporter::onSelectPaymentMethod,
                createUSBankAccountFormArguments = {
                    USBankAccountFormArguments.create(
                        viewModel = viewModel,
                        paymentMethodMetadata = paymentMethodMetadata,
                        hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
                        selectedPaymentMethodCode = it,
                        bankFormInteractor = bankFormInteractor,
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

        return AddPaymentMethodInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            supportedPaymentMethods = supportedPaymentMethods,
            arguments = createFormArguments(selectedPaymentMethodCode),
            formElements = formElementsForCode(selectedPaymentMethodCode),
            paymentSelection = selection.value,
            processing = processing.value,
            incentive = incentive.value,
            usBankAccountFormArguments = createUSBankAccountFormArguments(selectedPaymentMethodCode),
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
                val newUsBankAccountFormArguments = createUSBankAccountFormArguments(newSelectedPaymentMethodCode)

                _state.value = _state.value.copy(
                    selectedPaymentMethodCode = newSelectedPaymentMethodCode,
                    arguments = newFormArguments,
                    formElements = newFormElements,
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
            processing.collect {
                _state.value = _state.value.copy(
                    processing = it
                )
            }
        }
    }

    override fun handleViewAction(viewAction: AddPaymentMethodInteractor.ViewAction) {
        when (viewAction) {
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
