package com.stripe.android.financialconnections.repository

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import com.stripe.android.financialconnections.utils.filterNotNullValues
import javax.inject.Inject

internal interface FinancialConnectionsRepository {
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession

    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun postCompleteFinancialConnectionsSessions(
        clientSecret: String,
        terminalError: String?
    ): FinancialConnectionsSession

    suspend fun postAuthorizationSessionOAuthResults(
        clientSecret: String,
        sessionId: String
    ): MixedOAuthParams
}

internal class FinancialConnectionsRepositoryImpl @Inject constructor(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiOptions: ApiRequest.Options,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsRepository {

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = listAccountsUrl,
            options = apiOptions,
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
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            financialConnectionsRequest,
            FinancialConnectionsSession.serializer()
        )
    }

    override suspend fun postCompleteFinancialConnectionsSessions(
        clientSecret: String,
        terminalError: String?
    ): FinancialConnectionsSession {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = completeUrl,
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret,
                "terminal_error" to terminalError
            ).filterNotNullValues()
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
            options = apiOptions,
            params = mapOf(
                NetworkConstants.PARAMS_ID to sessionId,
                NetworkConstants.PARAMS_CLIENT_SECRET to clientSecret
            )
        )
        return requestExecutor.execute(
            request,
            MixedOAuthParams.serializer()
        )
    }

    internal companion object {

        internal const val listAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/list_accounts"

        internal const val sessionReceiptUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/session_receipt"

        internal const val authorizationSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions"

        internal const val completeUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/complete"

        internal const val authorizationSessionOAuthResultsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/oauth_results"

        internal const val authorizeSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/authorized"
    }
}
