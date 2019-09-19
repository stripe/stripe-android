package com.stripe.android.model

import org.json.JSONObject

internal object TokenFixtures {

    @JvmField
    val CARD_TOKEN = Token.fromJson(JSONObject(
        """
        {
            "id": "tok_189fi32eZvKYlo2Ct0KZvU5Y",
            "object": "token",
            "card": {
                "id": "card_189fi32eZvKYlo2CHK8NPRME",
                "object": "card",
                "address_city": null,
                "address_country": null,
                "address_line1": null,
                "address_line1_check": null,
                "address_line2": null,
                "address_state": null,
                "address_zip": null,
                "address_zip_check": null,
                "brand": "Visa",
                "country": "US",
                "cvc_check": null,
                "dynamic_last4": null,
                "exp_month": 8,
                "exp_year": 2017,
                "funding": "credit",
                "last4": "4242",
                "metadata": {},
                "name": null,
                "tokenization_method": null
            },
            "client_ip": null,
            "created": 1462905355,
            "livemode": false,
            "type": "card",
            "used": false
        }
        """.trimIndent()
    ))!!

    @JvmField
    val BANK_TOKEN = Token.fromJson(JSONObject(
        """
        {
            "id": "btok_9xJAbronBnS9bH",
            "object": "token",
            "bank_account": {
                "id": "ba_19dOY72eZvKYlo2CVNPhmtv3",
                "object": "bank_account",
                "account_holder_name": "Jane Austen",
                "account_holder_type": "individual",
                "bank_name": "STRIPE TEST BANK",
                "country": "US",
                "currency": "usd",
                "fingerprint": "1JWtPxqbdX5Gamtc",
                "last4": "6789",
                "routing_number": "110000000",
                "status": "new"
            },
            "client_ip": null,
            "created": 1484765567,
            "livemode": false,
            "type": "bank_account",
            "used": false
        }
        """.trimIndent()
    ))!!
}
