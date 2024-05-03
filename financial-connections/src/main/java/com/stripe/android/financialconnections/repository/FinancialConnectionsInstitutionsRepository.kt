package com.stripe.android.financialconnections.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET

internal interface FinancialConnectionsInstitutionsRepository {

    suspend fun featuredInstitutions(
        clientSecret: String,
    ): InstitutionResponse

    suspend fun searchInstitutions(
        clientSecret: String,
        query: String,
        limit: Int
    ): InstitutionResponse

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiOptions: ApiRequest.Options,
            apiRequestFactory: ApiRequest.Factory
        ): FinancialConnectionsInstitutionsRepository = FinancialConnectionsInstitutionsRepositoryImpl(
            requestExecutor,
            apiOptions,
            apiRequestFactory
        )
    }
}

private class FinancialConnectionsInstitutionsRepositoryImpl(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiOptions: ApiRequest.Options,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsInstitutionsRepository {

    override suspend fun featuredInstitutions(
        clientSecret: String,
    ): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = featuredInstitutionsUrl,
            options = apiOptions,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
            )
        )
        return requestExecutor.execute(
            request,
            InstitutionResponse.serializer()
        )
    }

    override suspend fun searchInstitutions(
        clientSecret: String,
        query: String,
        limit: Int

    ): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = institutionsUrl,
            options = apiOptions,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                "query" to query,
                "limit" to limit
            )
        )
        return requestExecutor.execute(
            request,
            InstitutionResponse.serializer()
        )
    }

    companion object {
        internal const val institutionsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/institutions"

        internal const val featuredInstitutionsUrl: String =
            "${ApiRequest.API_HOST}/v1/connections/featured_institutions"
    }
}
