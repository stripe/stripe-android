package com.stripe.android.financialconnections.repository

import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.financialconnections.network.NetworkConstants.PARAMS_CLIENT_SECRET
import com.stripe.android.financialconnections.repository.api.ProvideApiRequestOptions

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
            provideApiRequestOptions: ProvideApiRequestOptions,
            apiRequestFactory: ApiRequest.Factory
        ): FinancialConnectionsInstitutionsRepository = FinancialConnectionsInstitutionsRepositoryImpl(
            requestExecutor,
            provideApiRequestOptions,
            apiRequestFactory
        )
    }
}

private class FinancialConnectionsInstitutionsRepositoryImpl(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val provideApiRequestOptions: ProvideApiRequestOptions,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsInstitutionsRepository {

    override suspend fun featuredInstitutions(
        clientSecret: String,
    ): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = featuredInstitutionsUrl,
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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
            options = provideApiRequestOptions(useConsumerPublishableKey = true),
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
