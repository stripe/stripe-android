package com.stripe.android.stripecardscan.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CardImageVerificationDetailsRequest(
    @SerialName("client_secret") val cardImageVerificationSecret: String
)

@Serializable
internal data class CardImageVerificationDetailsResult(
    @SerialName("expected_card")
    val expectedCard: CardImageVerificationDetailsExpectedCard?,

    @SerialName("accepted_image_configs")
    val acceptedImageConfigs: CardImageVerificationDetailsAcceptedImageConfigs? = null
)

@Serializable
internal data class CardImageVerificationDetailsExpectedCard(
    @SerialName("issuer") val issuer: String?,
    @SerialName("last4") val lastFour: String?
)

@Serializable
internal enum class CardImageVerificationDetailsFormat {
    @SerialName("heic")
    HEIC,

    @SerialName("jpeg")
    JPEG,

    @SerialName("webp")
    WEBP;
}

@Serializable
internal data class CardImageVerificationDetailsImageSettings(
    @SerialName("compression_ratio") val compressionRatio: Double? = null,
    @SerialName("image_size") val imageSize: List<Double>? = null,
    @SerialName("image_count") val imageCount: Int? = null
)

@Serializable
internal data class CardImageVerificationDetailsAcceptedImageConfigs(
    @SerialName("default_settings")
    val defaultSettings: CardImageVerificationDetailsImageSettings? = null,

    @SerialName("format_settings")
    val formatSettings:
        HashMap<CardImageVerificationDetailsFormat,
            CardImageVerificationDetailsImageSettings?>? = null,

    @SerialName("preferred_formats")
    val preferredFormats: List<CardImageVerificationDetailsFormat>? = null
)
