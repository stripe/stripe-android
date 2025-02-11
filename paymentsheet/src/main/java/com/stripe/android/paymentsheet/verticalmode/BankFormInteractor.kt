package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal class BankFormInteractor(
    private val updateSelection: (PaymentSelection.New.USBankAccount?) -> Unit,
    val paymentMethodIncentiveInteractor: PaymentMethodIncentiveInteractor,
) {

    fun handleLinkedBankAccountChanged(selection: PaymentSelection.New.USBankAccount?) {
        updateSelection(selection)
        paymentMethodIncentiveInteractor.setEligible(selection == null)
    }

    companion object {

        fun create(viewModel: BaseSheetViewModel): BankFormInteractor {
            return BankFormInteractor(
                updateSelection = viewModel::updateSelection,
                paymentMethodIncentiveInteractor = PaymentMethodIncentiveInteractor.create(viewModel),
            )
        }
    }
}
