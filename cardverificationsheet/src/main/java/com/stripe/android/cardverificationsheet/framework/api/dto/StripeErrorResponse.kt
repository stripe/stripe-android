package com.stripe.android.cardverificationsheet.framework.api.dto

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class StripeErrorResponse(
    @SerialName("status") val status: String,
    @SerialName("error_code") val errorCode: String,
    @SerialName("error_message") val errorMessage: String,
    @SerialName("error_payload") val errorPayload: String?
)
