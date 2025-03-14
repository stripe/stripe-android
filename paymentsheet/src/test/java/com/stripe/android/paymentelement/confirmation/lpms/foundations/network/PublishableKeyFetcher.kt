package com.stripe.android.paymentelement.confirmation.lpms.foundations.network

internal object PublishableKeyFetcher {
    private const val PUBLISHABLE_KEY_PATH = "publishable_key"
    private const val ACCOUNT_PARAMETER = "account"

    suspend fun publishableKey(
        country: MerchantCountry,
    ): Result<String> {
        val result = executeFuelGetRequest(
            url = STRIPE_CI_TEST_BACKEND_URL + PUBLISHABLE_KEY_PATH,
            parameters = listOf(
                ACCOUNT_PARAMETER to country.value,
            ),
            responseDeserializer = PublishableKeyRequest.Response.serializer(),
        )

        return result.mapCatching { response ->
            response.publishableKey
        }
    }
}
