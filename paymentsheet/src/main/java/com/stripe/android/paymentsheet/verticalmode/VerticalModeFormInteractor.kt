package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal interface VerticalModeFormInteractor {
    val isLiveMode: Boolean

    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

    data class State(
        val selectedPaymentMethodCode: String,
        val isProcessing: Boolean,
        private val isValidating: Boolean,
        val usBankAccountFormArguments: USBankAccountFormArguments,
        val formArguments: FormArguments,
        val showsWalletHeader: Boolean,
        private val formElements: List<FormElement>,
        val headerInformation: FormHeaderInformation?,
    ) {
        val formUiElements = formElements.onEach { element ->
            element.onValidationStateChanged(isValidating)
        }
    }

    sealed interface ViewAction {
        data object FieldInteraction : ViewAction
        data class FormFieldValuesChanged(val formValues: FormFieldValues?) : ViewAction
    }
}

internal class DefaultVerticalModeFormInteractor(
    private val selectedPaymentMethodCode: String,
    private val formArguments: FormArguments,
    private val formElements: List<FormElement>,
    private val onFormFieldValuesChanged: (formValues: FormFieldValues?, selectedPaymentMethodCode: String) -> Unit,
    private val usBankAccountArguments: USBankAccountFormArguments,
    private val reportFieldInteraction: (String) -> Unit,
    private val showsWalletHeader: StateFlow<Boolean>,
    private val headerInformation: FormHeaderInformation?,
    override val isLiveMode: Boolean,
    processing: StateFlow<Boolean>,
    validationRequested: SharedFlow<Unit>,
    paymentMethodIncentive: StateFlow<PaymentMethodIncentive?>,
    private val coroutineScope: CoroutineScope,
    private val uiContext: CoroutineContext,
) : VerticalModeFormInteractor {
    private val isValidating = MutableStateFlow(false)

    override val state: StateFlow<VerticalModeFormInteractor.State> = combineAsStateFlow(
        processing,
        paymentMethodIncentive,
        isValidating,
        showsWalletHeader,
    ) { isProcessing, paymentMethodIncentive, isValidating, showsWalletHeader ->
        VerticalModeFormInteractor.State(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            isProcessing = isProcessing,
            usBankAccountFormArguments = usBankAccountArguments,
            formArguments = formArguments,
            formElements = formElements,
            isValidating = isValidating,
            showsWalletHeader = showsWalletHeader,
            headerInformation = headerInformation?.copy(
                promoBadge = paymentMethodIncentive?.takeIfMatches(selectedPaymentMethodCode)?.displayText,
            )?.takeIf { !showsWalletHeader },
        )
    }

    init {
        coroutineScope.launch {
            validationRequested.collect {
                withContext(uiContext) {
                    isValidating.value = true
                }
            }
        }
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

    override fun close() {
        coroutineScope.cancel()
    }

    companion object {
        fun create(
            selectedPaymentMethodCode: String,
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            customerStateHolder: CustomerStateHolder,
            bankFormInteractor: BankFormInteractor,
        ): VerticalModeFormInteractor {
            val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            val formHelper = DefaultFormHelper.create(
                viewModel = viewModel,
                paymentMethodMetadata = paymentMethodMetadata,
                shouldCreateAutomaticallyLaunchedCardScanFormDataHelper = true,
            )
            return DefaultVerticalModeFormInteractor(
                selectedPaymentMethodCode = selectedPaymentMethodCode,
                formArguments = formHelper.createFormArguments(selectedPaymentMethodCode),
                formElements = formHelper.formElementsForCode(selectedPaymentMethodCode),
                onFormFieldValuesChanged = formHelper::onFormFieldValuesChanged,
                usBankAccountArguments = USBankAccountFormArguments.create(
                    viewModel = viewModel,
                    paymentMethodMetadata = paymentMethodMetadata,
                    hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
                    selectedPaymentMethodCode = selectedPaymentMethodCode,
                    bankFormInteractor = bankFormInteractor,
                ),
                showsWalletHeader = viewModel.walletsState.mapAsStateFlow { it != null },
                headerInformation = paymentMethodMetadata.formHeaderInformationForCode(
                    selectedPaymentMethodCode,
                    customerHasSavedPaymentMethods = customerStateHolder.paymentMethods.value.any {
                        it.type?.code == selectedPaymentMethodCode
                    },
                ),
                isLiveMode = paymentMethodMetadata.stripeIntent.isLiveMode,
                processing = viewModel.processing,
                paymentMethodIncentive = bankFormInteractor.paymentMethodIncentiveInteractor.displayedIncentive,
                reportFieldInteraction = viewModel.analyticsListener::reportFieldInteraction,
                validationRequested = viewModel.validationRequested,
                coroutineScope = coroutineScope,
                uiContext = Dispatchers.Main,
            )
        }
    }
}
