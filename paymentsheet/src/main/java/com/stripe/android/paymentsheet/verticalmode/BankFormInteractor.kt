package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal class BankFormInteractor private constructor(
    private val updateSelection: (PaymentSelection.New.USBankAccount?) -> Unit,
) {

    fun handleLinkedBankAccountChanged(selection: PaymentSelection.New.USBankAccount?) {
        updateSelection(selection)
    }

    companion object {

        fun create(viewModel: BaseSheetViewModel): BankFormInteractor {
            return BankFormInteractor(
                updateSelection = viewModel::updateSelection,
            )
        }
    }
}
