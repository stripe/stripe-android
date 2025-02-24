package com.stripe.android.paymentsheet

import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.FormHelper.FormType
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.transformToPaymentMethodCreateParams
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class DefaultFormHelper(
    private val coroutineScope: CoroutineScope,
    private val linkInlineHandler: LinkInlineHandler,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val newPaymentSelectionProvider: () -> NewOrExternalPaymentSelection?,
    private val selectionUpdater: (PaymentSelection?) -> Unit,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator?,
) : FormHelper {
    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata,
            linkInlineHandler: LinkInlineHandler = LinkInlineHandler.create()
        ): FormHelper {
            return DefaultFormHelper(
                coroutineScope = viewModel.viewModelScope,
                linkInlineHandler = linkInlineHandler,
                cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = {
                    viewModel.newPaymentSelection
                },
                linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
                selectionUpdater = {
                    viewModel.updateSelection(it)
                },
            )
        }

        fun create(
            coroutineScope: CoroutineScope,
            cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
            paymentMethodMetadata: PaymentMethodMetadata,
        ): FormHelper {
            return DefaultFormHelper(
                coroutineScope = coroutineScope,
                linkInlineHandler = LinkInlineHandler.create(),
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = { null },
                linkConfigurationCoordinator = null,
                selectionUpdater = {},
            )
        }
    }

    private val lastFormValues = MutableSharedFlow<Pair<FormFieldValues?, String>>()

    private val paymentSelection: Flow<PaymentSelection?> = combine(
        lastFormValues,
        linkInlineHandler.linkInlineState,
    ) { formValues, inlineSignupViewState ->
        val paymentSelection = formValues.first?.transformToPaymentSelection(
            paymentMethod = supportedPaymentMethodForCode(formValues.second),
            paymentMethodMetadata = paymentMethodMetadata,
        ) ?: return@combine null

        if (paymentSelection !is PaymentSelection.New.Card) {
            return@combine paymentSelection
        }

        if (inlineSignupViewState != null && inlineSignupViewState.useLink) {
            val userInput = inlineSignupViewState.userInput

            if (userInput != null) {
                PaymentSelection.New.LinkInline(
                    paymentMethodCreateParams = paymentSelection.paymentMethodCreateParams,
                    paymentMethodOptionsParams = paymentSelection.paymentMethodOptionsParams,
                    paymentMethodExtraParams = paymentSelection.paymentMethodExtraParams,
                    customerRequestedSave = paymentSelection.customerRequestedSave,
                    brand = paymentSelection.brand,
                    input = userInput,
                )
            } else {
                null
            }
        } else {
            paymentSelection
        }
    }

    init {
        coroutineScope.launch {
            paymentSelection.collect { selection ->
                selectionUpdater(selection)
            }
        }
    }

    override fun formElementsForCode(code: String): List<FormElement> {
        val currentSelection = newPaymentSelectionProvider()?.takeIf { it.getType() == code }

        return paymentMethodMetadata.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                onLinkInlineSignupStateChanged = linkInlineHandler::onStateUpdated,
                paymentMethodCreateParams = currentSelection?.getPaymentMethodCreateParams(),
                paymentMethodExtraParams = currentSelection?.getPaymentMethodExtraParams(),
                initialLinkUserInput = when (val selection = currentSelection?.paymentSelection) {
                    is PaymentSelection.New.LinkInline -> selection.input
                    else -> null
                }
            ),
        ) ?: emptyList()
    }

    override fun createFormArguments(
        paymentMethodCode: PaymentMethodCode,
    ): FormArguments {
        return FormArgumentsFactory.create(
            paymentMethodCode = paymentMethodCode,
            metadata = paymentMethodMetadata,
        )
    }

    override fun onFormFieldValuesChanged(formValues: FormFieldValues?, selectedPaymentMethodCode: String) {
        coroutineScope.launch {
            lastFormValues.emit(formValues to selectedPaymentMethodCode)
        }
    }

    override fun getPaymentMethodParams(
        formValues: FormFieldValues?,
        selectedPaymentMethodCode: String
    ): PaymentMethodCreateParams? {
        return formValues?.transformToPaymentMethodCreateParams(
            paymentMethodCode = selectedPaymentMethodCode,
            paymentMethodMetadata = paymentMethodMetadata
        )
    }

    private fun requiresFormScreen(paymentMethodCode: String, formElements: List<FormElement>): Boolean {
        val userInteractionAllowed = formElements.any { it.allowsUserInteraction }
        return userInteractionAllowed ||
            paymentMethodCode == PaymentMethod.Type.USBankAccount.code ||
            paymentMethodCode == PaymentMethod.Type.Link.code
    }

    override fun formTypeForCode(paymentMethodCode: PaymentMethodCode): FormType {
        val formElements = formElementsForCode(paymentMethodCode)
        return if (requiresFormScreen(paymentMethodCode, formElements)) {
            FormType.UserInteractionRequired
        } else {
            val mandate = formElements.firstNotNullOfOrNull { it.mandateText }
            if (mandate == null) {
                FormType.Empty
            } else {
                FormType.MandateOnly(mandate)
            }
        }
    }

    private fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(paymentMethodMetadata.supportedPaymentMethodForCode(code = code))
    }
}
