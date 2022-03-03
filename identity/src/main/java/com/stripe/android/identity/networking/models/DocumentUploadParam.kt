package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DocumentUploadParam(
    @SerialName("back_score")
    val backScore: Float? = null,
    @SerialName("front_card_score")
    val frontCardScore: Float? = null,
    @SerialName("high_res_image")
    val highResImage: String,
    @SerialName("invalid_score")
    val invalidScore: Float? = null,
    @SerialName("low_res_image")
    val lowResImage: String? = null,
    @SerialName("no_document_score")
    val noDocumentScore: Float? = null,
    @SerialName("passport_score")
    val passportScore: Float? = null,
    @SerialName("upload_method")
    val uploadMethod: UploadMethod
) {
    @Serializable
    internal enum class UploadMethod {
        @SerialName("auto_capture")
        AUTOCAPTURE,

        @SerialName("file_upload")
        FILEUPLOAD,

        @SerialName("manual_capture")
        MANUALCAPTURE;
    }
}
