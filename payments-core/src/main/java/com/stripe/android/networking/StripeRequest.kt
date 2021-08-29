package com.stripe.android.networking

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.stripe.android.exception.InvalidRequestException
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.util.HashMap
import java.util.HashSet

/**
 * A class representing a request to a Stripe-owned service.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
abstract class StripeRequest {
    internal abstract val method: Method
    abstract val baseUrl: String
    abstract val params: Map<String, *>?
    internal abstract val mimeType: MimeType
    internal abstract val headersFactory: RequestHeadersFactory

    private val queryStringFactory = QueryStringFactory()
    internal val compactParams: Map<String, *>?
        get() {
            return params?.let { compactParams(it) }
        }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    fun getCompactParams() = compactParams

    /**
     * If the HTTP method is [Method.GET], this is the URL with query string;
     * otherwise, just the URL.
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

    internal open val contentType: String
        get() {
            return "$mimeType; charset=$CHARSET"
        }

    internal val headers: Map<String, String>
        get() {
            return headersFactory.create()
        }

    /**
     * Override for complex request body logic (i.e. multipart form requests)
     */
    internal open fun writeBody(outputStream: OutputStream) {
        bodyBytes?.let {
            outputStream.write(it)
            outputStream.flush()
        }
    }

    /**
     * Override for standard requests (i.e. not multipart form requests)
     */
    protected open val body: String? = null

    private val bodyBytes: ByteArray?
        get() {
            try {
                return body?.toByteArray(Charsets.UTF_8)
            } catch (e: UnsupportedEncodingException) {
                throw InvalidRequestException(
                    message = "Unable to encode parameters to ${Charsets.UTF_8.name()}. " +
                        "Please contact support@stripe.com for assistance.",
                    cause = e
                )
            }
        }

    protected val query: String
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
        MultipartForm("multipart/form-data"),
        Json("application/json");

        override fun toString(): String = code
    }

    internal companion object {
        internal val DEFAULT_SYSTEM_PROPERTY_SUPPLIER = { name: String ->
            System.getProperty(name).orEmpty()
        }

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
