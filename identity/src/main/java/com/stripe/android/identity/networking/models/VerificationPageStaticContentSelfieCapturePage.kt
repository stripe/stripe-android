package com.stripe.android.identity.networking.models

// TODO(ccen) Generate from schema and populate from network response
internal data class VerificationPageStaticContentSelfieCapturePage(
    val autoCaptureTimeout: Int,
    val filePurpose: String,
    val numSamples: Int,
    val sampleInterval: Int,
    val faceDetectorThreshold: Float,
    val maxCenteredThresholdX: Float,
    val maxCenteredThresholdY: Float,
    val minEdgeThreshold: Float,
    val minCoverageThreshold: Float,
    val maxCoverageThreshold: Float,
    val lowResImageMaxDimension: Int,
    val lowResImageCompressionQuality: Float,
    val highResImageMaxDimension: Int,
    val highResImageCompressionQuality: Float,
    val highResImageCropPadding: Float,
    val consentText: String
)
