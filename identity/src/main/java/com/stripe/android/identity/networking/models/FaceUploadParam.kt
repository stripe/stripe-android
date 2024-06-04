package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
internal data class FaceUploadParam(
    @SerialName("best_high_res_image")
    val bestHighResImage: String,
    @SerialName("best_low_res_image")
    val bestLowResImage: String,
    @SerialName("first_high_res_image")
    val firstHighResImage: String,
    @SerialName("first_low_res_image")
    val firstLowResImage: String,
    @SerialName("last_high_res_image")
    val lastHighResImage: String,
    @SerialName("last_low_res_image")
    val lastLowResImage: String,
    @SerialName("best_face_score")
    val bestFaceScore: Float,
    @SerialName("face_score_variance")
    val faceScoreVariance: Float,
    @SerialName("num_frames")
    val numFrames: Int,
    @SerialName("best_exposure_duration")
    val bestExposureDuration: Int? = null,
    @SerialName("best_brightness_value")
    val bestBrightnessValue: Float? = null,
    @SerialName("best_camera_lens_model")
    val bestCameraLensModel: String? = null,
    @SerialName("best_focal_length")
    val bestFocalLength: Float? = null,
    @SerialName("best_is_virtual_camera")
    val bestIsVirtualCamera: Boolean? = null,
    @SerialName("best_exposure_iso")
    val bestExposureIso: Float? = null,
    @SerialName("training_consent")
    val trainingConsent: Boolean? = null
) : Parcelable
