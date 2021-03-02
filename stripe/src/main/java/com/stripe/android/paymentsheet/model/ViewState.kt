package com.stripe.android.paymentsheet.model

import com.stripe.android.PaymentIntentResult

sealed class ViewState {

    abstract fun isReady() : Boolean

    sealed class Buy: ViewState(){
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

    sealed class Add: ViewState(){
        object Ready : Add() {
            override fun isReady(): Boolean = true
        }

        object Confirming : Add() {
            override fun isReady(): Boolean = false
        }

        data class Completed(
            val result: PaymentSelection
        ) : Add() {
            override fun isReady(): Boolean = false
        }
    }
}
