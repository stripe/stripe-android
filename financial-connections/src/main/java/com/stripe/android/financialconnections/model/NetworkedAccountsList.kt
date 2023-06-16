package com.stripe.android.financialconnections.model

import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NetworkedAccountsList(
    @SerialName(value = "data") @Required val data: List<PartnerAccount>,

    @SerialName(value = "url") @Required val url: String,

    @SerialName(value = "count") val count: Int? = null,

    @SerialName(value = "repair_authorization_enabled") val repairAuthorizationEnabled: Boolean,

    @SerialName(value = "total_count") val totalCount: Int? = null,

    @SerialName(value = "display") val display: Display? = null
)
