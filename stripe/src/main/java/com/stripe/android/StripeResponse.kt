package com.stripe.android

import java.net.HttpURLConnection

/**
 * Represents a response from the Stripe servers.
 *
 * @param responseCode the response code (i.e. 404)
 * @param responseBody the body of the response
 * @param responseHeaders any headers associated with the response
 */
internal data class StripeResponse(
    /**
     * @return the [response code][.mResponseCode].
     */
    val responseCode: Int,
    /**
     * @return the [response body][.mResponseBody].
     */
    val responseBody: String?,
    /**
     * @return the [response headers][.mResponseHeaders].
     */
    val responseHeaders: Map<String, List<String>>? = null
) {
    val isOk: Boolean
        get() = responseCode == HttpURLConnection.HTTP_OK

    val requestId: String?
        get() = responseHeaders?.get("Request-Id")?.firstOrNull()

    fun hasErrorCode(): Boolean {
        return responseCode < 200 || responseCode >= 300
    }

    override fun toString(): String {
        return "Request-Id: $requestId, Status Code: $responseCode"
    }
}
