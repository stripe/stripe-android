package com.stripe.android.financialconnections.model

import kotlinx.serialization.Serializable

@Serializable
data class InstitutionResponse(
    val data: List<Institution>
)

@Serializable
data class Institution(
    val id: String,
    val name: String
)