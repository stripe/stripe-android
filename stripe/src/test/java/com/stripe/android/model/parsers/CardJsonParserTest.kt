package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Card
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardFixtures
import com.stripe.android.model.CardFunding
import com.stripe.android.model.TokenizationMethod
import kotlin.test.Test

class CardJsonParserTest {
    @Test
    fun parseGooglePayCard() {
        assertThat(CardFixtures.CARD_GOOGLE_PAY.tokenizationMethod)
            .isEqualTo(TokenizationMethod.GooglePay)
    }

    @Test
    fun `parse returns expected Card`() {
        assertThat(
            CardJsonParser().parse(CardFixtures.CARD_USD_JSON)
        ).isEqualTo(
            Card(
                id = "card_189fi32eZvKYlo2CHK8NPRME",
                expYear = 2017,
                expMonth = 8,
                cvcCheck = "unavailable",
                brand = CardBrand.Visa,
                addressLine1 = "123 Market St",
                addressLine1Check = "unavailable",
                addressLine2 = "#345",
                addressCity = "San Francisco",
                addressState = "CA",
                addressZip = "94107",
                addressZipCheck = "unavailable",
                addressCountry = "US",
                country = "US",
                currency = "usd",
                customerId = "customer77",
                funding = CardFunding.Credit,
                fingerprint = "abc123",
                last4 = "4242",
                name = "Jenny Rosen"
            )
        )
    }
}
