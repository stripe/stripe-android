package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFunding
import com.stripe.android.model.SourceFixtures
import com.stripe.android.model.SourceTypeModel
import com.stripe.android.model.TokenizationMethod
import kotlin.test.Test

class SourceCardDataJsonParserTest {
    @Test
    fun `should parse correctly`() {
        assertThat(CARD_DATA)
            .isEqualTo(
                SourceTypeModel.Card(
                    brand = CardBrand.Visa,
                    funding = CardFunding.Credit,
                    last4 = "4242",
                    expiryMonth = 12,
                    expiryYear = 2050,
                    country = "US",
                    addressLine1Check = "unchecked",
                    addressZipCheck = "unchecked",
                    cvcCheck = "unchecked",
                    dynamicLast4 = "4242",
                    threeDSecureStatus = SourceTypeModel.Card.ThreeDSecureStatus.Optional,
                    tokenizationMethod = TokenizationMethod.ApplePay
                )
            )
    }

    @Test
    fun `should implement equals correctly`() {
        assertThat(PARSER.parse(SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON))
            .isEqualTo(CARD_DATA)
    }

    @Test
    fun `should implement hashCode correctly`() {
        assertThat(
            PARSER.parse(SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON).hashCode()
        ).isEqualTo(CARD_DATA.hashCode())
    }

    private companion object {
        private val PARSER = SourceCardDataJsonParser()
        private val CARD_DATA =
            PARSER.parse(SourceFixtures.SOURCE_CARD_DATA_WITH_APPLE_PAY_JSON)
    }
}
