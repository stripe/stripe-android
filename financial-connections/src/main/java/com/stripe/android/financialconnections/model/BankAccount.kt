package com.stripe.android.financialconnections.model

import androidx.annotation.RestrictTo
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Poko
class BankAccount internal constructor(

    @SerialName(value = "id") @Required
    override val id: String,

    @SerialName(value = "last4") @Required
    val last4: String,

    @SerialName(value = "bank_name") val bankName: String? = null,

    @SerialName(value = "routing_number") val routingNumber: String? = null

) : PaymentAccount()
