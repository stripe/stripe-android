package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConsumerFixtures
import com.stripe.android.model.ConsumerPaymentDetailsShare
import com.stripe.android.model.PaymentMethod
import org.junit.Test

internal class ConsumerPaymentDetailsShareJsonParserTest {
    @Test
    fun `parse result`() {
        val expected = ConsumerPaymentDetailsShare(
            paymentMethod = PaymentMethod(
                id = "pm_1NsnWALu5o3P18Zp36Q7YfWW",
                created = 1550757934255L,
                liveMode = true,
                type = PaymentMethod.Type.Card,
                billingDetails = PaymentMethod.BillingDetails(
                    address = Address(
                        city = "San Francisco",
                        country = "US",
                        line1 = "1234 Main Street",
                        postalCode = "94111",
                        state = "CA"
                    ),
                    email = "jenny.rosen@example.com",
                    name = "Jenny Rosen",
                    phone = "123-456-7890"
                ),
                customerId = "cus_AQsHpvKfKwJDrF",
                card = PaymentMethod.Card(
                    brand = CardBrand.Visa,
                    checks = PaymentMethod.Card.Checks(
                        addressLine1Check = "unchecked",
                        addressPostalCodeCheck = null,
                        cvcCheck = "unchecked"
                    ),
                    country = "US",
                    expiryMonth = 8,
                    expiryYear = 2022,
                    funding = "credit",
                    fingerprint = "fingerprint123",
                    last4 = "4242",
                    threeDSecureUsage = PaymentMethod.Card.ThreeDSecureUsage(
                        isSupported = true
                    ),
                    wallet = null
                ),
                code = "card"
            )
        )
        
        assertThat(
            ConsumerPaymentDetailsShareJsonParser
                .parse(ConsumerFixtures.PAYMENT_DETAILS_SHARE_JSON)
        ).isEqualTo(expected)
    }
}
