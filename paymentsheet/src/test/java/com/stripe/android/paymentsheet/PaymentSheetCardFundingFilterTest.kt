package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.CardFunding
import org.junit.Test

internal class PaymentSheetCardFundingFilterTest {

    @Test
    fun testIsAccepted_allFundingTypesAccepted() {
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
    fun testIsAccepted_onlyDebitAccepted() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Debit)
        )

        assertThat(filter.isAccepted(CardFunding.Debit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun testIsAccepted_onlyCreditAccepted() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Credit)
        )

        assertThat(filter.isAccepted(CardFunding.Credit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun testIsAccepted_onlyPrepaidAccepted() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Prepaid)
        )

        assertThat(filter.isAccepted(CardFunding.Prepaid)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun testIsAccepted_onlyUnknownAccepted() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Unknown)
        )

        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
    }

    @Test
    fun testIsAccepted_multipleFundingTypesAccepted() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Debit,
                PaymentSheet.CardFundingType.Credit
            )
        )

        assertThat(filter.isAccepted(CardFunding.Debit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Credit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isFalse()
    }

    @Test
    fun testIsAccepted_emptyListRejectsAll() {
        val filter = PaymentSheetCardFundingFilter(emptyList())

        for (funding in CardFunding.entries) {
            assertThat(filter.isAccepted(funding)).isFalse()
        }
    }

    @Test
    fun testIsAccepted_nullFundingTreatedAsUnknown() {
        val filterAcceptingUnknown = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Unknown)
        )
        assertThat(filterAcceptingUnknown.isAccepted(null)).isTrue()

        val filterNotAcceptingUnknown = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Credit)
        )
        assertThat(filterNotAcceptingUnknown.isAccepted(null)).isFalse()
    }
}
