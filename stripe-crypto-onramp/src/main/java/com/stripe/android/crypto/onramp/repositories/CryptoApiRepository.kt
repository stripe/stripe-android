package com.stripe.android.crypto.onramp.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithResultParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.CryptoCustomerRequestParams
import com.stripe.android.model.CryptoCustomerResponse
import com.stripe.android.model.parsers.CryptoCustomerJsonParser

/*
* Repository interface for crypto-related operations.
*/
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CryptoApiRepository(
    private val stripeNetworkClient: StripeNetworkClient,
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
        consumerSessionClientSecret: String,
        options: ApiRequest.Options
    ): Result<CryptoCustomerResponse> {
        val request = apiRequestFactory.createPost(
            url = getGrantPartnerMerchantPermissionsUrl,
            options = options,
            params = CryptoCustomerRequestParams(consumerSessionClientSecret).toParamMap()
        )

        return executeRequestWithResultParser(
            stripeNetworkClient = stripeNetworkClient,
            stripeErrorJsonParser = stripeErrorJsonParser,
            request = request,
            responseJsonParser = CryptoCustomerJsonParser()
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