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
    val type: Type? = null
) {
    @Serializable
    internal enum class Type {
        @SerialName("driving_license")
        DRIVINGLICENSE,

        @SerialName("id_card")
        IDCARD,

        @SerialName("passport")
        PASSPORT
    }
}
