package com.stripe.android.financialconnections.repository

import androidx.annotation.VisibleForTesting
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
import com.stripe.android.financialconnections.di.PUBLISHABLE_KEY
import com.stripe.android.financialconnections.model.FinancialConnectionsAccountList
import com.stripe.android.financialconnections.model.FinancialConnectionsSession
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.GetFinancialConnectionsAcccountsParams
import com.stripe.android.financialconnections.model.InstitutionResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Named

internal class FinancialConnectionsApiRepository @Inject constructor(
    @Named(PUBLISHABLE_KEY) publishableKey: String,
    private val stripeNetworkClient: StripeNetworkClient,
    private val apiRequestFactory: ApiRequest.Factory
) : FinancialConnectionsRepository {

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

    override suspend fun getFinancialConnectionsAccounts(
        getFinancialConnectionsAcccountsParams: GetFinancialConnectionsAcccountsParams
    ): FinancialConnectionsAccountList {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = listAccountsUrl,
            options = options,
            params = getFinancialConnectionsAcccountsParams.toParamMap()
        )
        return executeRequest(
            financialConnectionsRequest,
            FinancialConnectionsAccountList.serializer()
        )
    }

    override suspend fun getFinancialConnectionsSession(
        clientSecret: String
    ): FinancialConnectionsSession {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = sessionReceiptUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret
            ),
        )
        return executeRequest(financialConnectionsRequest, FinancialConnectionsSession.serializer())
    }

    override suspend fun generateFinancialConnectionsSessionManifest(
        clientSecret: String,
        applicationId: String
    ): FinancialConnectionsSessionManifest {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = generateHostedUrl,
            options = options,
            params = mapOf(
                PARAMS_FULLSCREEN to true,
                PARAMS_HIDE_CLOSE_BUTTON to true,
                PARAMS_CLIENT_SECRET to clientSecret,
                PARAMS_APPLICATION_ID to applicationId
            ),
        )
        return executeRequest(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        )
    }

    override suspend fun getFinancialConnectionsSessionManifest(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val financialConnectionsRequest = apiRequestFactory.createGet(
            url = getManifestUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
            ),
        )
        return executeRequest(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        )
    }

    override suspend fun markConsentAcquired(
        clientSecret: String
    ): FinancialConnectionsSessionManifest {
        val financialConnectionsRequest = apiRequestFactory.createPost(
            url = consentAcquiredUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
            ),
        )
        return executeRequest(
            financialConnectionsRequest,
            FinancialConnectionsSessionManifest.serializer()
        )
    }

    override suspend fun featuredInstitutions(clientSecret: String): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = featuredInstitutionsUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
            ),
        )
        return executeRequest(
            request,
            InstitutionResponse.serializer()
        )
    }

    override suspend fun searchInstitutions(
        clientSecret: String,
        query: String,
    ): InstitutionResponse {
        val request = apiRequestFactory.createGet(
            url = institutionsUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                "query" to query,
                "limit" to SEARCH_INSTITUTIONS_LIMIT
            ),
        )
        return executeRequest(
            request,
            InstitutionResponse.serializer()
        )
    }

    override suspend fun postAuthorizationSession(
        clientSecret: String,
        institutionId: String,
    ): FinancialConnectionsAuthorizationSession {
        val request = apiRequestFactory.createPost(
            url = authorizationSessionUrl,
            options = options,
            params = mapOf(
                PARAMS_CLIENT_SECRET to clientSecret,
                "use_mobile_handoff" to false,
                "institution" to institutionId
            ),
        )
        return executeRequest(
            request,
            FinancialConnectionsAuthorizationSession.serializer()
        )
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

        private const val SEARCH_INSTITUTIONS_LIMIT = 8

        internal const val PARAMS_CLIENT_SECRET = "client_secret"
        internal const val PARAMS_APPLICATION_ID = "application_id"
        internal const val PARAMS_FULLSCREEN = "fullscreen"
        internal const val PARAMS_HIDE_CLOSE_BUTTON = "hide_close_button"

        internal const val listAccountsUrl: String =
            "$API_HOST/v1/link_account_sessions/list_accounts"

        internal const val consentAcquiredUrl: String =
            "$API_HOST/v1/link_account_sessions/consent_acquired"

        internal const val sessionReceiptUrl: String =
            "$API_HOST/v1/link_account_sessions/session_receipt"

        internal const val generateHostedUrl: String =
            "$API_HOST/v1/link_account_sessions/generate_hosted_url"

        internal const val getManifestUrl: String =
            "$API_HOST/v1/link_account_sessions/manifest"

        internal const val institutionsUrl: String =
            "$API_HOST/v1/connections/institutions"

        internal const val authorizationSessionUrl: String =
            "$API_HOST/v1/connections/auth_sessions"

        internal const val featuredInstitutionsUrl: String =
            "$API_HOST/v1/connections/featured_institutions"
    }
}
