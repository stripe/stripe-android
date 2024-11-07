package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class IntAsRgbHexStringSerializerTest {
    @Test
    fun `encodes plain ints as ints`() {
        assertEquals(
            "1",
            Json.encodeToString(1)
        )
    }

    @Test
    fun `encodes IntAsRgbHexString as correctly formatted string`() {
        assertEquals(
            "\"#FF0000\"",
            Json.encodeToString<IntAsRgbHexString>(0xff0000)
        )
    }
}
