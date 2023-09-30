package com.stripe.android.financialconnections.repository

import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.exception.AuthenticationException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.networking.DEFAULT_RETRY_CODES
import com.stripe.android.core.networking.HEADER_USER_AGENT
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.version.StripeSdkVersion
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.network.FinancialConnectionsRequestExecutor
import kotlinx.serialization.Serializable
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import javax.inject.Inject

/**
 * Repository to centralize reads and writes to the [FinancialConnectionsSessionManifest]
 * of the current flow.
 */
@Suppress("TooManyFunctions")
internal interface FinancialConnectionsCredentialsRepository {

    /**
     * Retrieves the current cached [SynchronizeSessionResponse] instance, or fetches
     * it from backend if no cached version available.
     */
    @Throws(
        AuthenticationException::class,
        InvalidRequestException::class,
        APIConnectionException::class,
        APIException::class
    )
    suspend fun tokenize(
        authSessionId: String,
        username: String,
        password: String,
    ): TokenResponse
}


internal class FinancialConnectionsCredentialsRepositoryImpl @Inject constructor(
    val requestExecutor: FinancialConnectionsRequestExecutor,
) : FinancialConnectionsCredentialsRepository {

    override suspend fun tokenize(
        authSessionId: String,
        username: String,
        password: String,
    ): TokenResponse {
        val request = CredentialsRequest(
            path = "/tokenize?auth_session_token=$authSessionId",
            params = mapOf(
                "credentials" to listOfNotNull(
                    mapOf(
                        "type" to "username",
                        "value" to username,
                    ),
                    mapOf(
                        "type" to "password",
                        "value" to password,
                    )
                )
            )
        )
        return requestExecutor.execute(
            request,
            TokenResponse.serializer()
        )
    }
}

@Serializable
data class TokenResponse(
    val id: String
)

internal class CredentialsRequest(
    private val params: Map<String, Any>,
    val path: String,
) : StripeRequest() {
    private val jsonBody: String
        get() {
            return StripeJsonUtils.mapToJsonObject(params).toString()
        }
    private val postBodyBytes: ByteArray
        get() {
            try {
                return jsonBody.toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                throw InvalidRequestException(
                    message = "Unable to encode parameters to ${Charsets.UTF_8.name()}. " +
                        "Please contact support@stripe.com for assistance.",
                    cause = e
                )
            }
        }

    override val url = BASE_URL + path

    override val method = Method.POST

    override val mimeType = MimeType.Json

    override val retryResponseCodes: Iterable<Int> = DEFAULT_RETRY_CODES

    override val headers: Map<String, String> = mapOf(
        HEADER_USER_AGENT to "Stripe/v1 android/${StripeSdkVersion.VERSION_NAME}",
        "authority" to AUTHORITY,
        "method" to method.code,
        "path" to path,
        "scheme" to SCHEME,
        "Accept" to mimeType.code,
        "Accept-Encoding" to "gzip, deflate, br",
        "Accept-Language" to "en-US,en;q=0.9",
        "Content-Type" to mimeType.code,
        "Content-Length" to "${postBodyBytes.size}",
        "Sec-Fetch-Dest" to "empty",
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "same-site",
    )

    override fun writePostBody(outputStream: OutputStream) {
        postBodyBytes.let {
            outputStream.write(it)
            outputStream.flush()
        }
    }

    companion object {
        private const val SCHEME = "https"
        private const val AUTHORITY = "bankcon-cred.stripe.com"
        private const val BASE_URL = "$SCHEME://$AUTHORITY"
    }

}