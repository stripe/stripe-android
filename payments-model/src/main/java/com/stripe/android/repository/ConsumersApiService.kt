package com.stripe.android.repository

import androidx.annotation.RestrictTo
import com.stripe.android.core.AppInfo
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.executeRequestWithModelJsonParser
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import com.stripe.android.model.parsers.ConsumerSessionJsonParser
import com.stripe.android.model.parsers.ConsumerSessionLookupJsonParser
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface ConsumersApiService {

    suspend fun lookupConsumerSession(
        email: String?,
        authSessionCookie: String?,
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup

    suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        authSessionCookie: String?,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        requestOptions: ApiRequest.Options
    ): ConsumerSession

    suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?,
        requestSurface: String,
        type: VerificationType,
        requestOptions: ApiRequest.Options
    ): ConsumerSession
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class ConsumersApiServiceImpl(
    private val stripeNetworkClient: StripeNetworkClient,
    apiVersion: String,
    sdkVersion: String = StripeSdkVersion.VERSION,
    appInfo: AppInfo?
) : ConsumersApiService {

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
        requestSurface: String,
        requestOptions: ApiRequest.Options
    ): ConsumerSessionLookup {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                consumerSessionLookupUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface
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

    /**
     * Triggers a verification for the consumer corresponding to the given client secret.
     */
    override suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        authSessionCookie: String?,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
        requestOptions: ApiRequest.Options,
    ): ConsumerSession {
        return executeRequestWithModelJsonParser(
            stripeErrorJsonParser = stripeErrorJsonParser,
            stripeNetworkClient = stripeNetworkClient,
            request = apiRequestFactory.createPost(
                startConsumerVerificationUrl,
                requestOptions,
                mapOf(
                    "request_surface" to requestSurface,
                    "credentials" to mapOf(
                        "consumer_session_client_secret" to consumerSessionClientSecret
                    ),
                    "type" to type.value,
                    "custom_email_type" to customEmailType?.value,
                    "connections_merchant_name" to connectionsMerchantName,
                    "locale" to locale.toLanguageTag()
                )
                    .filterValues { it != null }
                    .plus(
                        authSessionCookie?.let {
                            mapOf(
                                "cookies" to
                                    mapOf("verification_session_client_secrets" to listOf(it))
                            )
                        } ?: emptyMap()
                    )
            ),
            responseJsonParser = ConsumerSessionJsonParser()
        )
    }

    /**
     * Confirms an SMS verification for the consumer corresponding to the given client secret.
     */
    override suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?,
        requestSurface: String,
        type: VerificationType,
        requestOptions: ApiRequest.Options
    ): ConsumerSession = executeRequestWithModelJsonParser(
        stripeErrorJsonParser = stripeErrorJsonParser,
        stripeNetworkClient = stripeNetworkClient,
        request = apiRequestFactory.createPost(
            confirmConsumerVerificationUrl,
            requestOptions,
            mapOf(
                "request_surface" to requestSurface,
                "credentials" to mapOf(
                    "consumer_session_client_secret" to consumerSessionClientSecret
                ),
                "type" to type.value,
                "code" to verificationCode
            ).plus(
                authSessionCookie?.let {
                    mapOf(
                        "cookies" to
                            mapOf("verification_session_client_secrets" to listOf(it))
                    )
                } ?: emptyMap()
            )
        ),
        responseJsonParser = ConsumerSessionJsonParser()
    )

    internal companion object {
        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/lookup`
         */
        internal val consumerSessionLookupUrl: String =
            getApiUrl("consumers/sessions/lookup")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/start_verification`
         */
        internal val startConsumerVerificationUrl: String =
            getApiUrl("consumers/sessions/start_verification")

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/confirm_verification`
         */
        internal val confirmConsumerVerificationUrl: String =
            getApiUrl("consumers/sessions/confirm_verification")

        private fun getApiUrl(path: String): String {
            return "${ApiRequest.API_HOST}/v1/$path"
        }
    }
}
