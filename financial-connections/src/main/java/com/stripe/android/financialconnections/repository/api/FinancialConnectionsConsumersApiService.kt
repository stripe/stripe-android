package com.stripe.android.financialconnections.repository.api

import androidx.annotation.RestrictTo
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.CustomEmailType
import com.stripe.android.model.VerificationType
import java.util.Locale

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
internal interface FinancialConnectionsConsumersApiService {

    suspend fun lookupConsumerSession(
        email: String?,
        authSessionCookie: String?,
        requestSurface: String,
    ): ConsumerSessionLookup

    suspend fun startConsumerVerification(
        consumerSessionClientSecret: String,
        locale: Locale,
        authSessionCookie: String?,
        requestSurface: String,
        type: VerificationType,
        customEmailType: CustomEmailType?,
        connectionsMerchantName: String?,
    ): ConsumerSession

    suspend fun confirmConsumerVerification(
        consumerSessionClientSecret: String,
        verificationCode: String,
        authSessionCookie: String?,
        requestSurface: String,
        type: VerificationType,
    ): ConsumerSession

    companion object {
        operator fun invoke(
            requestExecutor: FinancialConnectionsRequestExecutor,
            apiOptions: ApiRequest.Options,
            apiRequestFactory: ApiRequest.Factory,
        ): FinancialConnectionsConsumersApiService =
            FinancialConnectionsConsumersApiServiceImpl(
                requestExecutor,
                apiOptions,
                apiRequestFactory,
            )
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
private class FinancialConnectionsConsumersApiServiceImpl(
    private val requestExecutor: FinancialConnectionsRequestExecutor,
    private val apiOptions: ApiRequest.Options,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsConsumersApiService {

    /**
     * Retrieves the ConsumerSession if the given email is associated with a Link account.
     */
    override suspend fun lookupConsumerSession(
        email: String?,
        authSessionCookie: String?,
        requestSurface: String,
    ): ConsumerSessionLookup {
        val request = apiRequestFactory.createPost(
            consumerSessionLookupUrl,
            apiOptions,
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
        )
        return requestExecutor.execute(
            request,
            ConsumerSessionLookup.serializer()
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
    ): ConsumerSession {
        val request = apiRequestFactory.createPost(
            startConsumerVerificationUrl,
            apiOptions,
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
        )
        return requestExecutor.execute(
            request,
            ConsumerSession.serializer()
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
    ): ConsumerSession {
        val request = apiRequestFactory.createPost(
            confirmConsumerVerificationUrl,
            apiOptions,
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
        )

        return requestExecutor.execute(
            request,
            ConsumerSession.serializer()
        )
    }

    private companion object {
        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/lookup`
         */
        val consumerSessionLookupUrl: String
            @JvmSynthetic
            get() = "${ApiRequest.API_HOST}/v1/consumers/sessions/lookup"

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/start_verification`
         */
        val startConsumerVerificationUrl: String
            @JvmSynthetic
            get() = "${ApiRequest.API_HOST}/v1/consumers/sessions/start_verification"

        /**
         * @return `https://api.stripe.com/v1/consumers/sessions/confirm_verification`
         */
        val confirmConsumerVerificationUrl: String
            @JvmSynthetic
            get() = "${ApiRequest.API_HOST}/v1/consumers/sessions/confirm_verification"
    }
}
