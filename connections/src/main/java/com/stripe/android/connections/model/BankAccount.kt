package com.stripe.android.connections.model

import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class BankAccount(

    @SerialName(value = "id") @Required val id: String,

    @SerialName(value = "last4") @Required val last4: String,

    @SerialName(value = "bank_name") val bankName: String? = null,

    @SerialName(value = "routing_number") val routingNumber: String? = null

) : PaymentAccount()
