package com.stripe.android.financialconnections.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET

internal interface FinancialConnectionsInstitutionsRepository {

    suspend fun featuredInstitutions(clientSecret: String): InstitutionResponse
    suspend fun searchInstitutions(clientSecret: String, query: String): InstitutionResponse

    companion object {
        operator fun invoke(
            publishableKey: String,
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiRequestFactory: ApiRequest.Factory
        ): FinancialConnectionsInstitutionsRepository = FinancialConnectionsInstitutionsRepositoryImpl(
            publishableKey,
            requestExecutor,
            apiRequestFactory
        )
    }
}

private class FinancialConnectionsInstitutionsRepositoryImpl(
    publishableKey: String,
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsInstitutionsRepository {

    private val options = ApiRequest.Options(
        apiKey = publishableKey
    )

    override suspend fun featuredInstitutions(clientSecret: String): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = featuredInstitutionsUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                "limit" to SEARCH_INSTITUTIONS_LIMIT
            )
        )
        return requestExecutor.execute(
            request,
            InstitutionResponse.serializer()
        )
    }

    override suspend fun searchInstitutions(
        clientSecret: String,
        query: String
    ): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = institutionsUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                "query" to query,
                "limit" to SEARCH_INSTITUTIONS_LIMIT
            )
        )
        return requestExecutor.execute(
            request,
            InstitutionResponse.serializer()
        )
    }

    companion object {
        private const val SEARCH_INSTITUTIONS_LIMIT = 8

        internal const val institutionsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/institutions"

        internal const val featuredInstitutionsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/featured_institutions"
    }
}
