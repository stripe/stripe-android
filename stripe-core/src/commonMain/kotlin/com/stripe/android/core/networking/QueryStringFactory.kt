package com.stripe.android.core.networking

import androidx.annotation.RestrictTo
import com.stripe.android.core.utils.urlEncode

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
        val compactParams = mutableMapOf<String, Any>()

        params.forEach { (key, value) ->
            when (value) {
                is Map<*, *> -> {
                    compactParams[key] = compactParams(value.asStringMap())
                }
                null -> Unit
                else -> compactParams[key] = value
            }
        }

        return compactParams
    }

    private fun flattenParams(params: Map<String, *>?): List<Parameter> {
        return flattenParamsMap(params)
    }

    /**
     * Determine if a value is a primitive. Primitives in lists can be serialized without indexes.
     */
    private fun isPrimitive(value: Any?) =
        value is String || value is Number || value is Boolean || value is Char

    /**
     * Determine if all elements in a list are primitives.
     */
    private fun isPrimitiveList(l: List<*>) = l.all { isPrimitive(it) }

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
        } else if (isPrimitiveList(params)) {
            // Lists of primitives can be serialized as `listName[]=`
            val newPrefix = "$keyPrefix[]"
            params.flatMap {
                flattenParamsValue(it, newPrefix)
            }
        } else {
            // Lists of objects must include the index like `listName[0]=`
            params.flatMapIndexed { index, value ->
                flattenParamsValue(value, "$keyPrefix[$index]")
            }
        }
    }

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

    private fun flattenParamsValue(
        value: Any?,
        keyPrefix: String
    ): List<Parameter> {
        return when (value) {
            is Map<*, *> -> flattenParamsMap(value.asStringMap(), keyPrefix)
            is List<*> -> flattenParamsList(value, keyPrefix)
            null -> {
                listOf(Parameter(keyPrefix, ""))
            }
            else -> {
                listOf(Parameter(keyPrefix, value.toString()))
            }
        }
    }

    private data class Parameter constructor(
        private val key: String,
        private val value: String
    ) {
        override fun toString(): String {
            val encodedKey = urlEncode(key)
            val encodedValue = urlEncode(value)
            return "$encodedKey=$encodedValue"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.asStringMap(): Map<String, *> {
        return this as Map<String, *>
    }
}
