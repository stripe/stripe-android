package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConsumerPaymentDetailsCardFundingTest {

    @Test
    fun `link funding is mapped to CardFunding correctly`() {
        val expectedResults = mapOf(
            ConsumerPaymentDetails.Card.Funding.Credit to CardFunding.Credit,
            ConsumerPaymentDetails.Card.Funding.Debit to CardFunding.Debit,
            ConsumerPaymentDetails.Card.Funding.Prepaid to CardFunding.Prepaid,
            ConsumerPaymentDetails.Card.Funding.Unknown to CardFunding.Unknown,
        )

        expectedResults.forEach { (linkFunding, expectedCardFunding) ->
            assertThat(linkFunding.cardFunding).isEqualTo(expectedCardFunding)
        }
    }

    @Test
    fun `fromCode with CREDIT returns Credit`() {
        assertThat(ConsumerPaymentDetails.Card.Funding.fromCode("CREDIT"))
            .isEqualTo(ConsumerPaymentDetails.Card.Funding.Credit)
    }

    @Test
    fun `fromCode with DEBIT returns Debit`() {
        assertThat(ConsumerPaymentDetails.Card.Funding.fromCode("DEBIT"))
            .isEqualTo(ConsumerPaymentDetails.Card.Funding.Debit)
    }

    @Test
    fun `fromCode with PREPAID returns Prepaid`() {
        assertThat(ConsumerPaymentDetails.Card.Funding.fromCode("PREPAID"))
            .isEqualTo(ConsumerPaymentDetails.Card.Funding.Prepaid)
    }

    @Test
    fun `fromCode with UNKNOWN returns Unknown`() {
        assertThat(ConsumerPaymentDetails.Card.Funding.fromCode("UNKNOWN"))
            .isEqualTo(ConsumerPaymentDetails.Card.Funding.Unknown)
    }

    @Test
    fun `fromCode with null returns Unknown`() {
        assertThat(ConsumerPaymentDetails.Card.Funding.fromCode(null))
            .isEqualTo(ConsumerPaymentDetails.Card.Funding.Unknown)
    }

    @Test
    fun `fromCode with empty string returns Unknown`() {
        assertThat(ConsumerPaymentDetails.Card.Funding.fromCode(""))
            .isEqualTo(ConsumerPaymentDetails.Card.Funding.Unknown)
    }
}
