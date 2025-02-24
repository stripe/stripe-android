package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AuthorizationRepairResponse(
    val id: String,
    val url: String,
    val flow: String,
    val institution: FinancialConnectionsInstitution,
    val display: Display,
    @SerialName("is_oauth")
    val isOAuth: Boolean,
)
