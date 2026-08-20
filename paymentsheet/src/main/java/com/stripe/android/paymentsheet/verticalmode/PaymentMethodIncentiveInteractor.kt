package com.stripe.android.paymentsheet.verticalmode

import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class PaymentMethodIncentiveInteractor(
    private val incentive: PaymentMethodIncentive?,
) {

    private val _displayedIncentive = MutableStateFlow(incentive)
    val displayedIncentive: StateFlow<PaymentMethodIncentive?> = _displayedIncentive.asStateFlow()

    fun setEligible(eligible: Boolean) {
        _displayedIncentive.value = if (eligible) incentive else null
    }
}
