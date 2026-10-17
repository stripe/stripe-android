package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

internal class CardFundingTest {
    @Test
    fun `given valid code, fromCode should return correct CardFunding`() {
        CardFunding.entries.forEach {
            assertThat(CardFunding.fromCode(it.code)).isEqualTo(it)
        }
    }

    @Test
    fun `fromCode should be case insensitive`() {
        assertThat(CardFunding.fromCode("credit")).isEqualTo(CardFunding.Credit)
        assertThat(CardFunding.fromCode("CREDIT")).isEqualTo(CardFunding.Credit)
        assertThat(CardFunding.fromCode("Credit")).isEqualTo(CardFunding.Credit)
        assertThat(CardFunding.fromCode("DEBIT")).isEqualTo(CardFunding.Debit)
        assertThat(CardFunding.fromCode("PREPAID")).isEqualTo(CardFunding.Prepaid)
    }

    @Test
    fun `given null code, fromCode should return Unknown`() {
        assertThat(CardFunding.fromCode(null)).isEqualTo(CardFunding.Unknown)
    }

    @Test
    fun `given invalid code, fromCode should return Unknown`() {
        assertThat(CardFunding.fromCode("invalid")).isEqualTo(CardFunding.Unknown)
        assertThat(CardFunding.fromCode("")).isEqualTo(CardFunding.Unknown)
    }
}
