package com.stripe.android.repository

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.Logger
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithModelJsonParser
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

interface LinkRepository {

    suspend fun lookupConsumerSession(
        email: String?,
        authSessionCookie: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup?
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class LinkAPIRepository(
    private val appInfo: AppInfo,
    private val workContext: CoroutineContext = Dispatchers.IO,
    private val logger: Logger = Logger.noop(),
    private val productUsageTokens: Set<String> = emptySet(),
    private val stripeNetworkClient: StripeNetworkClient = DefaultStripeNetworkClient(
        workContext = workContext,
        logger = logger
    )
) : LinkRepository {

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
            stripeErrorJsonParser = StripeErrorJsonParser(),
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