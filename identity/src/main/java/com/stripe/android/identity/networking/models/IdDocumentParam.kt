package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class IdDocumentParam(
    @SerialName("back")
    val back: DocumentUploadParam? = null,
    @SerialName("front")
    val front: DocumentUploadParam? = null,
    @SerialName("type")
    val type: CollectedDataParam.Type? = null
)
