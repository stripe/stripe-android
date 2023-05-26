package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentDocumentCapturePage(
    @SerialName("autocapture_timeout")
    val autocaptureTimeout: Int,
    @SerialName("file_purpose")
    val filePurpose: String,
    // Override the compression quality
    // @SerialName("high_res_image_compression_quality")
    val highResImageCompressionQuality: Float = 0.99f,
    @SerialName("high_res_image_crop_padding")
    val highResImageCropPadding: Float,
    @SerialName("high_res_image_max_dimension")
    val highResImageMaxDimension: Int,
    // Override the compression quality
    // @SerialName("low_res_image_compression_quality")
    val lowResImageCompressionQuality: Float = 0.98f,
    @SerialName("low_res_image_max_dimension")
    val lowResImageMaxDimension: Int,
    @SerialName("models")
    val models: VerificationPageStaticContentDocumentCaptureModels,
    @SerialName("require_live_capture")
    val requireLiveCapture: Boolean,
    @SerialName("motion_blur_min_duration")
    val motionBlurMinDuration: Int,
    @SerialName("motion_blur_min_iou")
    val motionBlurMinIou: Float
) : Parcelable
