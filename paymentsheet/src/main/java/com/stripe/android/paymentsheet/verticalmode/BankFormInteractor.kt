package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PromoBadgesState
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class BankFormInteractor private constructor(
    private val selection: StateFlow<PaymentSelection?>,
    private val updateSelection: (PaymentSelection.New.USBankAccount?) -> Unit,
) {

    private val _promoBadgesState = MutableStateFlow(PromoBadgesState.empty())
    val promoBadgesState: StateFlow<PromoBadgesState> = _promoBadgesState.asStateFlow()

    fun handleLinkedBankAccountChanged(selection: PaymentSelection.New.USBankAccount?) {
        val previousSelection = this.selection.value
        updateSelection(selection)

        val code = (selection ?: previousSelection as? PaymentSelection.New)?.paymentMethodCreateParams?.typeCode
        val promoBadgeVisible = selection == null

        if (code != null) {
            setPromoBadgeVisibility(code, promoBadgeVisible)
        }
    }

    private fun setPromoBadgeVisibility(code: String, visible: Boolean) {
        _promoBadgesState.update {
            it.set(code, visible)
        }
    }

    companion object {

        fun create(viewModel: BaseSheetViewModel): BankFormInteractor {
            return BankFormInteractor(
                selection = viewModel.selection,
                updateSelection = viewModel::updateSelection,
            )
        }
    }
}
