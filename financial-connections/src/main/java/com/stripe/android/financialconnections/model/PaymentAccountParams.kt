package com.stripe.android.financialconnections.model

internal sealed class PaymentAccountParams(val type: String) {

    abstract fun toParamMap(): Map<String, String>

    data class LinkedAccount(
        val id: String
    ) : PaymentAccountParams("linked_account") {
        override fun toParamMap(): Map<String, String> = mapOf(
            "type" to type,
            "$type[id]" to id
        )
    }

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
