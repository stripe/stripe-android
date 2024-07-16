package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.FormHeaderInformation
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.bankaccount.CollectBankAccountLauncher
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

internal interface VerticalModeFormInteractor {
    val state: StateFlow<State>

    fun handleViewAction(viewAction: ViewAction)

    fun close()

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
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val linkInlineHandler = LinkInlineHandler.create(viewModel, coroutineScope)
    private val linkSignupMode = viewModel.linkHandler.linkSignupMode.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
    private val formHelper = FormHelper.create(viewModel)
    private val formArguments: FormArguments = formHelper.createFormArguments(selectedPaymentMethodCode)
    private val formElements: List<FormElement> = formHelper.formElementsForCode(selectedPaymentMethodCode)
    private val usBankAccountArguments: USBankAccountFormArguments =
        USBankAccountFormArguments.create(
            viewModel = viewModel,
            hostedSurface = CollectBankAccountLauncher.HOSTED_SURFACE_PAYMENT_ELEMENT,
            selectedPaymentMethodCode = selectedPaymentMethodCode
        )

    override val state: StateFlow<VerticalModeFormInteractor.State> = combineAsStateFlow(
        viewModel.processing,
        linkSignupMode,
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
                viewModel.analyticsListener.reportFieldInteraction(selectedPaymentMethodCode)
            }
            is VerticalModeFormInteractor.ViewAction.FormFieldValuesChanged -> {
                formHelper.onFormFieldValuesChanged(viewAction.formValues, selectedPaymentMethodCode)
            }
            is VerticalModeFormInteractor.ViewAction.LinkSignupStateChanged -> {
                linkInlineHandler.onStateUpdated(viewAction.linkInlineSignupViewState)
            }
        }
    }

    override fun close() {
        coroutineScope.cancel()
    }
}
