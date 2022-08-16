package com.stripe.android.financialconnections.model

import kotlinx.serialization.Serializable

@Serializable
internal data class InstitutionResponse(
    val data: List<FinancialConnectionsInstitution>
)
