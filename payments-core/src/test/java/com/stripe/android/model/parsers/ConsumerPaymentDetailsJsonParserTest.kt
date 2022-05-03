package com.stripe.android.model.parsers

import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerFixtures
import com.stripe.android.model.ConsumerPaymentDetails
import org.junit.Test
import kotlin.test.assertEquals

class ConsumerPaymentDetailsJsonParserTest {

    @Test
    fun `parse single card payment details`() {
        assertEquals(
            ConsumerPaymentDetailsJsonParser()
                .parse(ConsumerFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS_JSON),
            ConsumerPaymentDetails(
                listOf(
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKJ6",
                        isDefault = true,
                        expiryYear = 2023,
                        expiryMonth = 12,
                        brand = CardBrand.MasterCard,
                        last4 = "4444"
                    )
                )
            )
        )
    }

    @Test
    fun `parse multiple card payment details`() {
        assertEquals(
            ConsumerPaymentDetailsJsonParser().parse(ConsumerFixtures.CONSUMER_PAYMENT_DETAILS_JSON),
            ConsumerPaymentDetails(
                listOf(
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKJ6",
                        isDefault = true,
                        expiryYear = 2023,
                        expiryMonth = 12,
                        brand = CardBrand.MasterCard,
                        last4 = "4444"
                    ),
                    ConsumerPaymentDetails.Card(
                        id = "QAAAKIL",
                        isDefault = false,
                        expiryYear = 2024,
                        expiryMonth = 4,
                        brand = CardBrand.Visa,
                        last4 = "4242"
                    )
                )
            )
        )
    }
}
