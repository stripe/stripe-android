package com.stripe.android.stripecardscan.framework.api.dto

import android.util.Size
import androidx.test.filters.SmallTest
import com.stripe.android.stripecardscan.framework.util.AcceptedImageConfigs
import com.stripe.android.stripecardscan.framework.util.ImageFormat
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
            val acceptedImageConfigs = AcceptedImageConfigs(it)
            val heicSettings = acceptedImageConfigs.imageSettings(ImageFormat.HEIC)
            assertNotNull(heicSettings)

            heicSettings?.let {
                assertEquals(it.first, 0.5)
                assertEquals(it.second, Size(1080, 1920))
            }

            val jpegSettings = acceptedImageConfigs.imageSettings(ImageFormat.JPEG)
            assertNotNull(jpegSettings)

            jpegSettings?.let {
                assertEquals(it.first, 0.8)
                assertEquals(it.second, Size(1080, 1920))
            }

            val webpSettings = acceptedImageConfigs.imageSettings(ImageFormat.WEBP)
            assertNotNull(webpSettings)

            webpSettings?.let {
                assertEquals(it.first, 0.7)
                assertEquals(it.second, Size(2160, 1920))
            }
        }
    }
}
