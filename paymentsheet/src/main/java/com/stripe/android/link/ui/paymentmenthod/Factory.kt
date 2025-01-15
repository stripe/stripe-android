package com.stripe.android.link.ui.paymentmenthod

import com.stripe.android.cards.CardAccountRangeRepository
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.DefaultFormHelper
import com.stripe.android.paymentsheet.FormHelper
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.BankFormInteractor

internal object Factory {
    fun paymentMethodMetadata(
        configuration: LinkConfiguration
    ): PaymentMethodMetadata {
        return PaymentMethodMetadata.create(
            configuration = configuration,
        )
    }

    fun formHelper(
        configuration: LinkConfiguration,
        cardAccountRangeRepositoryFactory: CardAccountRangeRepository.Factory,
        selectionUpdater: UpdateSelection,
    ): FormHelper {
        return DefaultFormHelper.create(
            cardAccountRangeRepositoryFactory = cardAccountRangeRepositoryFactory,
            paymentMethodMetadata = paymentMethodMetadata(configuration),
            selectionUpdater = selectionUpdater
        )
    }

    fun usBankAccountArguments(
        paymentMethodMetadata: PaymentMethodMetadata
    ): USBankAccountFormArguments {
        return USBankAccountFormArguments.create(
            paymentMethodMetadata = paymentMethodMetadata,
            selectedPaymentMethodCode = "card",
            bankFormInteractor = BankFormInteractor.create { }
        )
    }
}
