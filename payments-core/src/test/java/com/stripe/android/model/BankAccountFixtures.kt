package com.stripe.android.model

import com.stripe.android.model.parsers.BankAccountJsonParser
import org.json.JSONObject

internal object BankAccountFixtures {
    val BANK_ACCOUNT = BankAccountJsonParser().parse(
        JSONObject(
            """
        {
            "id": "ba_19d8Fh2eZvKYlo2C9qw8RwpV",
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
        }
            """.trimIndent()
        )
    )
}
