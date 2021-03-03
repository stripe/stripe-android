package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentOptionResult

internal sealed class ViewState {

    abstract fun isReady(): Boolean

    internal sealed class PaymentSheet : ViewState() {
        data class Ready(
            val amount: Long,
            val currencyCode: String
        ) : PaymentSheet() {
            override fun isReady(): Boolean = true
        }

        object Confirming : PaymentSheet() {
            override fun isReady(): Boolean = false
        }

        data class Completed(
            val result: PaymentIntentResult
        ) : PaymentSheet() {
            override fun isReady(): Boolean = false
        }
    }

    internal sealed class PaymentOptions : ViewState() {
        object Ready : PaymentOptions() {
            override fun isReady(): Boolean = true
        }

        object Confirming : PaymentOptions() {
            override fun isReady(): Boolean = false
        }

        data class Completed(
            val result: PaymentOptionResult
        ) : PaymentOptions() {
            override fun isReady(): Boolean = false
        }
    }
}
