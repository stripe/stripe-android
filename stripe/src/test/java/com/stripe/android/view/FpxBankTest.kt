package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
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

    @Test
    fun testOrder() {
        val expected = listOf(
            "Maybank2U",
            "CIMB Clicks",
            "Public Bank",
            "RHB Bank",
            "Hong Leong Bank",
            "AmBank",
            "Affin Bank",
            "Alliance Bank",
            "Bank Islam",
            "Bank Muamalat",
            "Bank Rakyat",
            "BSN",
            "HSBC Bank",
            "KFH",
            "Maybank2E",
            "OCBC Bank",
            "Standard Chartered",
            "UOB"
        )
        val actual = FpxBank.values().map { it.displayName }
        assertThat(expected)
            .isEqualTo(actual)
    }
}
