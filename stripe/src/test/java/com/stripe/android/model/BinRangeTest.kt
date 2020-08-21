package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.CardNumber
import kotlin.test.Test

class BinRangeTest {
    @Test
    fun `BinRange should match expected ranges`() {
        val binRange = BinRange(low = "134", high = "167")

        assertThat(binRange.matches(CardNumber.Unvalidated("")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("0")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("1")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("2")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("00")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("13")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("14")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("16")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("20")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("133")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("134")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("135")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("167")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("168")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("1244")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("1340")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("1344")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("1444")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("1670")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("1679")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("1680")))
            .isFalse()
    }

    @Test
    fun `BinRange should handle leading zeroes`() {
        val binRange = BinRange(low = "004", high = "017")

        assertThat(binRange.matches(CardNumber.Unvalidated("")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("0")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("1")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("00")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("01")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("10")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("20")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("000")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("002")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("004")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("009")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("014")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("017")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("019")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("020")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("100")))
            .isFalse()

        assertThat(binRange.matches(CardNumber.Unvalidated("0000")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("0021")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("0044")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("0098")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("0143")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("0173")))
            .isTrue()
        assertThat(binRange.matches(CardNumber.Unvalidated("0195")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("0202")))
            .isFalse()
        assertThat(binRange.matches(CardNumber.Unvalidated("1004")))
            .isFalse()
    }
}
