package com.stripe.android.model

import com.stripe.android.model.parsers.CardJsonParser
import org.json.JSONObject

object CardFixtures {

    @JvmField
    val MINIMUM_CARD: Card = Card.create("4242424242424242", 1, 2050, "123")

    private const val CARD_ADDRESS_L1 = "123 Main Street"
    private const val CARD_ADDRESS_L2 = "906"
    private const val CARD_CITY = "San Francisco"
    private const val CARD_COUNTRY = "US"
    private const val CARD_CURRENCY = "USD"
    private const val CARD_CVC = "123"
    private const val CARD_NAME = "J Q Public"
    private const val CARD_NUMBER = "4242424242424242"
    private const val CARD_STATE = "CA"
    private const val CARD_ZIP = "94107"

    internal val CARD = Card.Builder(CARD_NUMBER, 8, 2019, CARD_CVC)
        .addressCity(CARD_CITY)
        .addressLine1(CARD_ADDRESS_L1)
        .addressLine2(CARD_ADDRESS_L2)
        .addressCountry(CARD_COUNTRY)
        .addressState(CARD_STATE)
        .addressZip(CARD_ZIP)
        .currency(CARD_CURRENCY)
        .name(CARD_NAME)
        .build()

    internal val CARD_USD = requireNotNull(CardJsonParser().parse(JSONObject(
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
            "metadata": {
                "color": "blue",
                "animal": "dog"
            }
        }
        """.trimIndent()
    )))

    internal val CARD_EUR = requireNotNull(CardJsonParser().parse(JSONObject(
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
            "currency": "eur",
            "customer": "customer77",
            "cvc_check": "unavailable",
            "exp_month": 8,
            "exp_year": 2017,
            "funding": "credit",
            "fingerprint": "abc123",
            "last4": "4242",
            "name": "John Cardholder",
            "metadata": {
                "color": "blue",
                "animal": "dog"
            }
        }
        """.trimIndent()
    )))

    internal val CARD_GOOGLE_PAY = requireNotNull(CardJsonParser().parse(JSONObject(
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
            "tokenization_method": "google_pay",
            "metadata": {
                "color": "blue",
                "animal": "dog"
            }
        }
        """.trimIndent()
    )))

    internal val CARD_WITH_ATTRIBUTION = Card.Builder()
        .loggingTokens(setOf("CardInputView"))
        .build()
}
