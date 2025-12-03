package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.AccountRangeFixtures
import com.stripe.android.model.AccountRange
import com.stripe.android.model.BinRange
import com.stripe.android.model.CardFunding
import org.json.JSONObject
import kotlin.test.Test

class AccountRangeJsonParserTest {

    private val accountRangeJsonParser = AccountRangeJsonParser()

    @Test
    fun `serialize-deserialize roundtrip should return expected object`() {
        val accountRange = AccountRangeFixtures.DEFAULT.first()

        assertThat(
            accountRangeJsonParser.parse(
                accountRangeJsonParser.serialize(accountRange)
            )
        ).isEqualTo(accountRange)
    }

    @Test
    fun `parse should correctly parse funding field when present`() {
        val json = JSONObject("""
            {
                "account_range_high": "4242424249999999",
                "account_range_low": "4242424240000000",
                "pan_length": 16,
                "brand": "VISA",
                "country": "US",
                "funding": "credit"
            }
        """.trimIndent())

        val result = accountRangeJsonParser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.funding).isEqualTo(CardFunding.Credit)
    }

    @Test
    fun `parse should handle missing funding field`() {
        val json = JSONObject("""
            {
                "account_range_high": "4242424249999999",
                "account_range_low": "4242424240000000",
                "pan_length": 16,
                "brand": "VISA",
                "country": "US"
            }
        """.trimIndent())

        val result = accountRangeJsonParser.parse(json)

        assertThat(result).isNotNull()
        assertThat(result?.funding).isNull()
    }

    @Test
    fun `parse should handle all funding types`() {
        val fundingTypes = listOf(
            "credit" to CardFunding.Credit,
            "debit" to CardFunding.Debit,
            "prepaid" to CardFunding.Prepaid,
            "unknown" to CardFunding.Unknown
        )

        fundingTypes.forEach { (code, expected) ->
            val json = JSONObject("""
                {
                    "account_range_high": "4242424249999999",
                    "account_range_low": "4242424240000000",
                    "pan_length": 16,
                    "brand": "VISA",
                    "funding": "$code"
                }
            """.trimIndent())

            val result = accountRangeJsonParser.parse(json)
            assertThat(result?.funding).isEqualTo(expected)
        }
    }

    @Test
    fun `serialize should include funding field when present`() {
        val accountRange = AccountRange(
            binRange = BinRange(
                low = "4242424240000000",
                high = "4242424249999999"
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
            country = "US",
            funding = CardFunding.Debit
        )

        val json = accountRangeJsonParser.serialize(accountRange)

        assertThat(json.getString("funding")).isEqualTo("debit")
    }

    @Test
    fun `serialize should omit funding field when null`() {
        val accountRange = AccountRange(
            binRange = BinRange(
                low = "4242424240000000",
                high = "4242424249999999"
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
            country = "US",
            funding = null
        )

        val json = accountRangeJsonParser.serialize(accountRange)

        // JSONObject.put(key, null) doesn't add the key, so funding should not be present
        assertThat(json.has("funding")).isFalse()
    }

    @Test
    fun `serialize-deserialize roundtrip with funding should return expected object`() {
        val accountRange = AccountRange(
            binRange = BinRange(
                low = "4242424240000000",
                high = "4242424249999999"
            ),
            panLength = 16,
            brandInfo = AccountRange.BrandInfo.Visa,
            country = "GB",
            funding = CardFunding.Credit
        )

        assertThat(
            accountRangeJsonParser.parse(
                accountRangeJsonParser.serialize(accountRange)
            )
        ).isEqualTo(accountRange)
    }
}
