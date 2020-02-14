package com.stripe.android.model

import com.stripe.android.model.parsers.TokenJsonParser
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.json.JSONObject

class TokenTest {

    @Test
    fun parseToken_whenCardToken_readsObjectCorrectly() {
        val expectedToken = Token(
            id = "tok_189fi32eZvKYlo2Ct0KZvU5Y",
            livemode = false,
            created = Date(1462905355L * 1000L),
            used = false,
            type = Token.TokenType.CARD,
            card = CARD
        )
        assertEquals(expectedToken, TokenFixtures.CARD_TOKEN)
    }

    @Test
    fun parseToken_whenNullString_returnsNull() {
        assertNull(Token.fromString(null))
    }

    @Test
    fun parseToken_whenBankAccount_readsObject() {
        val createdDate = Date(1484765567L * 1000L)
        val expectedToken = Token(
            id = "btok_9xJAbronBnS9bH",
            livemode = false,
            created = createdDate,
            used = false,
            type = Token.TokenType.BANK_ACCOUNT,
            bankAccount = BankAccountFixtures.BANK_ACCOUNT
        )
        val answerToken = TokenFixtures.BANK_TOKEN
        assertNotNull(answerToken)
        assertEquals(expectedToken.id, answerToken.id)
        assertEquals(expectedToken.livemode, answerToken.livemode)
        assertEquals(expectedToken.created, answerToken.created)
        assertEquals(expectedToken.used, answerToken.used)
        assertEquals(Token.TokenType.BANK_ACCOUNT, answerToken.type)

        assertNotNull(answerToken.bankAccount)
        assertNull(answerToken.card)
    }

    @Test
    fun parseToken_withoutId_returnsNull() {
        val token = TokenJsonParser().parse(RAW_TOKEN_NO_ID)
        assertNull(token)
    }

    @Test
    fun parseToken_withoutType_returnsNull() {
        val token = TokenJsonParser().parse(RAW_BANK_TOKEN_NO_TYPE)
        assertNull(token)
    }

    private companion object {
        private val CARD = Card.Builder(expMonth = 8, expYear = 2017)
            .id("card_189fi32eZvKYlo2CHK8NPRME")
            .brand(CardBrand.Visa)
            .country("US")
            .last4("4242")
            .funding(CardFunding.Credit)
            .metadata(emptyMap())
            .build()

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
