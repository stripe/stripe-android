package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class FpxBankTest {
    @Test
    fun testGet_withValidBank() {
        assertThat(FpxBank.Hsbc).isEqualTo(FpxBank.get("hsbc"))
    }

    @Test
    fun testGet_withInvalidBank() {
        assertThat(FpxBank.get("not_a_bank")).isNull()
    }

    @Test
    fun testOrder() {
        val expected = listOf(
            "Affin Bank",
            "Agrobank",
            "Alliance Bank",
            "AmBank",
            "Bank Islam",
            "Bank Muamalat",
            "Bank of China",
            "Bank Rakyat",
            "BSN",
            "CIMB Clicks",
            "Hong Leong Bank",
            "HSBC Bank",
            "KFH",
            "Maybank2E",
            "Maybank2U",
            "MBSB Bank",
            "OCBC Bank",
            "Public Bank",
            "RHB Bank",
            "Standard Chartered",
            "UOB Bank"
        )
        val actual = FpxBank.entries.map { it.displayName }
        assertThat(expected)
            .isEqualTo(actual)
    }
}
