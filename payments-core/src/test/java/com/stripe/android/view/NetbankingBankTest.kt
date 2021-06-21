package com.stripe.android.view

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class NetbankingBankTest {
    @Test
    fun testGet_withValidBank() {
        assertThat(NetbankingBank.AirtelBank).isEqualTo(NetbankingBank.get("airtel"))
    }

    @Test
    fun testGet_withInvalidBank() {
        assertThat(FpxBank.get("not_a_bank")).isNull()
    }
}
