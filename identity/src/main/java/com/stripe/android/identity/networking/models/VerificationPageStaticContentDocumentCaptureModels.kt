package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class VerificationPageStaticContentDocumentCaptureModels(
    @SerialName("id_detector_url")
    val idDetectorUrl: String,
    @SerialName("id_detector_min_score")
    val idDetectorMinScore: Float
) : Parcelable
