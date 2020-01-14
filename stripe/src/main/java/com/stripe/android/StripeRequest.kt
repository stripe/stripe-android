package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import java.util.HashMap
import java.util.HashSet

/**
 * A class representing a request to a Stripe-owned service.
 */
internal abstract class StripeRequest {
    abstract val method: Method
    abstract val baseUrl: String
    abstract val params: Map<String, *>?
    abstract val mimeType: MimeType

    private val queryStringFactory = QueryStringFactory()
    internal val compactParams: Map<String, *>?
        get() {
            return params?.let { compactParams(it) }
        }

    /**
     * @return if the HTTP method is [Method.GET], return URL with query string;
     * otherwise, return the URL
     */
    internal val url: String
        @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
        get() {
            return if (Method.GET == method) {
                urlWithQuery
            } else {
                baseUrl
            }
        }

    internal val contentType: String
        get() {
            return "$mimeType; charset=$CHARSET"
        }

    internal val headers: Map<String, String>
        get() {
            return createHeaders()
                .plus(
                    mapOf(
                        HEADER_USER_AGENT to userAgent,
                        HEADER_ACCEPT_CHARSET to CHARSET
                    )
                )
        }

    internal abstract val userAgent: String

    internal abstract val body: String

    internal val bodyBytes: ByteArray
        get() {
            try {
                return body.toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                throw InvalidRequestException(
                    message = "Unable to encode parameters to ${Charsets.UTF_8.name()}. " +
                        "Please contact support@stripe.com for assistance.",
                    e = e
                )
            }
        }

    internal abstract fun createHeaders(): Map<String, String>

    internal val query: String
        get() {
            return queryStringFactory.create(compactParams)
        }

    private val urlWithQuery: String
        get() {
            return listOfNotNull(
                baseUrl,
                query.takeIf { it.isNotEmpty() }
            ).joinToString(
                // In some cases, URL can already contain a question mark
                // (eg, upcoming invoice lines)
                separator = if (baseUrl.contains("?")) {
                    "&"
                } else {
                    "?"
                }
            )
        }

    internal enum class Method(val code: String) {
        GET("GET"),
        POST("POST"),
        DELETE("DELETE")
    }

    internal enum class MimeType(val code: String) {
        Form("application/x-www-form-urlencoded"),
        Json("application/json");

        override fun toString(): String = code
    }

    internal companion object {
        const val DEFAULT_USER_AGENT = "Stripe/v1 ${Stripe.VERSION}"

        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ACCEPT_CHARSET = "Accept-Charset"
        private val CHARSET = Charsets.UTF_8.name()

        /**
         * Copy the {@param params} map and recursively remove null and empty values. The Stripe API
         * requires that parameters with null values are removed from requests.
         *
         * @param params a [Map] from which to remove the keys that have `null` values
         */
        private fun compactParams(params: Map<String, *>): Map<String, Any> {
            val compactParams = HashMap<String, Any>(params)

            // Remove all null values; they cause validation errors
            for (key in HashSet(compactParams.keys)) {
                when (val value = compactParams[key]) {
                    is CharSequence -> {
                        if (value.isEmpty()) {
                            compactParams.remove(key)
                        }
                    }
                    is Map<*, *> -> {
                        compactParams[key] = compactParams(value as Map<String, *>)
                    }
                    null -> {
                        compactParams.remove(key)
                    }
                }
            }

            return compactParams
        }
    }
}
