package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.cards.AccountRangeFixtures
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
    fun `parse should default to Unknown funding when field is missing`() {
        val json =
            JSONObject(
                """
            {
                "account_range_high": "4242424249999999",
                "account_range_low": "4242424240000000",
                "pan_length": 16,
                "brand": "VISA",
                "country": "US"
            }
                """.trimIndent()
            )

        val result = accountRangeJsonParser.parse(json)

        assertThat(result?.funding).isEqualTo(CardFunding.Unknown)
    }

    @Test
    fun `parse should handle all funding types`() {
        val fundingTypes = listOf(
            "credit" to CardFunding.Credit,
            "CREDIT" to CardFunding.Credit,
            "debit" to CardFunding.Debit,
            "DEBIT" to CardFunding.Debit,
            "prepaid" to CardFunding.Prepaid,
            "PREPAID" to CardFunding.Prepaid,
            "unknown" to CardFunding.Unknown,
            "UNKNOWN" to CardFunding.Unknown
        )

        fundingTypes.forEach { (code, expected) ->
            val json =
                JSONObject(
                    """
                {
                    "account_range_high": "4242424249999999",
                    "account_range_low": "4242424240000000",
                    "pan_length": 16,
                    "brand": "VISA",
                    "funding": "$code"
                }
                    """.trimIndent()
                )

            val result = accountRangeJsonParser.parse(json)
            assertThat(result?.funding).isEqualTo(expected)
        }
    }
}
