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
        val json = "{\n" +
            "  \"accepted_image_configs\": {\n" +
            "    \"default_settings\": {\n" +
            "      \"compression_ratio\": 0.8,\n" +
            "      \"image_size\": [\n" +
            "        1080,\n" +
            "        1920\n" +
            "      ]\n" +
            "    },\n" +
            "    \"format_settings\": {\n" +
            "      \"heic\": {\n" +
            "        \"compression_ratio\": 0.5\n" +
            "      },\n" +
            "      \"webp\": {\n" +
            "        \"compression_ratio\": 0.7\n" +
            "        \"image_size\": [\n" +
            "            2160,\n" +
            "            1920\n" +
            "      ]\n" +
            "      }\n" +
            "    },\n" +
            "    \"preferred_formats\": [\n" +
            "      \"heic\",\n" +
            "      \"webp\",\n" +
            "      \"jpeg\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"expected_card\": {\n" +
            "    \"last4\": \"9012\",\n" +
            "    \"issuer\": \"Visa\"\n" +
            "  }\n" +
            "}"

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
