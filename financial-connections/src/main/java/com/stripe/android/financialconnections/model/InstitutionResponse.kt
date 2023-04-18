package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InstitutionResponse(
    @SerialName("show_manual_entry") val showManualEntry: Boolean? = false,
    @SerialName("data") val data: List<FinancialConnectionsInstitution>
)
