package com.stripe.android.view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FpxBankTest {
    @Test
    fun testGet_withValidBank() {
        assertEquals(FpxBank.Hsbc, FpxBank.get("hsbc"))
    }

    @Test
    fun testGet_withInvalidBank() {
        assertNull(FpxBank.get("not_a_bank"))
    }
}
