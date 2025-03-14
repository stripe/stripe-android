package com.stripe.android.financialconnections.lite.repository.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SynchronizeSessionResponse(
    @SerialName("manifest")
    val manifest: FinancialConnectionsSessionManifest,
)

@Serializable
internal data class FinancialConnectionsSessionManifest(
    @SerialName(value = "cancel_url")
    val cancelUrl: String? = null,

    @SerialName(value = "hosted_auth_url")
    val hostedAuthUrl: String? = null,

    @SerialName(value = "success_url")
    val successUrl: String? = null,
)
