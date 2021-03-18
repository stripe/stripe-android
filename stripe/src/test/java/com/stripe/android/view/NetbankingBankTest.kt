package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetbankingBankText {
    @Test
    fun testGet_withValidBank() {
        val expectedBank = NetbankingBank.values().first()
        val actualBank = NetbankingBank.get("kotak")
        assertEquals(expectedBank.code, actualBank?.code)
        assertEquals(expectedBank.displayName, actualBank?.displayName)
    }

    @Test
    fun testGet_withInvalidBank() {
        assertNull(FpxBank.get("not_a_bank"))
    }

}