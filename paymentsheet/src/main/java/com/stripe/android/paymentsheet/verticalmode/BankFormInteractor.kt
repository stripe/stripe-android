package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection

internal class BankFormInteractor(
    private val updateSelection: (PaymentSelection.New.USBankAccount?) -> Unit,
    val paymentMethodIncentiveInteractor: PaymentMethodIncentiveInteractor,
) {

    fun handleLinkedBankAccountChanged(selection: PaymentSelection.New.USBankAccount?) {
        updateSelection(selection)
        paymentMethodIncentiveInteractor.setEligible(selection == null)
    }
}
