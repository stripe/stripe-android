package com.stripe.android.model.parsers

import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFilePurpose
import kotlin.test.Test
import kotlin.test.assertEquals
import org.json.JSONObject

class StripeFileJsonParserTest {

    @Test
    fun testParse() {
        assertEquals(FILE, StripeFileJsonParser().parse(FILE_JSON))
    }

    private companion object {
        private val FILE = StripeFile(
            id = "file_1FzRQ6CRMbs6FrXfgjerzyUQ",
            created = 1578677834L,
            filename = "upload.png",
            purpose = StripeFilePurpose.IdentityDocument,
            size = 25722,
            title = null,
            type = "png"
        )

        private val FILE_JSON = JSONObject("""
            {
                "id": "file_1FzRQ6CRMbs6FrXfgjerzyUQ",
                "object": "file",
                "created": 1578677834,
                "filename": "upload.png",
                "purpose": "identity_document",
                "size": 25722,
                "title": null,
                "type": "png"
            }
        """.trimIndent())
    }
}
