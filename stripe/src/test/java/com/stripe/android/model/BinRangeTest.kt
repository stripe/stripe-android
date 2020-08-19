package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class BinRangeTest {
    @Test
    fun `BinRange should match expected ranges`() {
        val binRange = BinRange(low = "134", high = "167")

        assertThat(binRange.matches("0")).isFalse()
        assertThat(binRange.matches("1")).isTrue()
        assertThat(binRange.matches("2")).isFalse()

        assertThat(binRange.matches("00")).isFalse()
        assertThat(binRange.matches("13")).isTrue()
        assertThat(binRange.matches("14")).isTrue()
        assertThat(binRange.matches("16")).isTrue()
        assertThat(binRange.matches("20")).isFalse()

        assertThat(binRange.matches("133")).isFalse()
        assertThat(binRange.matches("134")).isTrue()
        assertThat(binRange.matches("135")).isTrue()
        assertThat(binRange.matches("167")).isTrue()
        assertThat(binRange.matches("168")).isFalse()

        assertThat(binRange.matches("1244")).isFalse()
        assertThat(binRange.matches("1340")).isTrue()
        assertThat(binRange.matches("1344")).isTrue()
        assertThat(binRange.matches("1444")).isTrue()
        assertThat(binRange.matches("1670")).isTrue()
        assertThat(binRange.matches("1679")).isTrue()
        assertThat(binRange.matches("1680")).isFalse()
    }

    @Test
    fun `BinRange should handle leading zeroes`() {
        val binRange = BinRange(low = "004", high = "017")

        assertThat(binRange.matches("0")).isTrue()
        assertThat(binRange.matches("1")).isFalse()

        assertThat(binRange.matches("00")).isTrue()
        assertThat(binRange.matches("01")).isTrue()
        assertThat(binRange.matches("10")).isFalse()
        assertThat(binRange.matches("20")).isFalse()

        assertThat(binRange.matches("000")).isFalse()
        assertThat(binRange.matches("002")).isFalse()
        assertThat(binRange.matches("004")).isTrue()
        assertThat(binRange.matches("009")).isTrue()
        assertThat(binRange.matches("014")).isTrue()
        assertThat(binRange.matches("017")).isTrue()
        assertThat(binRange.matches("019")).isFalse()
        assertThat(binRange.matches("020")).isFalse()
        assertThat(binRange.matches("100")).isFalse()

        assertThat(binRange.matches("0000")).isFalse()
        assertThat(binRange.matches("0021")).isFalse()
        assertThat(binRange.matches("0044")).isTrue()
        assertThat(binRange.matches("0098")).isTrue()
        assertThat(binRange.matches("0143")).isTrue()
        assertThat(binRange.matches("0173")).isTrue()
        assertThat(binRange.matches("0195")).isFalse()
        assertThat(binRange.matches("0202")).isFalse()
        assertThat(binRange.matches("1004")).isFalse()
    }
}
