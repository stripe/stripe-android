package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.HashMap
import java.util.HashSet
import java.util.Objects

/**
 * A class representing a request to a Stripe-owned service.
 */
internal abstract class StripeRequest(
    val method: Method,
    val baseUrl: String,
    params: Map<String, *>?,
    private val mimeType: String
) {
    val params: Map<String, *>? = params?.let { compactParams(it) }

    /**
     * @return if the HTTP method is [Method.GET], return URL with query string;
     * otherwise, return the URL
     */
    internal val url: String
        @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
        get() = if (Method.GET == method) urlWithQuery() else baseUrl

    internal val contentType: String
        get() = "$mimeType; charset=$CHARSET"

    internal val headers: Map<String, String>
        get() {
            return createHeaders()
                .plus(HEADER_USER_AGENT to getUserAgent())
        }

    internal abstract fun getUserAgent(): String

    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    internal abstract fun getOutputBytes(): ByteArray

    internal val baseHashCode: Int
        get() = Objects.hash(method, baseUrl, params)

    internal abstract fun createHeaders(): Map<String, String>

    internal val query: String
        @Throws(InvalidRequestException::class, UnsupportedEncodingException::class)
        get() {
            return flattenParams(params).joinToString("&") {
                urlEncodePair(it.key, it.value)
            }
        }

    @Throws(InvalidRequestException::class, UnsupportedEncodingException::class)
    private fun urlWithQuery(): String {
        val query = this.query
        return if (query.isEmpty()) {
            baseUrl
        } else {
            // In some cases, URL can already contain a question mark
            // (eg, upcoming invoice lines)
            listOf(baseUrl, query).joinToString(
                separator = if (baseUrl.contains("?")) "&" else "?"
            )
        }
    }

    @Throws(InvalidRequestException::class)
    private fun flattenParams(params: Map<String, *>?): List<Parameter> {
        return flattenParamsMap(params, null)
    }

    @Throws(InvalidRequestException::class)
    private fun flattenParamsList(
        params: List<*>,
        keyPrefix: String
    ): List<Parameter> {
        // Because application/x-www-form-urlencoded cannot represent an empty
        // list, convention is to take the list parameter and just set it to an
        // empty string. (e.g. A regular list might look like `a[]=1&b[]=2`.
        // Emptying it would look like `a=`.)
        return if (params.isEmpty()) {
            listOf(Parameter(keyPrefix, ""))
        } else {
            val newPrefix = "$keyPrefix[]"
            params.flatMap {
                flattenParamsValue(it, newPrefix)
            }
        }
    }

    @Throws(InvalidRequestException::class)
    private fun flattenParamsMap(
        params: Map<String, *>?,
        keyPrefix: String?
    ): List<Parameter> {
        return params?.flatMap { (key, value) ->
            val newPrefix = keyPrefix?.let { "$it[$key]" } ?: key
            flattenParamsValue(value, newPrefix)
        }
            ?: emptyList()
    }

    @Throws(InvalidRequestException::class)
    private fun flattenParamsValue(
        value: Any?,
        keyPrefix: String
    ): List<Parameter> {
        return when (value) {
            is Map<*, *> -> flattenParamsMap(value as Map<String, Any>?, keyPrefix)
            is List<*> -> flattenParamsList(value, keyPrefix)
            "" -> throw InvalidRequestException(
                "You cannot set '$keyPrefix' to an empty string. We interpret empty strings as " +
                    "null in requests. You may set '$keyPrefix' to null to delete the property.",
                keyPrefix, null, 0, null, null, null, null)
            null -> {
                listOf(Parameter(keyPrefix, ""))
            }
            else -> {
                listOf(Parameter(keyPrefix, value.toString()))
            }
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun urlEncodePair(k: String, v: String): String {
        val encodedKey = urlEncode(k)
        val encodedValue = urlEncode(v)
        return "$encodedKey=$encodedValue"
    }

    @Throws(UnsupportedEncodingException::class)
    private fun urlEncode(str: String): String {
        // Preserve original behavior that passing null for an object id will lead
        // to us actually making a request to /v1/foo/null
        return URLEncoder.encode(str, CHARSET)
    }

    fun typedEquals(request: StripeRequest): Boolean {
        return method == request.method &&
            baseUrl == request.baseUrl &&
            params == request.params
    }

    internal enum class Method(val code: String) {
        GET("GET"),
        POST("POST"),
        DELETE("DELETE")
    }

    private data class Parameter internal constructor(
        internal val key: String,
        internal val value: String
    )

    internal companion object {
        const val HEADER_USER_AGENT = "User-Agent"
        const val CHARSET = "UTF-8"

        const val DEFAULT_USER_AGENT = "Stripe/v1 ${Stripe.VERSION}"

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
