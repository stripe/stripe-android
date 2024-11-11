package com.stripe.android.lpmfoundations.paymentmethod.bank

import com.stripe.android.lpmfoundations.luxe.FormElementsBuilder
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.UiDefinitionFactory
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormElement

internal object BankFormElementsFactory {

    fun create(
        metadata: PaymentMethodMetadata,
        arguments: UiDefinitionFactory.Arguments,
    ): List<FormElement> {
        val bankFormElement = BankFormElement.create(
            usBankAccountFormArgs = arguments.usBankAccountFormArguments,
            metadata = metadata,
        )

        return FormElementsBuilder(arguments.stripBillingDetails())
            .element(bankFormElement)
            .build()
    }
}

private fun UiDefinitionFactory.Arguments.stripBillingDetails(): UiDefinitionFactory.Arguments {
    return copy(
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Never,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Never,
            attachDefaultsToPaymentMethod = false,
        )
    )
}
