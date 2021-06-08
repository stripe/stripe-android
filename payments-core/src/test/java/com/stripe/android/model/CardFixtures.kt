package com.stripe.android.model

import com.stripe.android.model.parsers.CardJsonParser
import org.json.JSONObject

object CardFixtures {
    internal val CARD = Card(
        expMonth = 8,
        expYear = 2050,
        addressLine1 = AddressFixtures.ADDRESS.line1,
        addressLine2 = AddressFixtures.ADDRESS.line2,
        addressCity = AddressFixtures.ADDRESS.city,
        addressCountry = AddressFixtures.ADDRESS.country,
        addressState = AddressFixtures.ADDRESS.state,
        addressZip = AddressFixtures.ADDRESS.postalCode,
        currency = "USD",
        name = "Jenny Rosen",
        brand = CardBrand.Visa,
        last4 = "4242",
        id = "id"
    )

    internal val CARD_USD_JSON = JSONObject(
        """
        {
            "id": "card_189fi32eZvKYlo2CHK8NPRME",
            "object": "card",
            "address_city": "San Francisco",
            "address_country": "US",
            "address_line1": "123 Market St",
            "address_line1_check": "unavailable",
            "address_line2": "#345",
            "address_state": "CA",
            "address_zip": "94107",
            "address_zip_check": "unavailable",
            "brand": "Visa",
            "country": "US",
            "currency": "usd",
            "customer": "customer77",
            "cvc_check": "unavailable",
            "exp_month": 8,
            "exp_year": 2017,
            "funding": "credit",
            "fingerprint": "abc123",
            "last4": "4242",
            "name": "Jenny Rosen",
            "metadata": {
                "color": "blue",
                "animal": "dog"
            }
        }
        """.trimIndent()
    )

    internal val CARD_USD = requireNotNull(CardJsonParser().parse(CARD_USD_JSON))

    internal val CARD_GOOGLE_PAY = requireNotNull(
        CardJsonParser().parse(
            JSONObject(
                """
        {
            "id": "card_189fi32eZvKYlo2CHK8NPRME",
            "object": "card",
            "address_city": "Des Moines",
            "address_country": "US",
            "address_line1": "123 Any Street",
            "address_line1_check": "unavailable",
            "address_line2": "456",
            "address_state": "IA",
            "address_zip": "50305",
            "address_zip_check": "unavailable",
            "brand": "Visa",
            "country": "US",
            "currency": "usd",
            "customer": "customer77",
            "cvc_check": "unavailable",
            "exp_month": 8,
            "exp_year": 2017,
            "funding": "credit",
            "fingerprint": "abc123",
            "last4": "4242",
            "name": "John Cardholder",
            "tokenization_method": "android_pay",
            "metadata": {
                "color": "blue",
                "animal": "dog"
            }
        }
                """.trimIndent()
            )
        )
    )
}
