package com.stripe.android.stripecardscan.framework.util

import android.util.Size
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsAcceptedImageConfigs
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsFormat
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsImageSettings

internal enum class ImageFormat(val string: String) {
    HEIC("heic"),
    JPEG("jpeg"),
    WEBP("webp");

    companion object {
        fun fromValue(value: CardImageVerificationDetailsFormat): ImageFormat =
            when (value) {
                CardImageVerificationDetailsFormat.HEIC -> HEIC
                CardImageVerificationDetailsFormat.JPEG -> JPEG
                CardImageVerificationDetailsFormat.WEBP -> WEBP
            }
    }
}

internal data class OptionalImageSettings(
    val compressionRatio: Double?,
    val imageSize: Size?,
    val imageCount: Int?
) {
    constructor(settings: CardImageVerificationDetailsImageSettings?) : this(
        compressionRatio = settings?.compressionRatio,
        imageSize = settings?.imageSize?.takeIf { it.size > 1 }?.let {
            Size(it.first().toInt(), it.last().toInt())
        },
        imageCount = settings?.imageCount
    )
}

internal data class ImageSettings(
    val compressionRatio: Double,
    val imageSize: Size,
    val imageCount: Int
) {
    companion object {
        // These default values are what Android was using before the addition of a server config.
        val DEFAULT = ImageSettings(0.92, Size(1080, 1920), 3)
    }
}

internal data class AcceptedImageConfigs(
    private val defaultSettings: OptionalImageSettings?,
    private val formatSettings: Map<ImageFormat, OptionalImageSettings?>? = null,
    val preferredFormats: List<ImageFormat>? = listOf(ImageFormat.JPEG)
) {
    companion object {
        internal fun isFormatSupported(format: ImageFormat) =
            format == ImageFormat.JPEG || format == ImageFormat.WEBP
    }

    constructor(configs: CardImageVerificationDetailsAcceptedImageConfigs? = null) : this(
        defaultSettings = configs?.defaultSettings?.let { OptionalImageSettings(it) },
        formatSettings = configs?.formatSettings?.takeIf { it.isNotEmpty() }?.map { entry ->
            ImageFormat.fromValue(entry.key) to OptionalImageSettings(entry.value)
        }
            ?.toMap(),
        preferredFormats = configs?.preferredFormats?.map { ImageFormat.fromValue(it) }
            ?.filter { isFormatSupported(it) }
            ?.takeIf { it.isNotEmpty() }
    )

    fun getImageSettings(format: ImageFormat): ImageSettings = ImageSettings(
        compressionRatio = formatSettings?.get(format)?.compressionRatio
            ?: defaultSettings?.compressionRatio ?: ImageSettings.DEFAULT.compressionRatio,
        imageSize = formatSettings?.get(format)?.imageSize ?: defaultSettings?.imageSize
            ?: ImageSettings.DEFAULT.imageSize,
        imageCount = formatSettings?.get(format)?.imageCount ?: defaultSettings?.imageCount
            ?: ImageSettings.DEFAULT.imageCount
    )

    fun getImageSettings(): Pair<ImageFormat, ImageSettings> {
        val format = preferredFormats?.first() ?: ImageFormat.JPEG
        return format to getImageSettings(format)
    }
}
