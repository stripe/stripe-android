package com.stripe.android.financialconnections.repository

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.FinancialConnectionsSheet.ElementsSessionContext.BillingDetails
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAccountsParams
import com.stripe.android.financialconnections.model.MixedOAuthParams
import com.stripe.android.financialconnections.model.PaymentMethod
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions
import com.stripe.android.financialconnections.utils.filterNotNullValues
import com.stripe.android.financialconnections.utils.toApiParams
import javax.inject.Inject

internal interface FinancialConnectionsRepository {
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAccountsParams
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

    suspend fun createPaymentMethod(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        billingDetails: BillingDetails?,
    ): PaymentMethod
}

internal class FinancialConnectionsRepositoryImpl @Inject constructor(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val provideApiRequestOptions: ProvideApiRequestOptions,
    private val fraudDetectionDataRepository: FraudDetectionDataRepository,
    private val apiRequestFactory: ApiRequest.Factory,
) : FinancialConnectionsRepository {

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAccountsParams: GetFinancialConnectionsAccountsParams
    ): FinancialConnectionsAccountList {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = listAccountsUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = getFinancialConnectionsAccountsParams.toParamMap()
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
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
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
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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

    override suspend fun createPaymentMethod(
        paymentDetailsId: String,
        consumerSessionClientSecret: String,
        billingDetails: BillingDetails?,
    ): PaymentMethod {
        val linkParams = mapOf(
            "type" to "link",
            "link" to mapOf(
                "credentials" to mapOf(
                    "consumer_session_client_secret" to consumerSessionClientSecret,
                ),
                "payment_details_id" to paymentDetailsId,
            ),
        )

        val billingParams = billingDetails?.let {
            mapOf("billing_details" to billingDetails.toApiParams())
        }.orEmpty()

        val fraudDetectionParams = fraudDetectionDataRepository.getCached()?.params.orEmpty()

        val request = apiRequestFactory.createPost(
            url = paymentMethodsUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = false),
            params = linkParams + billingParams + fraudDetectionParams,
        )

        return requestExecutor.execute(
            request,
            PaymentMethod.serializer()
        )
    }

    internal companion object {

        private const val listAccountsUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/list_accounts"

        private const val sessionReceiptUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/session_receipt"

        internal const val authorizationSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions"

        private const val completeUrl: String =
            "${ApiRequest.API_HOST}/v1/link_account_sessions/complete"

        private const val authorizationSessionOAuthResultsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/oauth_results"

        internal const val authorizeSessionUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/auth_sessions/authorized"

        private const val paymentMethodsUrl: String =
            "${ApiRequest.API_HOST}/v1/payment_methods"
    }
}
