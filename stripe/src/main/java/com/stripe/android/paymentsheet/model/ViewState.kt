package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult

sealed class ViewState {
    data class Ready(
        val amount: Long,
        val currencyCode: String
    ) : ViewState()

    object Confirming : ViewState()

    data class Completed(
        val paymentIntentResult: PaymentIntentResult
    ) : ViewState()
}
