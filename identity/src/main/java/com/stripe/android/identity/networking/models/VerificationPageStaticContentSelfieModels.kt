package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageStaticContentSelfieModels(
    @SerialName("face_detector_url")
    val faceDetectorUrl: String,
    @SerialName("face_detector_min_score")
    val faceDetectorMinScore: Float,
    @SerialName("face_detector_min_iou")
    val faceDetectorIou: Float
)
