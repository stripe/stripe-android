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
    val keepMarginOnTransformedDocumentImage:Boolean,

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

// TODO - remove
val sampleSettings = VerificationPageStaticContentDocumentCaptureMBSettings(
    // for com.stripe.android.identity.example.theme1
    licenseKey = "sRwCACpjb20uc3RyaXBlLmFuZHJvaWQuaWRlbnRpdHkuZXhhbXBsZS50aGVtZTEAbGV5SkRjbVZoZEdWa1QyNGlPakUzTURRNE16SXpORGN6T0RFc0lrTnlaV0YwWldSR2IzSWlPaUppWWpFM056RXdOeTAyWVRKbExUUTFaREF0T1RWbU55MDFZbUkzT1RrMU9UQXhNVEFpZlE9PeyUpZzX6kKM53HngPN/fim44wr8X8zd5ldolQEmUWUsXbdTmtELcsbIkXTN1m4rLwbxosqP3m1mRoiYIz9PDksqN/ytnjnyw1oPfucqxT1vQbozkYkDA1ff72/AM7w=",

    // for com.stripe.android.identity.example.theme2
//    licenseKey = "sRwCACpjb20uc3RyaXBlLmFuZHJvaWQuaWRlbnRpdHkuZXhhbXBsZS50aGVtZTIAbGV5SkRjbVZoZEdWa1QyNGlPakUzTURRNE16SXpPREV6TkRBc0lrTnlaV0YwWldSR2IzSWlPaUppWWpFM056RXdOeTAyWVRKbExUUTFaREF0T1RWbU55MDFZbUkzT1RrMU9UQXhNVEFpZlE9PXkvN1Z4Nlgc0z49nYEClY05lNU6DRtBdQw8g0fpEWcXFKhmRW0AVWRN+rwCjDG1x7L9YKCVa4v/xvk6zxd+BO73Uq478fKEXQvgho5k6R+BgAmqXC2G95WKH9Xjuzw=",
    returnTransformedDocumentImage = true,

    keepMarginOnTransformedDocumentImage = true,

    documentFramingMargin = 0.05f,

    handOcclusionThreshold = 0.05f,

    captureStrategy = VerificationPageStaticContentDocumentCaptureMBSettings.CaptureStrategy.SINGLE_FRAME,

    tooBrightThreshold = 0.9f,

    tooDarkThreshold = 0.95f,

    minimumDocumentDpi = 200,

    adjustMinimumDocumentDpi = true,

    tiltPolicy = VerificationPageStaticContentDocumentCaptureMBSettings.TiltPolicy.RELAXED,

    blurPolicy = VerificationPageStaticContentDocumentCaptureMBSettings.BlurPolicy.STRICT,

    glarePolicy = VerificationPageStaticContentDocumentCaptureMBSettings.GlarePolicy.STRICT
)