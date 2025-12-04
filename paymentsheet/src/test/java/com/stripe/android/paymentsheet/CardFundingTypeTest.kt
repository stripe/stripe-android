package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardFunding
import org.junit.Test

internal class CardFundingTypeTest {

    @Test
    fun `all CardFundingType entries should map to corresponding CardFunding`() {
        val expectedMappings = mapOf(
            PaymentSheet.CardFundingType.Debit to CardFunding.Debit,
            PaymentSheet.CardFundingType.Credit to CardFunding.Credit,
            PaymentSheet.CardFundingType.Prepaid to CardFunding.Prepaid,
            PaymentSheet.CardFundingType.Unknown to CardFunding.Unknown
        )

        for ((cardFundingType, expectedCardFunding) in expectedMappings) {
            assertThat(cardFundingType.cardFunding).isEqualTo(expectedCardFunding)
        }
    }
}
