package com.stripe.android.connections.repository

import androidx.annotation.VisibleForTesting
import com.stripe.android.connections.di.PUBLISHABLE_KEY
import com.stripe.android.connections.model.LinkAccountSession
import com.stripe.android.connections.model.LinkAccountSessionManifest
import com.stripe.android.connections.model.LinkedAccountList
import com.stripe.android.connections.model.ListLinkedAccountParams
import com.stripe.android.core.Logger
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.exception.PermissionException
import com.stripe.android.core.exception.RateLimitException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.HTTP_TOO_MANY_REQUESTS
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.networking.responseJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Named

internal class ConnectionsApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) publishableKey: String,
    private val logger: Logger,
    private val stripeNetworkClient: StripeNetworkClient,
    private val apiRequestFactory: ApiRequest.Factory
) : ConnectionsRepository {

    @VisibleForTesting
    internal val json: Json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val options = ApiRequest.Options(
        apiKey = publishableKey
    )

    override suspend fun getLinkedAccounts(
        listLinkedAccountParams: ListLinkedAccountParams
    ): LinkedAccountList {
        val connectionsRequest = apiRequestFactory.createGet(
            url = listAccountsUrl,
            options = options,
            params = listLinkedAccountParams.toParamMap()
        )
        return executeRequest(connectionsRequest, LinkedAccountList.serializer())
    }

    override suspend fun getLinkAccountSession(
        clientSecret: String
    ): LinkAccountSession {
        val connectionsRequest = apiRequestFactory.createGet(
            url = sessionReceiptUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret
            ),
        )
        return executeRequest(connectionsRequest, LinkAccountSession.serializer())
    }

    override suspend fun generateLinkAccountSessionManifest(
        clientSecret: String,
        applicationId: String
    ): LinkAccountSessionManifest {
        val connectionsRequest = apiRequestFactory.createPost(
            url = generateHostedUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                PARAMS_APPLICATION_ID to applicationId
            ),
        )
        return executeRequest(connectionsRequest, LinkAccountSessionManifest.serializer())
    }

    private suspend fun <Response> executeRequest(
        request: StripeRequest,
        responseSerializer: KSerializer<Response>
    ): Response = runCatching {
        stripeNetworkClient.executeRequest(
            request
        )
    }.fold(
        onSuccess = { response ->
            if (response.isError) {
                throw handleApiError(response)
            } else {
                logger.debug(response.body!!)
                json.decodeFromString(
                    responseSerializer,
                    requireNotNull(response.body)
                )
            }
        },
        onFailure = {
            throw APIConnectionException(
                "Failed to execute $request",
                cause = it
            )
        }
    )

    @Throws(
        InvalidRequestException::class,
        AuthenticationException::class,
        APIException::class
    )
    private fun handleApiError(response: StripeResponse<String>): Exception {
        val requestId = response.requestId?.value
        val responseCode = response.code
        val stripeError = StripeErrorJsonParser().parse(response.responseJson())
        throw when (responseCode) {
            HttpURLConnection.HTTP_BAD_REQUEST,
            HttpURLConnection.HTTP_NOT_FOUND -> InvalidRequestException(
                stripeError,
                requestId,
                responseCode
            )
            HttpURLConnection.HTTP_UNAUTHORIZED -> AuthenticationException(stripeError, requestId)
            HttpURLConnection.HTTP_FORBIDDEN -> PermissionException(stripeError, requestId)
            HTTP_TOO_MANY_REQUESTS -> RateLimitException(stripeError, requestId)
            else -> APIException(stripeError, requestId, responseCode)
        }
    }

    internal companion object {
        private const val API_HOST = "https://api.stripe.com"

        internal const val PARAMS_CLIENT_SECRET = "client_secret"
        internal const val PARAMS_APPLICATION_ID = "application_id"

        /**
         * @return `https://api.stripe.com/v1/link_account_sessions/list_accounts`
         */
        internal val listAccountsUrl: String
            @JvmSynthetic
            get() = getApiUrl("list_accounts")

        /**
         * @return `https://api.stripe.com/v1/link_account_sessions/generate_hosted_url`
         */
        internal val generateHostedUrl: String
            @JvmSynthetic
            get() = getApiUrl("generate_hosted_url")

        internal val sessionReceiptUrl: String
            @JvmSynthetic
            get() = getApiUrl("session_receipt")

        private fun getApiUrl(path: String): String {
            return "$API_HOST/v1/link_account_sessions/$path"
        }
    }
}
