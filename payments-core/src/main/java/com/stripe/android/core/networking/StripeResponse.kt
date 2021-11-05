package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import java.io.File
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_MULT_CHOICE

/**
 * Represents a response from the Stripe servers.
 * Upon receiving the HTTP response, its body is parsed into [ResponseBody], such as a
 * [String] or a [File].
 *
 * @param code the response code (i.e. 404)
 * @param body the body of the response
 * @param headers any headers associated with the response
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class StripeResponse<ResponseBody> internal constructor(
    /**
     * the response code
     */
    internal val code: Int,
    /**
     * the response body
     */
    internal val body: ResponseBody?,
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
