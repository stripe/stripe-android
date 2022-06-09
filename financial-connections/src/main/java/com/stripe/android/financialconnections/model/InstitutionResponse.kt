package com.stripe.android.financialconnections.model

import kotlinx.serialization.Serializable

@Serializable
internal data class InstitutionResponse(
    val data: List<Institution>
)

@Serializable
internal data class Institution(
    val id: String,
    val name: String
)
