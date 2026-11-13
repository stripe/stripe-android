package com.stripe.android.paymentsheet.stripebalance

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.DEFAULT_API_CONFIG
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.networktesting.RequestMatchers.hasQueryParam
import com.stripe.android.networktesting.RequestMatchers.header
import com.stripe.android.networktesting.RequestMatchers.method
import com.stripe.android.networktesting.RequestMatchers.not
import com.stripe.android.networktesting.RequestMatchers.path
import com.stripe.android.networktesting.RequestMatchers.query
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DefaultStripeBalanceRepositoryTest {
    @get:Rule
    val networkRule = NetworkRule()

    private val repository = DefaultStripeBalanceRepository(DefaultStripeNetworkClient())

    @Test
    fun `lists the financial accounts with the CustomerSession and the payment's amount`() = runTest {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/stripe_balance/customer_session/financial_accounts"),
            header("Authorization", "Bearer ek_123"),
            header("Stripe-Account", ApiKeyFixtures.FAKE_ACCOUNT_ID),
            query("customer_session_client_secret", "cuss_123_secret_456"),
            query("amount", "5099"),
            query("currency", "gbp"),
            applyDefaultAuthorization = false,
        ) { response ->
            response.setBody(
                """
                {
                  "data": [
                    {
                      "id": "fa_123",
                      "display_name": "Operating account",
                      "balance": {"available": {"gbp": {"value": 10000, "currency": "gbp"}}},
                      "eligible": true,
                      "ineligible_reason": null
                    }
                  ]
                }
                """.trimIndent()
            )
        }

        val accounts = repository.listFinancialAccounts(
            customerSessionClientSecret = "cuss_123_secret_456",
            ephemeralKeySecret = "ek_123",
            amount = 5099,
            currency = "GBP",
            apiConfiguration = DEFAULT_API_CONFIG,
        ).getOrThrow()

        assertThat(accounts).containsExactly(
            StripeBalanceFinancialAccount(
                id = "fa_123",
                displayName = "Operating account",
                availableBalances = mapOf("gbp" to 10_000L),
                eligible = true,
                ineligibleReason = null,
            )
        )
    }

    @Test
    fun `lists the financial accounts without an amount when setting up`() = runTest {
        networkRule.enqueue(
            method("GET"),
            path("/v1/elements/stripe_balance/customer_session/financial_accounts"),
            query("customer_session_client_secret", "cuss_123_secret_456"),
            not(hasQueryParam("amount")),
            not(hasQueryParam("currency")),
            applyDefaultAuthorization = false,
        ) { response ->
            response.setBody("""{"data": [{"id": "fa_123", "balance": {"available": {}}, "eligible": null}]}""")
        }

        val accounts = repository.listFinancialAccounts(
            customerSessionClientSecret = "cuss_123_secret_456",
            ephemeralKeySecret = "ek_123",
            amount = null,
            currency = null,
            apiConfiguration = DEFAULT_API_CONFIG,
        ).getOrThrow()

        assertThat(accounts.single().eligible).isNull()
    }
}
