package com.stripe.android.identity.networking.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TODO(IDPROD-3944) - verify with server change
@Serializable
internal data class FaceUploadParam(
    @SerialName("best_high_res_image")
    val bestHighResImage: String? = null,
    @SerialName("best_low_res_image")
    val bestLowResImage: String? = null,
    @SerialName("first_high_res_image")
    val firstHighResImage: String? = null,
    @SerialName("first_low_res_image")
    val firstLowResImage: String? = null,
    @SerialName("last_high_res_image")
    val lastHighResImage: String? = null,
    @SerialName("last_low_res_image")
    val lastLowResImage: String? = null,
    @SerialName("face_score_variance")
    val faceScoreVariance: Float? = null,
    @SerialName("num_frames")
    val numFrames: Int? = null,
)
