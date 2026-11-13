package com.stripe.android.paymentsheet.stripebalance

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class StripeBalanceFinancialAccountListJsonParserTest {
    @Test
    fun `parses the financial accounts response`() {
        val json = JSONObject(
            """
            {
              "object": "list",
              "has_more": false,
              "data": [
                {
                  "object": "stripe_balance.financial_account",
                  "id": "fa_operating",
                  "display_name": "Operating account",
                  "status": "OPEN",
                  "balance": {"available": {"gbp": {"value": 25000, "currency": "gbp"}}},
                  "eligible": true,
                  "ineligible_reason": null
                },
                {
                  "id": "fa_reserve",
                  "display_name": null,
                  "balance": {"available": {"GBP": {"value": 2500, "currency": "gbp"}}},
                  "eligible": false,
                  "ineligible_reason": "insufficient_balance"
                }
              ]
            }
            """.trimIndent()
        )

        val accounts = StripeBalanceFinancialAccountListJsonParser.parse(json)?.data

        assertThat(accounts).containsExactly(
            StripeBalanceFinancialAccount(
                id = "fa_operating",
                displayName = "Operating account",
                availableBalances = mapOf("gbp" to 25000L),
                eligible = true,
                ineligibleReason = null,
            ),
            StripeBalanceFinancialAccount(
                id = "fa_reserve",
                displayName = null,
                availableBalances = mapOf("gbp" to 2500L),
                eligible = false,
                ineligibleReason = StripeBalanceFinancialAccount.IneligibleReason.InsufficientBalance,
            ),
        ).inOrder()
    }

    @Test
    fun `parses accounts without eligibility or with reasons the SDK doesn't know`() {
        val json = JSONObject(
            """
            {
              "data": [
                {"id": "fa_untagged", "balance": {"available": {}}},
                {"id": "fa_future", "balance": {"available": {}}, "eligible": false, "ineligible_reason": "frozen"}
              ]
            }
            """.trimIndent()
        )

        val accounts = StripeBalanceFinancialAccountListJsonParser.parse(json)?.data.orEmpty()

        assertThat(accounts.map { it.eligible }).containsExactly(null, false).inOrder()
        assertThat(accounts[0].isSelectable).isTrue()
        assertThat(accounts[1].ineligibleReason)
            .isEqualTo(StripeBalanceFinancialAccount.IneligibleReason.Other("frozen"))
        assertThat(accounts[1].isSelectable).isFalse()
    }

    @Test
    fun `fails the whole response when any account is malformed`() {
        val validAccount = """{"id": "fa_valid", "balance": {"available": {"gbp": {"value": 2500}}}}"""
        val malformedAccounts = listOf(
            """{"balance": {"available": {}}}""",
            """{"id": "fa_no_balance"}""",
            """{"id": "fa_bad_value", "balance": {"available": {"gbp": {"value": "lots"}}}}""",
        )

        malformedAccounts.forEach { malformedAccount ->
            val json = JSONObject("""{"data": [$validAccount, $malformedAccount]}""")

            assertThat(StripeBalanceFinancialAccountListJsonParser.parse(json)).isNull()
        }
    }
}
