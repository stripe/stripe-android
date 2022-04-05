package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VerificationPageStaticContentDocumentCaptureModels(
    @SerialName("id_detector_url")
    val idDetectorUrl: String,
    @SerialName("id_detector_min_score")
    val idDetectorMinScore: Float,
)
