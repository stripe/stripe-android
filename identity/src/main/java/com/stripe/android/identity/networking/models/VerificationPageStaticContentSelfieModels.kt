package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO(IDPROD-3944) - verify with server change
@Serializable
internal data class VerificationPageStaticContentSelfieModels(
    @SerialName("face_detector_url")
    val faceDetectorUrl: String,
    @SerialName("face_detector_min_score")
    val faceDetectorMinScore: Float,
    @SerialName("face_detector_iou")
    val faceDetectorIou: Float
)
