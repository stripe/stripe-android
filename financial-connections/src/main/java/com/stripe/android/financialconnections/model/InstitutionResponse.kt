package com.stripe.android.financialconnections.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class InstitutionResponse(
    val data: List<Institution>
)

@Serializable
internal data class Institution(
    val id: String,
    val name: String,
    val url: String?,
    val featured: Boolean,
    @SerialName("featured_order") val featuredOrder: Int?
)
