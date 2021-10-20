package com.stripe.android.cardverificationsheet.framework.api.dto

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ValidateApiKeyResponse(
    @SerialName("is_valid_api_key") val isApiKeyValid: Boolean,
    @SerialName("invalid_key_reason") val keyInvalidReason: String?
)
