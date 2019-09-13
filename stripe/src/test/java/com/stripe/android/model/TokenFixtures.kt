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
        "{\n" +
            "  \"id\": \"btok_9xJAbronBnS9bH\",\n" +
            "  \"object\": \"token\",\n" +
            "  \"bank_account\": {\n" +
            "    \"id\": \"ba_19dOY72eZvKYlo2CVNPhmtv3\",\n" +
            "    \"object\": \"bank_account\",\n" +
            "    \"account_holder_name\": \"Jane Austen\",\n" +
            "    \"account_holder_type\": \"individual\",\n" +
            "    \"bank_name\": \"STRIPE TEST BANK\",\n" +
            "    \"country\": \"US\",\n" +
            "    \"currency\": \"usd\",\n" +
            "    \"fingerprint\": \"1JWtPxqbdX5Gamtc\",\n" +
            "    \"last4\": \"6789\",\n" +
            "    \"routing_number\": \"110000000\",\n" +
            "    \"status\": \"new\"\n" +
            "  },\n" +
            "  \"client_ip\": null,\n" +
            "  \"created\": 1484765567,\n" +
            "  \"livemode\": false,\n" +
            "  \"type\": \"bank_account\",\n" +
            "  \"used\": false\n" +
            "}"
    ))!!
}
