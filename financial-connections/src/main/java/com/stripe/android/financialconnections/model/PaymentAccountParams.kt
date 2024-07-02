package com.stripe.android.financialconnections.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal sealed class PaymentAccountParams(val type: String) : Parcelable {

    abstract fun toParamMap(): Map<String, String>

    @Parcelize
    data class LinkedAccount(
        val id: String
    ) : PaymentAccountParams("linked_account") {
        override fun toParamMap(): Map<String, String> = mapOf(
            "type" to type,
            "$type[id]" to id
        )
    }

    @Parcelize
    data class BankAccount(
        val routingNumber: String,
        val accountNumber: String
    ) : PaymentAccountParams("bank_account") {
        override fun toParamMap(): Map<String, String> = mapOf(
            "type" to type,
            "$type[routing_number]" to routingNumber,
            "$type[account_number]" to accountNumber
        )
    }
}
