package com.stripe.android.crypto.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.networking.toMap
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.crypto.onramp.model.CryptoCustomerRequestParams
import com.stripe.android.crypto.onramp.model.CryptoCustomerResponse
import com.stripe.android.crypto.onramp.model.parsers.CryptoCustomerJsonParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject
import javax.inject.Named

/*
* Repository interface for crypto-related operations.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal class CryptoApiRepository @Inject internal constructor(
    private val stripeNetworkClient: StripeNetworkClient,
    @Named(PUBLISHABLE_KEY) private val publishableKeyProvider: () -> String,
    @Named(STRIPE_ACCOUNT_ID) private val stripeAccountIdProvider: () -> String?,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?
) {
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    suspend fun grantPartnerMerchantPermissions(
        consumerSessionClientSecret: String
    ): Result<CryptoCustomerResponse> {
        val params = CryptoCustomerRequestParams(CryptoCustomerRequestParams.Credentials(consumerSessionClientSecret))
        val request = apiRequestFactory.createPost(
            url = getGrantPartnerMerchantPermissionsUrl,
            options = buildRequestOptions(),
            params = Json.encodeToJsonElement(params).toMap()
        )

        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = request,
            responseJsonParser = CryptoCustomerJsonParser()
        )
    }

    private fun buildRequestOptions(): ApiRequest.Options {
        return ApiRequest.Options(
            apiKey = publishableKeyProvider(),
            stripeAccount = stripeAccountIdProvider(),
        )
    }

    internal companion object {
        /**
         * @return `https://api.stripe.com/v1/crypto/internal/customers`
         */
        internal val getGrantPartnerMerchantPermissionsUrl: String = getApiUrl("crypto/internal/customers")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
