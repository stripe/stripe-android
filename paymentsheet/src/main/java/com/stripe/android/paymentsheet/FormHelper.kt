package com.stripe.android.paymentsheet

import android.content.Context
import com.stripe.android.cards.DefaultCardAccountRangeRepositoryFactory
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.ui.transformToPaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import com.stripe.android.uicore.elements.FormElement

internal class FormHelper(
    private val context: Context,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val newPaymentSelectionProvider: () -> NewOrExternalPaymentSelection?,
    private val selectionUpdater: (PaymentSelection?) -> Unit,
    private val linkConfigurationCoordinator: LinkConfigurationCoordinator,
    private val onLinkInlineSignupStateChanged: (InlineSignupViewState) -> Unit,
) {
    companion object {
        fun create(
            viewModel: BaseSheetViewModel,
            linkInlineHandler: LinkInlineHandler,
            paymentMethodMetadata: PaymentMethodMetadata
        ): FormHelper {
            return FormHelper(
                context = viewModel.getApplication(),
                paymentMethodMetadata = paymentMethodMetadata,
                newPaymentSelectionProvider = {
                    viewModel.newPaymentSelection
                },
                linkConfigurationCoordinator = viewModel.linkConfigurationCoordinator,
                onLinkInlineSignupStateChanged = linkInlineHandler::onStateUpdated,
                selectionUpdater = {
                    viewModel.updateSelection(it)
                }
            )
        }
    }

    private val cardAccountRangeRepositoryFactory = DefaultCardAccountRangeRepositoryFactory(context)

    fun formElementsForCode(code: String): List<FormElement> {
        val currentSelection = newPaymentSelectionProvider()?.takeIf { it.getType() == code }

        return paymentMethodMetadata.formElementsForCode(
            code = code,
            uiDefinitionFactoryArgumentsFactory = UiDefinitionFactory.Arguments.Factory.Default(
                cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
                linkConfigurationCoordinator = linkConfigurationCoordinator,
                onLinkInlineSignupStateChanged = onLinkInlineSignupStateChanged,
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
        val newSelection = formValues?.transformToPaymentSelection(
            paymentMethod = supportedPaymentMethodForCode(selectedPaymentMethodCode),
            paymentMethodMetadata = paymentMethodMetadata,
        )
        selectionUpdater(newSelection)
    }

    private fun supportedPaymentMethodForCode(code: String): SupportedPaymentMethod {
        return requireNotNull(paymentMethodMetadata.supportedPaymentMethodForCode(code = code, false)) // TODO:
    }
}
