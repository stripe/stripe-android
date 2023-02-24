package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MixedOAuthParams(
    @SerialName(value = "state") val state: String,
    @SerialName(value = "code") val code: String?,
    @SerialName(value = "status") val status: String?,
    @SerialName(value = "public_token") val publicToken: String?
)
