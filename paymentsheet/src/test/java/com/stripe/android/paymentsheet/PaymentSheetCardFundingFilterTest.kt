package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentSheetCardFundingFilter
import com.stripe.android.model.CardFunding
import org.junit.Test
import com.stripe.android.R as StripeR

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
    fun `filter with only debit accepted should return true for debit and unknown`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Debit)
        )

        assertThat(filter.isAccepted(CardFunding.Debit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
    }

    @Test
    fun `filter with only credit accepted should return true for credit and unknown`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Credit)
        )

        assertThat(filter.isAccepted(CardFunding.Credit)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
    }

    @Test
    fun `filter with only prepaid accepted should return true for prepaid and unknown`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Prepaid)
        )

        assertThat(filter.isAccepted(CardFunding.Prepaid)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
    }

    @Test
    fun `filter with only unknown in list should return true for unknown and false for others`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Unknown)
        )

        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
    }

    @Test
    fun `filter with no accepted types should reject all funding types except unknown`() {
        val filter = PaymentSheetCardFundingFilter(emptyList())

        assertThat(filter.isAccepted(CardFunding.Credit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Debit)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Prepaid)).isFalse()
        assertThat(filter.isAccepted(CardFunding.Unknown)).isTrue()
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns null when all three types are allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Credit,
                PaymentSheet.CardFundingType.Debit,
                PaymentSheet.CardFundingType.Prepaid
            )
        )

        assertThat(filter.allowedFundingTypesDisplayMessage()).isNull()
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns debit_credit when only credit and debit are allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Credit,
                PaymentSheet.CardFundingType.Debit
            )
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_debit_credit)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns credit_prepaid when only credit and prepaid are allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Credit,
                PaymentSheet.CardFundingType.Prepaid
            )
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_credit_prepaid)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns debit_prepaid when only debit and prepaid are allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Debit,
                PaymentSheet.CardFundingType.Prepaid
            )
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_debit_prepaid)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns credit when only credit is allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Credit)
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_credit)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns debit when only debit is allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Debit)
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_debit)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns prepaid when only prepaid is allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Prepaid)
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_prepaid)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns null when no types are allowed`() {
        val filter = PaymentSheetCardFundingFilter(emptyList())

        assertThat(filter.allowedFundingTypesDisplayMessage()).isNull()
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns null when only unknown is allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(PaymentSheet.CardFundingType.Unknown)
        )

        assertThat(filter.allowedFundingTypesDisplayMessage()).isNull()
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns credit when credit and unknown are allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Credit,
                PaymentSheet.CardFundingType.Unknown
            )
        )

        assertThat(filter.allowedFundingTypesDisplayMessage())
            .isEqualTo(StripeR.string.stripe_card_funding_only_credit)
    }

    @Test
    fun `allowedFundingTypesDisplayMessage returns null when all four types including unknown are allowed`() {
        val filter = PaymentSheetCardFundingFilter(
            listOf(
                PaymentSheet.CardFundingType.Credit,
                PaymentSheet.CardFundingType.Debit,
                PaymentSheet.CardFundingType.Prepaid,
                PaymentSheet.CardFundingType.Unknown
            )
        )

        assertThat(filter.allowedFundingTypesDisplayMessage()).isNull()
    }
}
