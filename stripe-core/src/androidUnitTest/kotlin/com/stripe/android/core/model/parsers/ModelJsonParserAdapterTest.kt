package com.stripe.android.core.model.parsers

import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import org.json.JSONException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ModelJsonParserAdapterTest {

    @Test
    fun parse_returnsLegacyParserResult() {
        val actual = ModelJsonParserAdapter(StripeFileJsonParser()).parse(VALID_STRIPE_FILE_JSON)

        assertEquals(EXPECTED_FILE, actual)
    }

    @Test
    fun parse_throwsForMalformedJson() {
        assertFailsWith<JSONException> {
            ModelJsonParserAdapter(StripeFileJsonParser()).parse("{")
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
