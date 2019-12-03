package com.stripe.android.model.parsers

import com.stripe.android.model.SourceFixtures
import kotlin.test.Test
import kotlin.test.assertNotNull

class SourceOwnerJsonParserTest {
    @Test
    fun parse_withoutNulls_isNotNull() {
        assertNotNull(SourceOwnerJsonParser().parse(SourceFixtures.SOURCE_OWNER_WITHOUT_NULLS))
    }

    @Test
    fun parse_withNulls_IsNotNull() {
        assertNotNull(SourceOwnerJsonParser().parse(SourceFixtures.SOURCE_OWNER_WITH_NULLS))
    }
}
