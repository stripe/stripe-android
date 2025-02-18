package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.TokenJsonParser
import com.stripe.android.model.parsers.TokenSerializer
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.util.Date
import kotlin.test.Test

class TokenTest {

    @Test
    fun `Card token is parsed correctly`() {
        assertThat(TokenFixtures.CARD_TOKEN)
            .isEqualTo(
                Token(
                    id = "tok_189fi32eZvKYlo2Ct0KZvU5Y",
                    livemode = false,
                    created = Date(1462905355L * 1000L),
                    used = false,
                    type = Token.Type.Card,
                    card = CARD
                )
            )
    }

    @Test
    fun `BankAccount token is parsed correctly`() {
        val expectedToken =
            Token(
                id = "btok_9xJAbronBnS9bH",
                livemode = false,
                created = Date(1484765567L * 1000L),
                used = false,
                type = Token.Type.BankAccount,
                bankAccount = BankAccountFixtures.BANK_ACCOUNT
            )
        assertThat(TokenFixtures.BANK_TOKEN)
            .isEqualTo(expectedToken)
        assertThat(Json.decodeFromString(TokenSerializer, TokenFixtures.BANK_TOKEN_JSON.toString()))
            .isEqualTo(expectedToken)
    }

    @Test
    fun `TokenSerializer operations are inverse`() {
        val expected = TokenFixtures.BANK_TOKEN
        assertThat(
            Json.decodeFromString(
                TokenSerializer,
                Json.encodeToString(TokenSerializer, expected)
            )
        )
            .isEqualTo(expected)
    }

    @Test
    fun `Token parser should require a non-null id`() {
        assertThat(TokenJsonParser().parse(RAW_TOKEN_NO_ID))
            .isNull()
    }

    @Test
    fun `Token parser should require a valid type id`() {
        assertThat(TokenJsonParser().parse(RAW_BANK_TOKEN_NO_TYPE))
            .isNull()
    }

    private companion object {
        private val CARD = Card(
            id = "card_189fi32eZvKYlo2CHK8NPRME",
            expMonth = 8,
            expYear = 2017,
            brand = CardBrand.Visa,
            country = "US",
            last4 = "4242",
            funding = CardFunding.Credit
        )

        private val RAW_TOKEN_NO_ID = JSONObject(
            """
            {
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
        )

        private val RAW_BANK_TOKEN_NO_TYPE = JSONObject(
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
                "used": false
            }
            """.trimIndent()
        )
    }
}
