package com.stripe.android.financialconnections.model

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NetworkedAccountsList(
    @SerialName(value = "data") @Required val data: List<PartnerAccount>,

    @SerialName(value = "display") val display: Display? = null
)
