package com.stripe.android.core.model.parsers

import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import org.json.JSONException
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelJsonParserAdapterTest {

    @Test
    fun parse_returnsLegacyParserResult() {
        val actual = ModelJsonParserAdapter(TestStripeFileParser()).parse(VALID_STRIPE_FILE_JSON)

        assertEquals(EXPECTED_FILE, actual)
    }

    @Test
    fun parse_throwsForMalformedJson() {
        assertFailsWith<JSONException> {
            ModelJsonParserAdapter(TestStripeFileParser()).parse("{")
        }
    }

    private companion object {
        private val EXPECTED_FILE = StripeFile(
            id = "file_1FzRQ6CRMbs6FrXfgjerzyUQ",
            created = 1578677834L,
            filename = "upload.png",
            purpose = StripeFilePurpose.BusinessIcon,
            size = 25722,
            title = null,
            type = "png",
            url = "https://files.stripe.com/v1/files/file_1G1H0DBbvEc/contents"
        )

        private class TestStripeFileParser : ModelJsonParser<StripeFile> {
            override fun parse(json: JSONObject): StripeFile {
                return StripeFile(
                    id = json.optString("id"),
                    created = json.optLong("created"),
                    filename = json.optString("filename"),
                    purpose = StripeFilePurpose.fromCode(json.optString("purpose")),
                    size = json.optInt("size"),
                    title = json.optString("title").takeIf { it.isNotEmpty() },
                    type = json.optString("type"),
                    url = json.optString("url")
                )
            }
        }

        private val VALID_STRIPE_FILE_JSON =
            """
            {
                "id": "file_1FzRQ6CRMbs6FrXfgjerzyUQ",
                "object": "file",
                "created": 1578677834,
                "filename": "upload.png",
                "purpose": "business_icon",
                "size": 25722,
                "title": null,
                "type": "png",
                "url": "https://files.stripe.com/v1/files/file_1G1H0DBbvEc/contents"
            }
            """.trimIndent()
    }
}
