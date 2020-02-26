package com.stripe.android

import com.stripe.android.exception.APIException
import java.net.HttpURLConnection
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents a response from the Stripe servers.
 *
 * @param code the response code (i.e. 404)
 * @param body the body of the response
 * @param headers any headers associated with the response
 */
internal data class StripeResponse internal constructor(
    /**
     * @return the response code
     */
    internal val code: Int,
    /**
     * @return the response body
     */
    internal val body: String?,
    /**
     * @return the response headers
     */
    internal val headers: Map<String, List<String>> = emptyMap()
) {
    internal val isOk: Boolean = code == HttpURLConnection.HTTP_OK
    internal val isError: Boolean = code < 200 || code >= 300

    internal val requestId: String? = getHeaderValue(REQUEST_ID_HEADER)?.firstOrNull()
    private val contentType: String? = getHeaderValue(CONTENT_TYPE_HEADER)?.firstOrNull()

    internal val responseJson: JSONObject
        @Throws(APIException::class)
        get() {
            return body?.let {
                try {
                    JSONObject(it)
                } catch (e: JSONException) {
                    throw APIException(
                        message = """
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
        return "Request-Id: $requestId, Status Code: $code"
    }

    internal fun getHeaderValue(key: String): List<String>? {
        return headers.entries
            .firstOrNull {
                it.key.equals(key, ignoreCase = true)
            }?.value
    }

    private companion object {
        private const val REQUEST_ID_HEADER = "Request-Id"
        private const val CONTENT_TYPE_HEADER = "Content-Type"
    }
}
