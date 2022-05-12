package com.stripe.android.stripecardscan.framework.util

import android.util.Size
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsAcceptedImageConfigs
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsFormat
import com.stripe.android.stripecardscan.framework.api.dto.CardImageVerificationDetailsImageSettings

internal enum class ImageFormat {
    HEIC,
    JPEG,
    WEBP;

    companion object {
        fun fromValue(value: CardImageVerificationDetailsFormat): ImageFormat =
            when (value) {
                CardImageVerificationDetailsFormat.HEIC -> ImageFormat.HEIC
                CardImageVerificationDetailsFormat.JPEG -> ImageFormat.JPEG
                CardImageVerificationDetailsFormat.WEBP -> ImageFormat.WEBP
            }
    }
}

internal data class ImageSettings(
    var compressionRatio: Double? = null,
    var imageSize: Size? = null
) {
    companion object {
        // These default values are what Android was using before the addition of a server config.
        val DEFAULT = ImageSettings(0.92, Size(1080, 1920))
    }

    constructor(settings: CardImageVerificationDetailsImageSettings?) : this() {
        settings?.let {
            compressionRatio = it.compressionRatio ?: compressionRatio

            it.imageSize?.takeIf { it.size > 1 }?.let {
                var pendingSize = imageSize ?: ImageSettings.DEFAULT.imageSize!!
                val width = it.first().toInt() ?: pendingSize.width
                val height = it.last().toInt() ?: pendingSize.height
                imageSize = Size(width, height)
            }
        }
    }
}

internal data class AcceptedImageConfigs(
    private var defaultSettings: ImageSettings? = ImageSettings.DEFAULT,
    private var formatSettings: HashMap<ImageFormat, ImageSettings?>? = null,
    var preferredFormats: Array<ImageFormat>? = Array<ImageFormat>(1) { ImageFormat.JPEG }
) {
    constructor(configs: CardImageVerificationDetailsAcceptedImageConfigs?) : this() {
        configs?.let {
            it.formatSettings?.takeIf { it.size > 0 }.also { formatSettings = HashMap() }?.forEach {
                val value = ImageSettings(it.value)
                val key = ImageFormat.fromValue(it.key)
                formatSettings?.put(key, value)
            }

            val mappedFormats = it.preferredFormats?.map { ImageFormat.fromValue(it) }
                ?.filter { isformatSupport(it) }
                ?.takeIf { it.count() > 0 }
                ?.let { preferredFormats = it.toTypedArray() }

            it.defaultSettings?.let { defaultSettings = ImageSettings(it) }
        }
    }

    internal fun isformatSupport(format: ImageFormat) =
        format == ImageFormat.JPEG || format == ImageFormat.WEBP

    fun imageSettings(format: ImageFormat): Pair<Double, Size> {
        // Default to client default settings
        var result = ImageSettings.DEFAULT

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

        return Pair(result.compressionRatio!!, result.imageSize!!)
    }
}
