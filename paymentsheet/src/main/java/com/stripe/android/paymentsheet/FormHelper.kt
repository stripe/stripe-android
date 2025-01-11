package com.stripe.android.paymentsheet

import androidx.lifecycle.viewModelScope
import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class FormHelper(
    private val coroutineScope: CoroutineScope,
    private val cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val newPaymentSelectionProvider: () -> NewOrExternalPaymentSelection?,
    private val selectionUpdater: (PaymentSelection?) -> Unit,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
) {
    private val lastFormValues = MutableSharedFlow<Pair<FormFieldValues?, String>>()
    private val lastSignupState = MutableStateFlow<InlineSignupViewState?>(null)
    private val paymentSelection: Flow<PaymentSelection?> = combine(
        lastFormValues,
        lastSignupState,
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
            paymentSelection.collectLatest {
                selectionUpdater(it)
            }
        }
    }

    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            paymentMethodMetadata: PaymentMethodMetadata
        ): FormHelper {
            return FormHelper(
                coroutineScope = viewModel.viewModelScope,
                cardAccountRangeRepositoryFactory = viewModel.cardAccountRangeRepositoryFactory,
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = {
                    viewModel.newPaymentSelection
                },
                linkConfigurationCoordinator = viewModel.linkHandler.linkConfigurationCoordinator,
                selectionUpdater = {
                    viewModel.updateSelection(it)
                }
            )
        }
    }

    fun formElementsForCode(code: String): List<FormElement> {
        val currentSelection = newPaymentSelectionProvider()?.takeIf { it.getType() == code }

        return paymentMethodMetadata.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                onLinkInlineSignupStateChanged = {
                    lastSignupState.value = it
                },
                paymentMethodCreateParams = currentSelection?.getPaymentMethodCreateParams(),
                paymentMethodExtraParams = currentSelection?.getPaymentMethodExtraParams(),
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
        coroutineScope.launch {
            lastFormValues.emit(formValues to selectedPaymentMethodCode)
        }
    }

    fun requiresFormScreen(selectedPaymentMethodCode: String): Boolean {
        val userInteractionAllowed = formElementsForCode(selectedPaymentMethodCode).any { it.allowsUserInteraction }
        return userInteractionAllowed ||
            selectedPaymentMethodCode == PaymentMethod.Type.USBankAccount.code ||
            selectedPaymentMethodCode == PaymentMethod.Type.Link.code
    }

    private fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(paymentMethodMetadata.supportedPaymentMethodForCode(code = code))
    }
}
