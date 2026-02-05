package com.stripe.android.financialconnections.repository.api

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.utils.filterNotNullValues
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.parsers.ConsumerPaymentDetailsJsonParser
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface FinancialConnectionsConsumersApiService {

    suspend fun postConsumerSession(
        email: String,
        clientSecret: String,
        requestSurface: String,
    ): ConsumerSessionLookup

    suspend fun listPaymentDetails(
        consumerSessionClientSecret: String,
        requestSurface: String,
    ): ConsumerPaymentDetails

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiOptions: ApiRequest.Options,
            apiRequestFactory: ApiRequest.Factory,
        ): FinancialConnectionsConsumersApiService =
            FinancialConnectionsConsumersApiServiceImpl(
                requestExecutor,
                apiOptions,
                apiRequestFactory,
            )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
private class FinancialConnectionsConsumersApiServiceImpl(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiOptions: ApiRequest.Options,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsConsumersApiService {

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun postConsumerSession(
        email: String,
        clientSecret: String,
        requestSurface: String,
    ): ConsumerSessionLookup {
        val request = apiRequestFactory.createPost(
            consumerSessionsUrl,
            apiOptions,
            mapOf(
                "email_address" to email.lowercase(),
                "client_secret" to clientSecret,
                "request_surface" to requestSurface
            ).filterNotNullValues()
        )
        return requestExecutor.execute(
            request,
            ConsumerSessionLookup.serializer()
        )
    }

    override suspend fun listPaymentDetails(
        consumerSessionClientSecret: String,
        requestSurface: String,
    ): ConsumerPaymentDetails {
        val request = apiRequestFactory.createPost(
            listPaymentDetailsUrl,
            apiOptions,
            mapOf(
                "request_surface" to requestSurface,
                "credentials" to mapOf(
                    "consumer_session_client_secret" to consumerSessionClientSecret
                ),
                "types" to listOf("bank_account")
            )
        )
        val responseBody = requestExecutor.execute(request)
        return ConsumerPaymentDetailsJsonParser.parse(JSONObject(responseBody))
    }

    private companion object {
        /**
         * @return `https://api.stripe.com/v1/connections/link_account_sessions/consumer_sessions`
         */
        val consumerSessionsUrl: String
            get() = "${ApiRequest.API_HOST}/v1/connections/link_account_sessions/consumer_sessions"

        /**
         * @return `https://api.stripe.com/v1/consumers/payment_details/list`
         */
        val listPaymentDetailsUrl: String
            get() = "${ApiRequest.API_HOST}/v1/consumers/payment_details/list"
    }
}
