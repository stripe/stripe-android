package com.stripe.android.paymentsheet.stripebalance

import com.stripe.android.Stripe
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import javax.inject.Inject

internal interface StripeBalanceRepository {
    /**
     * Lists the financial accounts of the CustomerSession's customer that Stripe balance can use. Given both [amount]
     * and [currency], each account is tagged with whether it can cover the payment.
     */
    suspend fun listFinancialAccounts(
        customerSessionClientSecret: String,
        ephemeralKeySecret: String,
        amount: Long?,
        currency: String?,
        apiConfiguration: ApiConfiguration.State,
    ): Result<List<StripeBalanceFinancialAccount>>
}

internal class DefaultStripeBalanceRepository @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
) : StripeBalanceRepository {

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = Stripe.appInfo,
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
    )
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun listFinancialAccounts(
        customerSessionClientSecret: String,
        ephemeralKeySecret: String,
        amount: Long?,
        currency: String?,
        apiConfiguration: ApiConfiguration.State,
    ): Result<List<StripeBalanceFinancialAccount>> {
        val params = buildMap {
            put("customer_session_client_secret", customerSessionClientSecret)
            // The server requires both or neither
            if (amount != null && currency != null) {
                put("amount", amount)
                put("currency", currency.lowercase())
            }
        }
        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createGet(
                url = FINANCIAL_ACCOUNTS_URL,
                options = ApiRequest.Options(
                    apiKey = ephemeralKeySecret,
                    stripeAccount = apiConfiguration.stripeAccountId,
                    betas = apiConfiguration.betas.toSet(),
                ),
                params = params,
            ),
            responseJsonParser = StripeBalanceFinancialAccountListJsonParser,
        ).map { it.data }
    }

    private companion object {
        val FINANCIAL_ACCOUNTS_URL: String
            get() = "${ApiRequest.API_HOST}/v1/elements/stripe_balance/customer_session/financial_accounts"
    }
}
