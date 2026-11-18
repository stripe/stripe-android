package com.stripe.android.identity.networking.models

import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.json
import com.stripe.android.identity.networking.VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE_JSON_STRING
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Test

internal class VerificationPageStaticContentSelfieCapturePageTest {
    @Test
    fun `missing MediaPipe crop padding is optional for existing responses`() {
        val page = decodeSelfiePage()

        assertThat(page.mediaPipeCropSidePadding).isNull()
        assertThat(page.mediaPipeCropTopPadding).isNull()
        assertThat(page.mediaPipeCropBottomPadding).isNull()
    }

    @Test
    fun `MediaPipe crop padding decodes server overrides`() {
        val page = decodeSelfiePage(
            mapOf(
                "media_pipe_crop_side_padding" to JsonPrimitive(0.25f),
                "media_pipe_crop_top_padding" to JsonPrimitive(0.5f),
                "media_pipe_crop_bottom_padding" to JsonPrimitive(0.1f)
            )
        )

        assertThat(page.mediaPipeCropSidePadding).isEqualTo(0.25f)
        assertThat(page.mediaPipeCropTopPadding).isEqualTo(0.5f)
        assertThat(page.mediaPipeCropBottomPadding).isEqualTo(0.1f)
    }

    @Test
    fun `MediaPipe crop padding preserves explicit zero and null values`() {
        val page = decodeSelfiePage(
            mapOf(
                "media_pipe_crop_side_padding" to JsonPrimitive(0f),
                "media_pipe_crop_top_padding" to JsonNull,
                "media_pipe_crop_bottom_padding" to JsonPrimitive(0f)
            )
        )

        assertThat(page.mediaPipeCropSidePadding).isEqualTo(0f)
        assertThat(page.mediaPipeCropTopPadding).isNull()
        assertThat(page.mediaPipeCropBottomPadding).isEqualTo(0f)
    }

    private fun decodeSelfiePage(
        padding: Map<String, JsonPrimitive> = emptyMap()
    ): VerificationPageStaticContentSelfieCapturePage {
        val page = json.parseToJsonElement(VERIFICATION_PAGE_REQUIRE_SELFIE_LIVE_CAPTURE_JSON_STRING).jsonObject
        val selfie = page.getValue("selfie").jsonObject
        return json.decodeFromJsonElement(
            VerificationPageStaticContentSelfieCapturePage.serializer(),
            JsonObject(selfie + padding)
        )
    }
}
