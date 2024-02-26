package com.stripe.android.identity.networking.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class VerificationPageStaticContentDocumentCaptureMBSettings(
    @SerialName("license_key")
    val licenseKey: String,

    @SerialName("return_transformed_document_image")
    val returnTransformedDocumentImage: Boolean,

    @SerialName("keep_margin_on_transformed_document_image")
    val keepMarginOnTransformedDocumentImage: Boolean,

    @SerialName("document_framing_margin")
    val documentFramingMargin: Float,

    @SerialName("hand_occlusion_threshold")
    val handOcclusionThreshold: Float,

    @SerialName("capture_strategy")
    val captureStrategy: CaptureStrategy,

    @SerialName("too_bright_threshold")
    val tooBrightThreshold: Float,

    @SerialName("too_dark_threshold")
    val tooDarkThreshold: Float,

    @SerialName("minimum_document_dpi")
    val minimumDocumentDpi: Int,

    @SerialName("adjust_minimum_document_dpi")
    val adjustMinimumDocumentDpi: Boolean,

    @SerialName("tilt_policy")
    val tiltPolicy: TiltPolicy,

    @SerialName("blur_policy")
    val blurPolicy: BlurPolicy,

    @SerialName("glare_policy")
    val glarePolicy: GlarePolicy
) : Parcelable {
    @Serializable
    enum class CaptureStrategy {
        @SerialName("single_frame")
        SINGLE_FRAME,

        @SerialName("optimize_for_quality")
        OPTIMIZE_FOR_QUALITY,

        @SerialName("optimize_for_speed")
        OPTIMIZE_FOR_SPEED,

        @SerialName("default")
        DEFAULT
    }

    @Serializable
    enum class TiltPolicy {
        @SerialName("disabled")
        DISABLED,

        @SerialName("normal")
        NORMAL,

        @SerialName("relaxed")
        RELAXED,

        @SerialName("strict")
        STRICT
    }

    @Serializable
    enum class BlurPolicy {
        @SerialName("disabled")
        DISABLED,

        @SerialName("normal")
        NORMAL,

        @SerialName("relaxed")
        RELAXED,

        @SerialName("strict")
        STRICT
    }

    @Serializable
    enum class GlarePolicy {
        @SerialName("disabled")
        DISABLED,

        @SerialName("normal")
        NORMAL,

        @SerialName("relaxed")
        RELAXED,

        @SerialName("strict")
        STRICT
    }
}
