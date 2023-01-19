package com.stripe.android.repository

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithModelJsonParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface LinkApiService {

    suspend fun lookupConsumerSession(
        email: String?,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class LinkApiServiceImpl(
    private val stripeNetworkClient: StripeNetworkClient,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?
) : LinkApiService {

    private val stripeErrorJsonParser = StripeErrorJsonParser()

    private val apiRequestFactory = ApiRequest.Factory(
        appInfo = appInfo,
        apiVersion = apiVersion,
        sdkVersion = sdkVersion
    )

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun lookupConsumerSession(
        email: String?,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                consumerSessionLookupUrl,
                requestOptions,
                mapOf(
                    "request_surface" to "android_payment_element"
                ).plus(
                    email?.let {
                        mapOf(
                            "email_address" to it.lowercase()
                        )
                    } ?: emptyMap()
                ).plus(
                    authSessionCookie?.let {
                        mapOf(
                            "cookies" to
                                mapOf("verification_session_client_secrets" to listOf(it))
                        )
                    } ?: emptyMap()
                )
            ),
            responseJsonParser = ConsumerSessionLookupJsonParser()
        )
    }

    internal companion object {
        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/lookup`
         */
        internal val consumerSessionLookupUrl: String
            @JvmSynthetic
            get() = getApiUrl("consumers/sessions/lookup")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
