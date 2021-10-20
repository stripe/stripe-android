package com.stripe.android.cardverificationsheet.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class StripeServerErrorResponse(
    @SerialName("error") val error: StripeServerError
)

@Serializable
internal data class StripeServerError(
    @SerialName("message") val message: String,
    @SerialName("type") val type: String,
)
