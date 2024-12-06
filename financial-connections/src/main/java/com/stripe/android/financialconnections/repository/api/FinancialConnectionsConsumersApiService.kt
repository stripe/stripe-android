package com.stripe.android.financialconnections.repository.api

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.UpdateAvailableIncentives
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.utils.filterNotNullValues
import com.stripe.android.model.ConsumerSessionLookup

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface FinancialConnectionsConsumersApiService {

    suspend fun postConsumerSession(
        email: String,
        clientSecret: String,
        requestSurface: String,
    ): ConsumerSessionLookup

    suspend fun updateAvailableIncentives(
        sessionId: String,
        consumerAccount: String,
    ): UpdateAvailableIncentives

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

    override suspend fun updateAvailableIncentives(
        sessionId: String,
        consumerAccount: String,
    ): UpdateAvailableIncentives {
        val request = apiRequestFactory.createPost(
            updateAvailableIncentivesUrl,
            apiOptions,
            mapOf(
                "session_id" to sessionId,
                "consumer_account" to consumerAccount,
            ).filterNotNullValues()
        )
        return requestExecutor.execute(
            request,
            UpdateAvailableIncentives.serializer()
        )
    }

    private companion object {
        /**
         * @return `https://api.stripe.com/v1/connections/link_account_sessions/consumer_sessions`
         */
        const val consumerSessionsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/link_account_sessions/consumer_sessions"

        const val updateAvailableIncentivesUrl: String =
            "${ApiRequest.API_HOST}/v1/consumers/incentives/update_available"
    }
}
