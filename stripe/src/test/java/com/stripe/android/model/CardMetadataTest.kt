package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CardMetadataTest {
    @Test
    fun `AccountRange should match expected ranges`() {
        val testRange = CardMetadata.AccountRange(
            accountRangeLow = "134",
            accountRangeHigh = "167",
            panLength = 16,
            country = "CA",
            brand = "visa"
        )

        assertThat(testRange.matches("0")).isFalse()
        assertThat(testRange.matches("1")).isTrue()
        assertThat(testRange.matches("2")).isFalse()

        assertThat(testRange.matches("00")).isFalse()
        assertThat(testRange.matches("13")).isTrue()
        assertThat(testRange.matches("14")).isTrue()
        assertThat(testRange.matches("16")).isTrue()
        assertThat(testRange.matches("20")).isFalse()

        assertThat(testRange.matches("133")).isFalse()
        assertThat(testRange.matches("134")).isTrue()
        assertThat(testRange.matches("135")).isTrue()
        assertThat(testRange.matches("167")).isTrue()
        assertThat(testRange.matches("168")).isFalse()

        assertThat(testRange.matches("1244")).isFalse()
        assertThat(testRange.matches("1340")).isTrue()
        assertThat(testRange.matches("1344")).isTrue()
        assertThat(testRange.matches("1444")).isTrue()
        assertThat(testRange.matches("1670")).isTrue()
        assertThat(testRange.matches("1679")).isTrue()
        assertThat(testRange.matches("1680")).isFalse()
    }

    @Test
    fun `AccountRange should handle leading zeroes`() {
        val testRange = CardMetadata.AccountRange(
            accountRangeLow = "004",
            accountRangeHigh = "017",
            panLength = 16,
            country = "JP",
            brand = "mastercard"
        )

        assertThat(testRange.matches("0")).isTrue()
        assertThat(testRange.matches("1")).isFalse()

        assertThat(testRange.matches("00")).isTrue()
        assertThat(testRange.matches("01")).isTrue()
        assertThat(testRange.matches("10")).isFalse()
        assertThat(testRange.matches("20")).isFalse()

        assertThat(testRange.matches("000")).isFalse()
        assertThat(testRange.matches("002")).isFalse()
        assertThat(testRange.matches("004")).isTrue()
        assertThat(testRange.matches("009")).isTrue()
        assertThat(testRange.matches("014")).isTrue()
        assertThat(testRange.matches("017")).isTrue()
        assertThat(testRange.matches("019")).isFalse()
        assertThat(testRange.matches("020")).isFalse()
        assertThat(testRange.matches("100")).isFalse()

        assertThat(testRange.matches("0000")).isFalse()
        assertThat(testRange.matches("0021")).isFalse()
        assertThat(testRange.matches("0044")).isTrue()
        assertThat(testRange.matches("0098")).isTrue()
        assertThat(testRange.matches("0143")).isTrue()
        assertThat(testRange.matches("0173")).isTrue()
        assertThat(testRange.matches("0195")).isFalse()
        assertThat(testRange.matches("0202")).isFalse()
        assertThat(testRange.matches("1004")).isFalse()
    }
}
