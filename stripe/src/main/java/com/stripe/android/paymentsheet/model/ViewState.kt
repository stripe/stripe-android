package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentOptionResult

internal sealed class ViewState {

    internal sealed class PaymentSheet : ViewState() {
        data class Ready(
            val amount: Long,
            val currencyCode: String
        ) : PaymentSheet()

        object StartProcessing : PaymentSheet()

        data class FinishProcessing(
            val result: PaymentIntentResult
        ) : PaymentSheet()
    }

    internal sealed class PaymentOptions : ViewState() {
        object Ready : PaymentOptions()

        object StartProcessing : PaymentOptions()

        // Completed indicates the animation should show it completed
        data class FinishProcessing(
            val result: PaymentOptionResult
        ) : PaymentOptions()

        // Finished indicates the activity should be closed immediately
        data class Finished(
            val result: PaymentOptionResult
        ) : PaymentOptions()
    }
}
