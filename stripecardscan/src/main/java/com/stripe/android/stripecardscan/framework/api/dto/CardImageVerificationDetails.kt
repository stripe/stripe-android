package com.stripe.android.stripecardscan.framework.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CardImageVerificationDetailsRequest(
    @SerialName("client_secret") val cardImageVerificationSecret: String
)

@Serializable
internal data class CardImageVerificationDetailsResult(
    @SerialName("expected_card") val expectedCard: CardImageVerificationDetailsExpectedCard?,
    @SerialName("accepted_image_configs") val acceptedImageConfigs: CardImageVerificationDetailsAcceptedImageConfigs? = null,
)

@Serializable
internal data class CardImageVerificationDetailsExpectedCard(
    @SerialName("issuer") val issuer: String?,
    @SerialName("last4") val lastFour: String?,
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
    @SerialName("compression_ratio") var compressionRatio: Double? = null,
    @SerialName("image_size") var imageSize: DoubleArray? = null,
) {
    companion object {
        // These default values are what Android was using before the addition of a server config.
        val DEFAULT = CardImageVerificationDetailsImageSettings(0.92, doubleArrayOf(1080.0, 1920.0))
    }
}

@Serializable
internal data class CardImageVerificationDetailsAcceptedImageConfigs(
    @SerialName("default_settings") private val defaultSettings: CardImageVerificationDetailsImageSettings? = CardImageVerificationDetailsImageSettings.DEFAULT,
    @SerialName("format_settings") private val formatSettings: HashMap<CardImageVerificationDetailsFormat, CardImageVerificationDetailsImageSettings?>? = null,
    @SerialName("preferred_formats") val preferredFormats: Array<CardImageVerificationDetailsFormat>? = Array<CardImageVerificationDetailsFormat>(1) {
        CardImageVerificationDetailsFormat.JPEG
    }
) {
    fun imageSettings(format: CardImageVerificationDetailsFormat): CardImageVerificationDetailsImageSettings {
        // Default to client default settings
        var result = CardImageVerificationDetailsImageSettings.DEFAULT

        // Override with server default settings
        defaultSettings?.let {
            result.compressionRatio = it.compressionRatio ?: result.compressionRatio
            result.imageSize = it.imageSize ?: result.imageSize
        }

        // Take format specific settings
        formatSettings?.get(format)?.let {
            result.compressionRatio = it.compressionRatio ?: result.compressionRatio
            result.imageSize = it.imageSize ?: result.imageSize
        }

        return result
    }
}
