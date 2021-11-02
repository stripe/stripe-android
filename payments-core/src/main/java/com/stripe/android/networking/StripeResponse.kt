package com.stripe.android.networking

import com.stripe.android.exception.APIException
import com.stripe.android.networking.RequestHeadersFactory.Companion.HEADER_CONTENT_TYPE
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_MULT_CHOICE

/**
 * Represents a response from the Stripe servers.
 *
 * @param code the response code (i.e. 404)
 * @param body the body of the response
 * @param headers any headers associated with the response
 */
internal data class StripeResponse internal constructor(
    /**
     * the response code
     */
    internal val code: Int,
    /**
     * the response body
     */
    internal val body: String?,
    /**
     * the response headers
     */
    internal val headers: Map<String, List<String>> = emptyMap()
) {
    internal val isOk: Boolean = code == HttpURLConnection.HTTP_OK
    internal val isError: Boolean = code < HttpURLConnection.HTTP_OK || code >= HTTP_MULT_CHOICE
    internal val isRateLimited = code == HTTP_TOO_MANY_REQUESTS

    internal val requestId: RequestId? = RequestId.fromString(
        getHeaderValue(HEADER_REQUEST_ID)?.firstOrNull()
    )

    private val contentType: String? = getHeaderValue(HEADER_CONTENT_TYPE)?.firstOrNull()

    internal val responseJson: JSONObject
        @Throws(APIException::class)
        get() {
            return body?.let {
                try {
                    JSONObject(it)
                } catch (e: JSONException) {
                    throw APIException(
                        message =
                        """
                            Exception while parsing response body.
                              Status code: $code
                              Request-Id: $requestId
                              Content-Type: $contentType
                              Body: "$it"
                        """.trimIndent(),
                        cause = e
                    )
                }
            } ?: JSONObject()
        }

    override fun toString(): String {
        return "$HEADER_REQUEST_ID: $requestId, Status Code: $code"
    }

    internal fun getHeaderValue(key: String): List<String>? {
        return headers.entries
            .firstOrNull {
                it.key.equals(key, ignoreCase = true)
            }?.value
    }

    internal companion object {
        internal const val HEADER_REQUEST_ID = "Request-Id"
    }
}
