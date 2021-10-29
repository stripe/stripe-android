package com.stripe.android.cardverificationsheet.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationFrameData(
    @SerialName("full_image_data") val fullImageData: String,
    @SerialName("cropped_center_image_data") val croppedCenterImageData: String,
    @SerialName("ocr_image_data") val ocrImageData: String,
    @SerialName("full_image_original_width") val fullImageOriginalWidth: Int,
    @SerialName("full_image_original_height") val fullImageOriginalHeight: Int,
)

@Serializable
internal data class VerifyFramesRequest(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("verification_frames_data") val verificationFramesData: String,
)

@Serializable
internal class VerifyFramesResult()
