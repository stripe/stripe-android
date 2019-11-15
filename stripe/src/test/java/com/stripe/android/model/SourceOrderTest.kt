package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceOrderTest {

    @Test
    fun testParse() {
        assertEquals(
            SourceOrderFixtures.SOURCE_ORDER,
            SourceOrder.fromJson(SourceOrderFixtures.SOURCE_ORDER_JSON)
        )
    }
}
