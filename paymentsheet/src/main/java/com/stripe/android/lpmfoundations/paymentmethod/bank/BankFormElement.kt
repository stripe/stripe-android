package com.stripe.android.lpmfoundations.paymentmethod.bank

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormArgumentsFactory
import com.stripe.android.paymentsheet.model.PaymentSelection.New
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountEmitters
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountForm
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import com.stripe.android.ui.core.elements.RenderableFormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow

internal class BankFormElement(
    private val formArgs: FormArguments,
    private val usBankAccountFormArgs: USBankAccountFormArguments,
) : RenderableFormElement(
    allowsUserInteraction = true,
    identifier = IdentifierSpec.Generic("bank_form")
) {

    override fun getFormFieldValueFlow(): StateFlow<List<Pair<IdentifierSpec, FormFieldEntry>>> {
        return stateFlowOf(emptyList())
    }

    @Composable
    override fun ComposeUI(enabled: Boolean) {
        val viewModel = viewModel<USBankAccountFormViewModel>(
            factory = USBankAccountFormViewModel.Factory {
                makeViewModelArgs()
            },
        )

        USBankAccountEmitters(
            viewModel = viewModel,
            usBankAccountFormArgs = usBankAccountFormArgs,
        )

        USBankAccountForm(
            viewModel = viewModel,
            formArgs = formArgs,
            usBankAccountFormArgs = usBankAccountFormArgs,
        )
    }

    private fun makeViewModelArgs(): USBankAccountFormViewModel.Args {
        return USBankAccountFormViewModel.Args(
            instantDebits = usBankAccountFormArgs.instantDebits,
            linkMode = usBankAccountFormArgs.linkMode,
            formArgs = formArgs,
            hostedSurface = usBankAccountFormArgs.hostedSurface,
            showCheckbox = usBankAccountFormArgs.showCheckbox,
            isCompleteFlow = usBankAccountFormArgs.isCompleteFlow,
            isPaymentFlow = usBankAccountFormArgs.isPaymentFlow,
            stripeIntentId = usBankAccountFormArgs.stripeIntentId,
            clientSecret = usBankAccountFormArgs.clientSecret,
            onBehalfOf = usBankAccountFormArgs.onBehalfOf,
            savedPaymentMethod = usBankAccountFormArgs.draftPaymentSelection as? New.USBankAccount,
            shippingDetails = usBankAccountFormArgs.shippingDetails,
        )
    }

    companion object {

        fun create(
            usBankAccountFormArgs: USBankAccountFormArguments,
            metadata: PaymentMethodMetadata,
        ): BankFormElement {
            val formArgs = FormArgumentsFactory.create(
                paymentMethodCode = if (usBankAccountFormArgs.instantDebits) {
                    PaymentMethod.Type.Link.code
                } else {
                    PaymentMethod.Type.USBankAccount.code
                },
                metadata = metadata,
            )

            return BankFormElement(
                formArgs = formArgs,
                usBankAccountFormArgs = usBankAccountFormArgs,
            )
        }
    }
}
