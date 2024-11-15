package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel

internal class BankFormInteractor private constructor(
    private val updateSelection: (PaymentSelection.New.USBankAccount?) -> Unit,
    val promoBadgeInteractor: PromoBadgeInteractor,
) {

    fun handleLinkedBankAccountChanged(selection: PaymentSelection.New.USBankAccount?) {
        updateSelection(selection)

        val promoBadgeVisible = selection == null
        promoBadgeInteractor.updateIncentiveVisibility(promoBadgeVisible)
    }

    companion object {

        fun create(viewModel: BaseSheetViewModel): BankFormInteractor {
            return BankFormInteractor(
                updateSelection = viewModel::updateSelection,
                promoBadgeInteractor = PromoBadgeInteractor.create(viewModel),
            )
        }
    }
}
