package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod.Type.Link
import com.stripe.android.model.PaymentMethod.Type.USBankAccount
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.LinkInlineHandler
import com.stripe.android.paymentsheet.forms.FormFieldValues
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
import kotlinx.coroutines.flow.update

internal interface VerticalModeFormInteractor {
    val isLiveMode: Boolean

    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun canGoBack(): Boolean

    fun updateWithResult(result: Any?)

    fun close()

    data class State(
        val selectedPaymentMethodCode: String,
        val isProcessing: Boolean,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val formArguments: FormArguments,
        val formElements: List<FormElement>,
        val headerInformation: FormHeaderInformation?,
        val intermediateResults: Map<PaymentMethodCode, Any> = emptyMap(),
    ) {

        val continueBeforeConfirmation: Boolean
            get() = selectedPaymentMethodCode in setOf(USBankAccount.code, Link.code) &&
                selectedPaymentMethodCode !in intermediateResults
    }

    sealed interface ViewAction {
        data object FieldInteraction : ViewAction
        data class FormFieldValuesChanged(val formValues: FormFieldValues?) : ViewAction
    }
}

internal class DefaultVerticalModeFormInteractor(
    private val selectedPaymentMethodCode: String,
    private val formArguments: FormArguments,
    private val formElements: (Any?) -> List<FormElement>,
    private val onFormFieldValuesChanged: (formValues: FormFieldValues?, selectedPaymentMethodCode: String) -> Unit,
    private val usBankAccountArguments: USBankAccountFormArguments,
    private val reportFieldInteraction: (String) -> Unit,
    private val headerInformation: FormHeaderInformation?,
    private val canGoBackDelegate: () -> Boolean,
    override val isLiveMode: Boolean,
    processing: StateFlow<Boolean>,
    private val coroutineScope: CoroutineScope,
    private val intermediateResults: Map<PaymentMethodCode, Any>,
) : VerticalModeFormInteractor {

    private val _state = MutableStateFlow(
        VerticalModeFormInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            isProcessing = false,
            usBankAccountFormArguments = usBankAccountArguments,
            formArguments = formArguments,
            formElements = formElements(intermediateResults[selectedPaymentMethodCode]),
            headerInformation = headerInformation,
            intermediateResults = intermediateResults,
        )
    )

    override val state: StateFlow<VerticalModeFormInteractor.State> = combineAsStateFlow(
        _state,
        processing,
    ) { state, processing ->
        state.copy(isProcessing = processing)
    }

    override fun handleViewAction(viewAction: VerticalModeFormInteractor.ViewAction) {
        when (viewAction) {
            VerticalModeFormInteractor.ViewAction.FieldInteraction -> {
                reportFieldInteraction(selectedPaymentMethodCode)
            }
            is VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged -> {
                onFormFieldValuesChanged(viewAction.formValues, selectedPaymentMethodCode)
            }
        }
    }

    override fun canGoBack(): Boolean {
        return canGoBackDelegate()
    }

    override fun updateWithResult(result: Any?) {
        val elements = formElements(result)
        _state.update { state ->
            state.copy(
                formElements = elements,
                intermediateResults = if (result != null) {
                    state.intermediateResults + (selectedPaymentMethodCode to result)
                } else {
                    state.intermediateResults - selectedPaymentMethodCode
                },
            )
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }

    companion object {
        fun create(
            selectedPaymentMethodCode: String,
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            intermediateResults: Map<PaymentMethodCode, Any>,
        ): VerticalModeFormInteractor {
            val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val formHelper = FormHelper.create(
                viewModel = viewModel,
                linkInlineHandler = LinkInlineHandler.create(viewModel, coroutineScope),
                paymentMethodMetadata = paymentMethodMetadata
            )
            return DefaultVerticalModeFormInteractor(
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                formArguments = formHelper.createFormArguments(selectedPaymentMethodCode),
                formElements = { formHelper.formElementsForCode(selectedPaymentMethodCode, it) },
                onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
                usBankAccountArguments = USBankAccountFormArguments.create(
                    viewModel = viewModel,
                    paymentMethodMetadata = paymentMethodMetadata,
                    hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
                    selectedPaymentMethodCode = selectedPaymentMethodCode
                ),
                headerInformation = paymentMethodMetadata.formHeaderInformationForCode(
                    selectedPaymentMethodCode,
                    customerHasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                        it.type?.code == selectedPaymentMethodCode
                    },
                ),
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
                canGoBackDelegate = { viewModel.navigationHandler.canGoBack },
                processing = viewModel.processing,
                reportFieldInteraction = viewModel.analyticsListener::reportFieldInteraction,
                coroutineScope = coroutineScope,
                intermediateResults = intermediateResults,
            )
        }
    }
}
