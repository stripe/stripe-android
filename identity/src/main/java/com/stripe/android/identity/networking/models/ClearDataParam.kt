package com.stripe.android.identity.networking.models

import com.stripe.android.core.networking.toMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class ClearDataParam(
    @SerialName("biometric_consent")
    val biometricConsent: Boolean = false,
    @SerialName("id_document_type")
    val idDocumentType: Boolean = false,
    @SerialName("id_document_front")
    val idDocumentFront: Boolean = false,
    @SerialName("id_document_back")
    val idDocumentBack: Boolean = false
) {
    internal companion object {
        private const val CLEAR_DATA_PARAM = "clear_data"

        /**
         * Create map entry for encoding into x-www-url-encoded string.
         */
        fun ClearDataParam.createCollectedDataParamEntry(json: Json) =
            CLEAR_DATA_PARAM to json.encodeToJsonElement(
                serializer(),
                this
            ).toMap()

        internal val CONSENT_TO_DOC_SELECT = ClearDataParam(
            biometricConsent = false,
            idDocumentType = true,
            idDocumentFront = true,
            idDocumentBack = true
        )

        internal val DOC_SELECT_TO_UPLOAD = ClearDataParam(
            biometricConsent = false,
            idDocumentType = false,
            idDocumentFront = true,
            idDocumentBack = true
        )

        internal val UPLOAD_TO_CONFIRM = ClearDataParam(
            biometricConsent = false,
            idDocumentType = false,
            idDocumentFront = false,
            idDocumentBack = false
        )
    }
}
