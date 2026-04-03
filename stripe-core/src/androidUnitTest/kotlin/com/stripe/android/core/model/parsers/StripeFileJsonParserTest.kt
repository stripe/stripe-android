package com.stripe.android.core.model.parsers

import com.stripe.android.core.model.StripeFile
import com.stripe.android.core.model.StripeFilePurpose
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals

class StripeFileJsonParserTest {

    @Test
    fun testParse() {
        assertEquals(FILE, StripeFileJsonParser().parse(DEFAULT))
    }

    private companion object {
        private val FILE = StripeFile(
            id = "file_1FzRQ6CRMbs6FrXfgjerzyUQ",
            created = 1578677834L,
            filename = "upload.png",
            purpose = StripeFilePurpose.BusinessIcon,
            size = 25722,
            title = null,
            type = "png",
            url = "https://files.stripe.com/v1/files/file_1G1H0DBbvEc/contents"
        )

        val DEFAULT = JSONObject(
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
        )
    }
}
