package com.stripe.android.stripecardscan.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StripeServerErrorResponse(
    @SerialName("error") val error: StripeServerError
)

@Serializable
internal data class StripeServerError(
    @SerialName("code") val code: String? = null,
    @SerialName("doc_url") val docUrl: String? = null,
    @SerialName("message") val message: String,
    @SerialName("param") val param: String? = null,
    @SerialName("type") val type: String
)
