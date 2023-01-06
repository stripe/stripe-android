package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentSelfieCapturePage(
    @SerialName("autocapture_timeout")
    val autoCaptureTimeout: Int,
    @SerialName("file_purpose")
    val filePurpose: String,
    @SerialName("num_samples")
    val numSamples: Int,
    @SerialName("sample_interval")
    val sampleInterval: Int,
    @SerialName("models")
    val models: VerificationPageStaticContentSelfieModels,
    @SerialName("max_centered_threshold_x")
    val maxCenteredThresholdX: Float,
    @SerialName("max_centered_threshold_y")
    val maxCenteredThresholdY: Float,
    @SerialName("min_edge_threshold")
    val minEdgeThreshold: Float,
    @SerialName("min_coverage_threshold")
    val minCoverageThreshold: Float,
    @SerialName("max_coverage_threshold")
    val maxCoverageThreshold: Float,
    @SerialName("low_res_image_max_dimension")
    val lowResImageMaxDimension: Int,
    @SerialName("low_res_image_compression_quality")
    val lowResImageCompressionQuality: Float,
    @SerialName("high_res_image_max_dimension")
    val highResImageMaxDimension: Int,
    @SerialName("high_res_image_compression_quality")
    val highResImageCompressionQuality: Float,
    @SerialName("high_res_image_crop_padding")
    val highResImageCropPadding: Float,
    @SerialName("training_consent_text")
    val consentText: String
) : Parcelable
