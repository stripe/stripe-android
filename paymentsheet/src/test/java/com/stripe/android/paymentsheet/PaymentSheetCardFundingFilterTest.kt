package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.CardFunding
import org.junit.Test

internal class PaymentSheetCardFundingFilterTest {

    @Test
    fun `filter with all funding types accepted should return true`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Debit,
                PaymentSheet.CardFundingType.Credit,
                PaymentSheet.CardFundingType.Prepaid,
                PaymentSheet.CardFundingType.Unknown
            )
        )

        for (funding in CardFunding.entries) {
            assertThat(filter.isAccepted(funding)).isTrue()
        }
    }

    @Test
    fun `filter with only debit accepted should return true for debit and false for others`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Debit)
        )

        assertThat(filter.isAccepted(CardFunding.Debit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun `filter with only credit accepted should return true for credit and false for others`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Credit)
        )

        assertThat(filter.isAccepted(CardFunding.Credit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun `filter with only prepaid accepted should return true for prepaid and false for others`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Prepaid)
        )

        assertThat(filter.isAccepted(CardFunding.Prepaid)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun `filter with only unknown accepted should return true for unknown and false for others`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Unknown)
        )

        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
    }

    @Test
    fun `filter with empty list should reject all funding types`() {
        val filter = PaymentSheetCardFundingFilter(emptyList())

        for (funding in CardFunding.entries) {
            assertThat(filter.isAccepted(funding)).isFalse()
        }
    }
}
