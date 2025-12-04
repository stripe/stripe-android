package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConsumerPaymentDetailsCardFundingTest {

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
}
