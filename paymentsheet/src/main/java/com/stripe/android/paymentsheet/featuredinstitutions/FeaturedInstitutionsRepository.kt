package com.stripe.android.paymentsheet.featuredinstitutions

import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import javax.inject.Inject

internal fun interface FeaturedInstitutionsRepository {
    suspend fun retrieveFeaturedInstitutions(
        requestOptions: ApiRequest.Options,
        country: String?,
    ): Result<FeaturedInstitutionList>
}

internal class DefaultFeaturedInstitutionsRepository @Inject constructor(
    private val stripeNetworkClient: StripeNetworkClient,
) : FeaturedInstitutionsRepository {

    private val apiRequestFactory = ApiRequest.Factory()
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun retrieveFeaturedInstitutions(
        requestOptions: ApiRequest.Options,
        country: String?,
    ): Result<FeaturedInstitutionList> {
        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = apiRequestFactory.createGet(
                url = FEATURED_INSTITUTIONS_URL,
                options = requestOptions,
                params = mapOf(
                    "product" to "instant_debits",
                    // The server picks which country's institutions to return, so IBP can support more countries
                    // without an SDK update. Default to the US, where instant debits launched.
                    "country" to (country ?: "US"),
                    "limit" to FEATURED_INSTITUTIONS_LIMIT,
                ),
            ),
            responseJsonParser = FeaturedInstitutionListJsonParser,
        )
    }

    private companion object {
        const val FEATURED_INSTITUTIONS_LIMIT = 12

        val FEATURED_INSTITUTIONS_URL: String
            get() = "${ApiRequest.API_HOST}/v1/connections/featured_institutions"
    }
}
