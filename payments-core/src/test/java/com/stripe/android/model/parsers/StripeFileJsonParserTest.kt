package com.stripe.android.model.parsers

import com.stripe.android.model.StripeFile
import com.stripe.android.model.StripeFileFixtures
import com.stripe.android.model.StripeFilePurpose
import kotlin.test.Test
import kotlin.test.assertEquals

class StripeFileJsonParserTest {

    @Test
    fun testParse() {
        assertEquals(FILE, StripeFileJsonParser().parse(StripeFileFixtures.DEFAULT))
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
    }
}
