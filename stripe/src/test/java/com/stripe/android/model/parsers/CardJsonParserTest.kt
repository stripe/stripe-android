package com.stripe.android.model.parsers

import com.stripe.android.model.CardFixtures
import com.stripe.android.model.TokenizationMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class CardJsonParserTest {
    @Test
    fun parseGooglePayCard() {
        assertEquals(
            TokenizationMethod.GooglePay,
            CardFixtures.CARD_GOOGLE_PAY.tokenizationMethod
        )
    }
}
