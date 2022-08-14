package com.stripe.android.financialconnections.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.ApiRequest.Companion.API_HOST
import com.stripe.android.financialconnections.di.PUBLISHABLE_KEY
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_ID
import javax.inject.Inject
import javax.inject.Named

internal class FinancialConnectionsRepositoryImpl @Inject constructor(
    @Named(PUBLISHABLE_KEY) publishableKey: String,
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsRepository {

    private val options = ApiRequest.Options(
        apiKey = publishableKey
    )

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = listAccountsUrl,
            options = options,
            params = getFinancialConnectionsAcccountsParams.toParamMap()
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsAccountList.serializer()
        )
    }

    override suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = sessionReceiptUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSession.serializer()
        )
    }

    override suspend fun postAuthorizationSessionOAuthResults(
        clientSecret: String,
        sessionId: String
    ): MixedOAuthParams {
        val request = apiRequestFactory.createPost(
            url = authorizationSessionOAuthResultsUrl,
            options = options,
            params = mapOf(
                PARAMS_ID to sessionId,
                PARAMS_CLIENT_SECRET to clientSecret,
            )
        )
        return requestExecutor.execute(
            request,
            MixedOAuthParams.serializer()
        )
    }

    internal companion object {

        internal const val listAccountsUrl: String =
            "$API_HOST/v1/link_account_sessions/list_accounts"

        internal const val sessionReceiptUrl: String =
            "$API_HOST/v1/link_account_sessions/session_receipt"

        internal const val authorizationSessionUrl: String =
            "$API_HOST/v1/connections/auth_sessions"

        internal const val authorizationSessionOAuthResultsUrl: String =
            "$API_HOST/v1/connections/auth_sessions/oauth_results"

        internal const val authorizeSessionUrl: String =
            "$API_HOST/v1/connections/auth_sessions/authorized"
    }
}
