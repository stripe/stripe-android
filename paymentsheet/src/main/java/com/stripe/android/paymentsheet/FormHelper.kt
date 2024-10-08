package com.stripe.android.paymentsheet

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory.Arguments.BankAccountAction
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.updateWithResult
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement

internal class FormHelper(
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val newPaymentSelectionProvider: () -> NewOrExternalPaymentSelection?,
    private val selectionUpdater: (PaymentSelection?) -> Unit,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
    private val onBankAccountAction: (BankAccountAction) -> Unit,
) {
    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            linkInlineHandler: LinkInlineHandler,
            paymentMethodMetadata: PaymentMethodMetadata
        ): FormHelper {
            return FormHelper(
                cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = {
                    viewModel.newPaymentSelection
                },
                linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
                onLinkInlineSignupStateChanged = linkInlineHandler::onStateUpdated,
                onBankAccountAction = { action ->
                    when (action) {
                        is BankAccountAction.InstitutionSelected -> {
                            viewModel.handleFinancialInstitutionSelected(action.institutionId)
                        }
                        is BankAccountAction.Remove -> {
                            val screen = viewModel.navigationHandler.currentScreen.value
                            screen.updateWithResult(result = null)
                        }
                    }
                },
                selectionUpdater = {
                    viewModel.updateSelection(it)
                }
            )
        }
    }

    fun formElementsForCode(code: String, result: Any?): List<FormElement> {
        val currentSelection = newPaymentSelectionProvider()?.takeIf { it.getType() == code }

        return paymentMethodMetadata.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                onLinkInlineSignupStateChanged = onLinkInlineSignupStateChanged,
                onBankAccountAction = onBankAccountAction,
                paymentMethodCreateParams = currentSelection?.getPaymentMethodCreateParams(),
                paymentMethodExtraParams = currentSelection?.getPaymentMethodExtraParams(),
                intermediateResult = result,
            ),
        ) ?: emptyList()
    }

    fun createFormArguments(
        paymentMethodCode: PaymentMethodCode,
    ): FormArguments {
        return FormArgumentsFactory.create(
            paymentMethodCode = paymentMethodCode,
            metadata = paymentMethodMetadata,
        )
    }

    fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        val newSelection = formValues?.transformToPaymentSelection(
            paymentMethod = supportedPaymentMethodForCode(selectedPaymentMethodCode),
            paymentMethodMetadata = paymentMethodMetadata,
        )
        selectionUpdater(newSelection)
    }

    fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean {
        val userInteractionAllowed = formElementsForCode(
            code = selectedPaymentMethodCode,
            result = null,
        ).any { it.allowsUserInteraction }
        
        return userInteractionAllowed ||
            selectedPaymentMethodCode == PaymentMethod.Type.USBankAccount.code ||
            selectedPaymentMethodCode == PaymentMethod.Type.Link.code
    }

    private fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(paymentMethodMetadata.supportedPaymentMethodForCode(code = code))
    }
}
