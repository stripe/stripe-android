package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult
import com.stripe.android.paymentsheet.PaymentOptionResult

internal sealed class ViewState {

    abstract fun isReady() : Boolean

    internal sealed class Buy: ViewState(){
        data class Ready(
            val amount: Long,
            val currencyCode: String
        ) : Buy() {
            override fun isReady(): Boolean = true
        }

        object Confirming : Buy() {
            override fun isReady(): Boolean = false
        }

        data class Completed(
            val result: PaymentIntentResult
        ) : Buy() {
            override fun isReady(): Boolean = false
        }
    }

    internal sealed class Add: ViewState(){
        object Ready : Add() {
            override fun isReady(): Boolean = true
        }

        object Confirming : Add() {
            override fun isReady(): Boolean = false
        }

        data class Completed(
            val result: PaymentOptionResult
        ) : Add() {
            override fun isReady(): Boolean = false
        }
    }
}
