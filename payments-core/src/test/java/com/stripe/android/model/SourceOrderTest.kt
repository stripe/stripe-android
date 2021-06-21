package com.stripe.android.model

import com.stripe.android.model.parsers.SourceOrderJsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceOrderTest {

    @Test
    fun testParse() {
        assertEquals(
            SourceOrderFixtures.SOURCE_ORDER,
            SourceOrderJsonParser().parse(SourceOrderFixtures.SOURCE_ORDER_JSON)
        )
    }
}
