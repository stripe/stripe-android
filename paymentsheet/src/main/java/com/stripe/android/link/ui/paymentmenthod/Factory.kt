package com.stripe.android.link.ui.paymentmenthod

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor

internal object Factory {
    fun paymentMethodMetadata(
        configuration: LinkConfiguration
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata.create(
            configuration = configuration,
            sharedDataSpecs = emptyList(),
            externalPaymentMethodSpecs = emptyList(),
            linkInlineConfiguration = null
        )
    }

    fun formHelper(
        configuration: LinkConfiguration,
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory
    ): FormHelper {
        return FormHelper.create(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata(configuration)
        )
    }

    fun usBankAccountArguments(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata
    ): USBankAccountFormArguments {
        return USBankAccountFormArguments.create(
            configuration = configuration,
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = "card",
            bankFormInteractor = BankFormInteractor.create {  }
        )
    }
}