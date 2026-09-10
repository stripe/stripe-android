package com.stripe.android.identity.networked

import com.stripe.android.core.Logger
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.model.parsers.StripeErrorJsonParser
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.core.utils.urlEncode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

internal class DefaultNetworkedIdentityRepository(
    private val merchantRequestOptions: ApiRequest.Options,
    private val stripeNetworkClient: StripeNetworkClient,
) : NetworkedIdentityRepository {
    private val apiRequestFactory = ApiRequest.Factory()
    private val stripeErrorJsonParser = StripeErrorJsonParser()

    override suspend fun lookup(
        email: String,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentityLookup> = post(
        path = "consumers/sessions/lookup",
        options = merchantRequestOptions,
        params = mapOf("email_address" to email) + cookies(authSessionSecrets),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::lookup,
    )

    override suspend fun signUp(
        request: NetworkedIdentitySignUpRequest,
    ): Result<NetworkedIdentitySignUpResponse> = post(
        path = "consumers/accounts/sign_up",
        options = merchantRequestOptions,
        params = mapOf(
            "email_address" to request.emailAddress,
            "phone_number" to request.phoneNumber,
            "country" to request.country,
            "country_inferring_method" to request.countryInferringMethod.name,
            "locale" to request.locale,
            "consent_action" to "entered_phone_number_email_clicked_save_with_link_identity",
            "default_opt_in_enabled" to request.defaultOptInEnabled,
            "changed_phone_number" to request.changedPhoneNumber,
            "checked_opt_in_box" to request.checkedOptInBox,
            "legal_name" to request.legalName,
            "hcaptcha_response" to request.hcaptchaResponse,
            "hcaptcha_key" to request.hcaptchaKey,
            "session_id" to request.sessionId,
        ) + cookies(request.authSessionSecrets),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::signUp,
    )

    override suspend fun startVerification(
        credentials: NetworkedIdentityCredentials,
        locale: String,
        accountPhoneNumber: String?,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentitySessionResponse> = post(
        path = "consumers/sessions/start_verification",
        options = credentials.requestOptions(),
        params = credentials.params() + cookies(authSessionSecrets) + mapOf(
            "type" to "SMS",
            "locale" to locale,
            "account_phone_number" to accountPhoneNumber,
        ),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::sessionResponse,
    )

    override suspend fun confirmVerification(
        credentials: NetworkedIdentityCredentials,
        code: String,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentitySessionResponse> = post(
        path = "consumers/sessions/confirm_verification",
        options = credentials.requestOptions(),
        params = credentials.params() + cookies(authSessionSecrets) + mapOf("type" to "SMS", "code" to code),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::sessionResponse,
    )

    override suspend fun listDocuments(
        credentials: NetworkedIdentityCredentials,
    ): Result<List<NetworkedIdentityDocument>> = post(
        path = "consumers/identity_documents/list",
        options = credentials.requestOptions(),
        params = credentials.params(),
        retryServerError = true,
        parse = NetworkedIdentityJsonParser::documents,
    )

    override suspend fun createAssociationToken(
        credentials: NetworkedIdentityCredentials,
        documentId: String,
    ): Result<NetworkedIdentityAssociationToken> = post(
        path = "consumers/identity_documents/${urlEncode(documentId)}/association_token",
        options = credentials.requestOptions(),
        params = credentials.params(),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::associationToken,
    )

    override suspend fun logout(
        credentials: NetworkedIdentityCredentials,
        authSessionSecrets: List<String>,
    ): Result<NetworkedIdentitySessionResponse> = post(
        path = "consumers/sessions/log_out",
        options = credentials.requestOptions(),
        params = credentials.params() + cookies(authSessionSecrets),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::sessionResponse,
    )

    // #TODO - Networked Identity: define extension triggers and absent/rotated-secret semantics before orchestration.
    override suspend fun extendSession(
        credentials: NetworkedIdentityCredentials,
    ): Result<NetworkedIdentityExtendSessionResponse> = post(
        path = "consumers/sessions/extend",
        options = credentials.requestOptions(),
        params = credentials.params(),
        retryServerError = false,
        parse = NetworkedIdentityJsonParser::extendSession,
    )

    private fun NetworkedIdentityCredentials.requestOptions() = ApiRequest.Options(
        apiKey = publishableKey,
        stripeAccount = null,
        idempotencyKey = null,
    )

    private fun NetworkedIdentityCredentials.params(): Map<String, Any> = mapOf(
        "credentials" to mapOf("consumer_session_client_secret" to sessionClientSecret),
    )

    private fun cookies(authSessionSecrets: List<String>): Map<String, Any> =
        authSessionSecrets.filter { it.isNotEmpty() }.distinct().takeIf { it.isNotEmpty() }?.let {
            mapOf("cookies" to mapOf("verification_session_client_secrets" to it))
        }.orEmpty()

    private suspend fun <T> post(
        path: String,
        options: ApiRequest.Options,
        params: Map<String, Any?>,
        retryServerError: Boolean,
        parse: (JSONObject) -> T,
    ): Result<T> {
        val request = NetworkedIdentityRequest(
            apiRequestFactory.createPost(
                url = "${ApiRequest.API_HOST}/v1/$path",
                options = options,
                params = params + ("request_surface" to "web_identity_product"),
                shouldCache = false,
            )
        )
        val response = try {
            val firstResponse = stripeNetworkClient.executeRequest(request)
            if (retryServerError && firstResponse.code in SERVER_ERROR_CODES) {
                delay(DOCUMENT_LIST_RETRY_DELAY_MS)
                stripeNetworkClient.executeRequest(request)
            } else {
                firstResponse
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Transport errors may embed the document URL; do not retain the underlying exception.
            return Result.failure(APIConnectionException(message = "Networked Identity request failed."))
        }
        return decodeResponse(response, parse)
    }

    private fun <T> decodeResponse(response: StripeResponse<String>, parse: (JSONObject) -> T): Result<T> = try {
        val json = JSONObject(requireNotNull(response.body))
        if (response.isError) {
            val error = stripeErrorJsonParser.parse(json)
            Result.failure(
                APIException(
                    // Backend messages/params may echo submitted identity information or OTPs.
                    stripeError = StripeError(type = error.type, code = error.code),
                    requestId = response.requestId?.value,
                    statusCode = response.code,
                    message = "Networked Identity request failed.",
                )
            )
        } else {
            Result.success(parse(json))
        }
    } catch (_: Exception) {
        // JSON exception messages can contain complete response bodies, including credentials.
        Result.failure(
            APIException(
                requestId = response.requestId?.value,
                statusCode = response.code,
                message = "Networked Identity response was invalid.",
            )
        )
    }

    internal companion object {
        private const val DOCUMENT_LIST_RETRY_DELAY_MS = 250L
        private val SERVER_ERROR_CODES = 500..599

        fun create(
            merchantRequestOptions: ApiRequest.Options,
            workContext: CoroutineContext,
        ): DefaultNetworkedIdentityRepository = DefaultNetworkedIdentityRepository(
            merchantRequestOptions = merchantRequestOptions,
            stripeNetworkClient = DefaultStripeNetworkClient(
                workContext = workContext,
                maxRetries = 0,
                logger = Logger.noop(),
            ),
        )
    }
}

/** Keep normal Android headers/encoding, but do not inherit retries or print document URLs. */
private class NetworkedIdentityRequest(private val request: ApiRequest) : StripeRequest() {
    override val method: Method = request.method
    override val mimeType: MimeType = request.mimeType
    override val retryResponseCodes: Iterable<Int> = emptyList()
    override val url: String = request.url
    override val headers: Map<String, String> = request.headers
    override var postHeaders: Map<String, String>? = request.postHeaders

    override fun writePostBody(outputStream: OutputStream) = request.writePostBody(outputStream)

    override fun toString(): String = "NetworkedIdentityRequest(POST, [redacted])"
}
