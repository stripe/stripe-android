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
data class StripeResponse<ResponseBody>(
    /**
     * the response code
     */
    val code: Int,
    /**
     * the response body
     */
    val body: ResponseBody?,

    /**
     * the response headers
     */
    val headers: Map<String, List<String>> = emptyMap(),
    val endpoint: String? = null,
) {
    val isOk: Boolean = code == HttpURLConnection.HTTP_OK
    val isError: Boolean = code < HttpURLConnection.HTTP_OK || code >= HTTP_MULT_CHOICE
    val isRateLimited = code == HTTP_TOO_MANY_REQUESTS

    val requestId: RequestId? = RequestId.fromString(
        getHeaderValue(HEADER_REQUEST_ID)?.firstOrNull()
    )

    override fun toString(): String {
        // E.g. req_123 /elements/sessions (200)
        val components = listOfNotNull(requestId, endpoint).joinToString(" ")
        return "$components ($code)"
    }

    fun getHeaderValue(key: String): List<String>? {
        return headers.entries
            .firstOrNull {
                it.key.equals(key, ignoreCase = true)
            }?.value
    }

    internal companion object {
        internal const val HEADER_REQUEST_ID = "Request-Id"
    }
}
