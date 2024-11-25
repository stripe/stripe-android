package com.stripe.android.connect.webview.serialization

import kotlinx.serialization.Serializable
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
            """{"bar":"#FF0000","baz":42}""",
            Json.encodeToString(Foo(bar = 0xff0000, baz = 42))
        )
    }

    @Serializable
    private data class Foo(
        val bar: IntAsRgbHexString,
        val baz: Int,
    )
}
