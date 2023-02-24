package com.stripe.android.stripecardscan.framework.api.dto

import android.util.Size
import androidx.test.filters.SmallTest
import com.stripe.android.stripecardscan.framework.util.AcceptedImageConfigs
import com.stripe.android.stripecardscan.framework.util.ImageFormat
import com.stripe.android.core.utils.decodeFromJson
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CardImageVerificationDetailsTest {

    @Test
    @SmallTest
    fun testImageSettingsMethod() {
        val json = """{
              "accepted_image_configs": {
                "default_settings": {
                  "compression_ratio": 0.8,
                  "image_size": [
                    1080,
                    1920
                  ],
                  "image_count": 5,
                  "unknown_field": "test"
                },
                "format_settings": {
                  "heic": {
                    "compression_ratio": 0.5,
                    "image_count": 3
                  },
                  "webp": {
                    "compression_ratio": 0.7
                    "image_size": [
                        2160,
                        1920
                    ]
                  }
                },
                "preferred_formats": [
                  "heic",
                  "webp",
                  "jpeg"
                ]
              },
              "expected_card": {
                "last4": "9012",
                "issuer": "Visa"
              }
            }"""

        val result = decodeFromJson(CardImageVerificationDetailsResult.serializer(), json)
        assertNotNull(result.acceptedImageConfigs)

        val acceptedImageConfigs = AcceptedImageConfigs(result.acceptedImageConfigs)

        val heicSettings = acceptedImageConfigs.getImageSettings(ImageFormat.HEIC)
        assertEquals(heicSettings.compressionRatio, 0.5)
        assertEquals(heicSettings.imageSize, Size(1080, 1920))
        assertEquals(heicSettings.imageCount, 3)

        val jpegSettings = acceptedImageConfigs.getImageSettings(ImageFormat.JPEG)
        assertEquals(jpegSettings.compressionRatio, 0.8)
        assertEquals(jpegSettings.imageSize, Size(1080, 1920))
        assertEquals(jpegSettings.imageCount, 5)

        val webpSettings = acceptedImageConfigs.getImageSettings(ImageFormat.WEBP)
        assertEquals(webpSettings.compressionRatio, 0.7)
        assertEquals(webpSettings.imageSize, Size(2160, 1920))
        assertEquals(webpSettings.imageCount, 5)
    }
}
