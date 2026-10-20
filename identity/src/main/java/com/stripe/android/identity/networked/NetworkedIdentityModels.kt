package com.stripe.android.identity.networked

import com.stripe.android.core.model.serializers.EnumIgnoreUnknownSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Deliberately not Parcelable or serializable: credentials belong to one flow in memory.
internal data class NetworkedIdentityCredentials(
    val linkSessionKey: String,
) {
    override fun toString(): String = "NetworkedIdentityCredentials([redacted])"
}

@Serializable
internal data class NetworkedIdentityDocument(
    @SerialName("id")
    val id: String,
    @SerialName("document_type")
    val documentType: NetworkedIdentityDocumentType,
    @SerialName("redacted_document_number")
    val redactedDocumentNumber: String? = null,
    @SerialName("expiration_date")
    val expirationDate: Long? = null,
    @SerialName("live_captured")
    val liveCaptured: Boolean? = null,
) {
    override fun toString(): String = "NetworkedIdentityDocument(type=$documentType)"
}

@Serializable(with = NetworkedIdentityDocumentType.Serializer::class)
internal enum class NetworkedIdentityDocumentType {
    @SerialName("passport")
    PASSPORT,

    @SerialName("driving_license")
    DRIVING_LICENSE,

    @SerialName("id_card")
    ID_CARD,

    @SerialName("unknown")
    UNKNOWN;

    internal object Serializer :
        EnumIgnoreUnknownSerializer<NetworkedIdentityDocumentType>(entries.toTypedArray(), UNKNOWN)
}

@Serializable
internal data class NetworkedIdentityAssociationToken(
    @SerialName("association_token")
    val associationToken: String,
) {
    override fun toString(): String = "NetworkedIdentityAssociationToken([redacted])"
}
