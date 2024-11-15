package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.toPaymentMethodIncentive
import com.stripe.android.paymentsheet.viewmodels.BaseSheetViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PromoBadgeInteractor private constructor(
    private val incentive: PaymentMethodIncentive?,
) {

    private val _displayedIncentive = MutableStateFlow(incentive)
    val displayedIncentive: StateFlow<PaymentMethodIncentive?> = _displayedIncentive.asStateFlow()

    fun updateIncentiveVisibility(visible: Boolean) {
        _displayedIncentive.value = incentive.takeIf { visible }
    }

    companion object {

        fun create(viewModel: BaseSheetViewModel): PromoBadgeInteractor {
            return PromoBadgeInteractor(
                incentive = viewModel.paymentMethodMetadata.value?.consumerIncentive?.toPaymentMethodIncentive(),
//                selection = viewModel.selection,
//                updateSelection = viewModel::updateSelection,
            )
        }
    }
}
