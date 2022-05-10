package com.stripe.android.stripecardscan.framework.api.dto

import androidx.test.filters.SmallTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
                  ]
                },
                "format_settings": {
                  "heic": {
                    "compression_ratio": 0.5
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

        val result = Json.decodeFromString<CardImageVerificationDetailsResult>(json)
        assertNotNull(result.acceptedImageConfigs)

        result.acceptedImageConfigs?.let {
            val heicSettings = it.imageSettings(CardImageVerificationDetailsFormat.HEIC)
            assertNotNull(heicSettings)

            heicSettings?.let {
                assertEquals(it.compressionRatio, 0.5)
                assertEquals(it.imageSize?.first(), 1080.0)
                assertEquals(it.imageSize?.last(), 1920.0)
            }

            val jpegSettings = it.imageSettings(CardImageVerificationDetailsFormat.JPEG)
            assertNotNull(jpegSettings)

            jpegSettings?.let {
                assertEquals(it.compressionRatio, 0.8)
                assertEquals(it.imageSize?.first(), 1080.0)
                assertEquals(it.imageSize?.last(), 1920.0)
            }

            val webpSettings = it.imageSettings(CardImageVerificationDetailsFormat.WEBP)
            assertNotNull(webpSettings)

            webpSettings?.let {
                assertEquals(it.compressionRatio, 0.7)
                assertEquals(it.imageSize?.first(), 2160.0)
                assertEquals(it.imageSize?.last(), 1920.0)
            }
        }
    }
}
