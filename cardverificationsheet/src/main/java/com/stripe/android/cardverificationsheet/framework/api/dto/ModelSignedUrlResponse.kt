package com.stripe.android.cardverificationsheet.framework.api.dto

import androidx.annotation.RestrictTo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ModelSignedUrlResponse(
    @SerialName("model_url") val modelUrl: String
)
