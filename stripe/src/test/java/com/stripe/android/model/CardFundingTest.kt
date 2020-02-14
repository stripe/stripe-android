package com.stripe.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class CardFundingTest {
    @Test
    fun fromCode_shouldReturnExpectedValue() {
        CardFunding.values().forEach {
            assertEquals(it, CardFunding.fromCode(it.code))
        }
    }
}
