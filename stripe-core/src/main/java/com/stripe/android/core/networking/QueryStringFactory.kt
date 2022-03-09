package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.HashMap
import java.util.HashSet

/**
 * Factory for HTTP request query strings, converts a [Map] of <param, value> into a query string
 * like "?p1=v1&p2=v2"
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object QueryStringFactory {

    /**
     * Create a query string from a [Map]
     */
    fun create(params: Map<String, *>?): String {
        return flattenParams(params).joinToString("&") {
            it.toString()
        }
    }

    /**
     * Create a query string from a [Map] with possible empty values, remove the empty values first
     */
    fun createFromParamsWithEmptyValues(params: Map<String, *>?): String {
        return params?.let(QueryStringFactory::compactParams)?.let(QueryStringFactory::create) ?: ""
    }

    /**
     * Copy the {@param params} map and recursively remove null and empty values. The Stripe API
     * requires that parameters with null values are removed from requests.
     *
     * @param params a [Map] from which to remove the keys that have `null` values
     */
    fun compactParams(params: Map<String, *>): Map<String, Any> {
        val compactParams = HashMap<String, Any>(params)

        // Remove all null values; they cause validation errors
        for (key in HashSet(compactParams.keys)) {
            when (val value = compactParams[key]) {
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

    @Throws(InvalidRequestException::class)
    private fun flattenParams(params: Map<String, *>?): List<Parameter> {
        return flattenParamsMap(params)
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
        keyPrefix: String? = null
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
            null -> {
                listOf(Parameter(keyPrefix, ""))
            }
            else -> {
                listOf(Parameter(keyPrefix, value.toString()))
            }
        }
    }

    private data class Parameter internal constructor(
        private val key: String,
        private val value: String
    ) {
        override fun toString(): String {
            val encodedKey = urlEncode(key)
            val encodedValue = urlEncode(value)
            return "$encodedKey=$encodedValue"
        }

        @Throws(UnsupportedEncodingException::class)
        private fun urlEncode(str: String): String {
            // Preserve original behavior that passing null for an object id will lead
            // to us actually making a request to /v1/foo/null
            return URLEncoder.encode(str, Charsets.UTF_8.name())
        }
    }
}
