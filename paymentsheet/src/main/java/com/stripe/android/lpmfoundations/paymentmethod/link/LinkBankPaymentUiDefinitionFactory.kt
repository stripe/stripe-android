package com.stripe.android.lpmfoundations.paymentmethod.link

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.lpmfoundations.paymentmethod.bank.BankFormElement
import com.stripe.android.lpmfoundations.paymentmethod.definitions.InstantDebitsDefinition
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.ui.core.R as StripeUiCoreR

internal object LinkBankPaymentUiDefinitionFactory : UiDefinitionFactory.Simple {

    override fun createSupportedPaymentMethod(): SupportedPaymentMethod {
        return SupportedPaymentMethod(
            code = InstantDebitsDefinition.type.code,
            displayNameResource = StripeUiCoreR.string.stripe_paymentsheet_payment_method_instant_debits,
            iconResource = StripeUiCoreR.drawable.stripe_ic_paymentsheet_pm_bank,
            iconRequiresTinting = true,
            lightThemeIconUrl = null,
            darkThemeIconUrl = null,
        )
    }

    override fun createFormElements(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val bankFormElement = BankFormElement(
            formArguments = arguments.formArguments,
            usBankAccountFormArgs = arguments.usBankAccountFormArguments,
        )

        return FormElementsBuilder(arguments)
            .element(bankFormElement)
            .build()
    }
}
