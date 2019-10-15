package com.stripe.android.model

import androidx.annotation.Size
import org.json.JSONArray
import org.json.JSONObject

/**
 * A set of JSON parsing utility functions.
 */
internal object StripeJsonUtils {
    private const val EMPTY = ""
    private const val NULL = "null"

    /**
     * Calls through to [JSONObject.optInt] only in the case that the
     * key exists. This returns `null` if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmStatic
    fun optBoolean(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Boolean? {
        return if (!jsonObject.has(fieldName)) {
            null
        } else jsonObject.optBoolean(fieldName)
    }

    /**
     * Calls through to [JSONObject.optInt] only in the case that the
     * key exists. This returns `null` if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmStatic
    fun optInteger(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Int? {
        return if (!jsonObject.has(fieldName)) {
            null
        } else jsonObject.optInt(fieldName)
    }

    /**
     * Calls through to [JSONObject.optLong] only in the case that the
     * key exists. This returns `null` if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmStatic
    fun optLong(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Long? {
        return if (!jsonObject.has(fieldName)) {
            null
        } else jsonObject.optLong(fieldName)
    }

    /**
     * Calls through to [JSONObject.optString] while safely
     * converting the raw string "null" and the empty string to `null`. Will not throw
     * an exception if the field isn't found.
     *
     * @param jsonObject the input object
     * @param fieldName the optional field name
     * @return the value stored in the field, or `null` if the field isn't present
     */
    @JvmStatic
    fun optString(
        jsonObject: JSONObject?,
        @Size(min = 1) fieldName: String
    ): String? {
        return nullIfNullOrEmpty(jsonObject?.optString(fieldName))
    }

    /**
     * Calls through to [JSONObject.optString] while safely converting
     * the raw string "null" and the empty string to `null`, along with any value that isn't
     * a two-character string.
     * @param jsonObject the object from which to retrieve the country code
     * @param fieldName the name of the field in which the country code is stored
     * @return a two-letter country code if one is found, or `null`
     */
    @JvmStatic
    @Size(2)
    fun optCountryCode(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): String? {
        val value = nullIfNullOrEmpty(jsonObject.optString(fieldName))
        return value?.takeIf { it.length == 2 }
    }

    /**
     * Calls through to [JSONObject.optString] while safely converting
     * the raw string "null" and the empty string to `null`, along with any value that isn't
     * a three-character string.
     * @param jsonObject the object from which to retrieve the currency code
     * @param fieldName the name of the field in which the currency code is stored
     * @return a three-letter currency code if one is found, or `null`
     */
    @JvmStatic
    @Size(3)
    fun optCurrency(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): String? {
        val value = nullIfNullOrEmpty(jsonObject.optString(fieldName))
        return value?.takeIf { it.length == 3 }
    }

    /**
     * Calls through to [JSONObject.optJSONObject] and then
     * uses [.jsonObjectToMap] on the result.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmStatic
    fun optMap(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Map<String, Any?>? {
        val foundObject = jsonObject.optJSONObject(fieldName) ?: return null
        return jsonObjectToMap(foundObject)
    }

    /**
     * Calls through to [JSONObject.optJSONObject] and then
     * uses [.jsonObjectToStringMap] on the result.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmStatic
    fun optHash(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Map<String, String>? {
        val foundObject = jsonObject.optJSONObject(fieldName) ?: return null
        return jsonObjectToStringMap(foundObject)
    }

    /**
     * Convert a [JSONObject] to a [Map].
     *
     * @param jsonObject a [JSONObject] to be converted
     * @return a [Map] representing the input, or `null` if the input is `null`
     */
    @JvmStatic
    fun jsonObjectToMap(jsonObject: JSONObject?): Map<String, Any?>? {
        if (jsonObject == null) {
            return null
        }

        val keys = jsonObject.names() ?: JSONArray()
        return (0 until keys.length())
            .map { idx -> keys.getString(idx) }
            .mapNotNull { key ->
                val value = jsonObject.opt(key)
                if (value != null && value != NULL) {
                    mapOf(
                        key to
                            when (value) {
                                is JSONObject -> jsonObjectToMap(value)
                                is JSONArray -> jsonArrayToList(value)
                                else -> value
                            }
                    )
                } else {
                    null
                }
            }
            .fold(emptyMap()) { acc, map -> acc.plus(map) }
    }

    /**
     * Convert a [JSONObject] to a flat, string-keyed and string-valued map. All values
     * are recorded as strings.
     *
     * @param jsonObject the input [JSONObject] to be converted
     * @return a [Map] representing the input, or `null` if the input is `null`
     */
    @JvmStatic
    fun jsonObjectToStringMap(jsonObject: JSONObject?): Map<String, String>? {
        if (jsonObject == null) {
            return null
        }

        val keys = jsonObject.names() ?: JSONArray()
        return (0 until keys.length())
            .map { keys.getString(it) }
            .mapNotNull { key ->
                val value = jsonObject.opt(key)
                if (value != null && value != NULL) {
                    mapOf(key to value.toString())
                } else {
                    null
                }
            }
            .fold(emptyMap()) { acc, map -> acc.plus(map) }
    }

    /**
     * Converts a [JSONArray] to a [List].
     *
     * @param jsonArray a [JSONArray] to be converted
     * @return a [List] representing the input, or `null` if said input is `null`
     */
    @JvmStatic
    internal fun jsonArrayToList(jsonArray: JSONArray?): List<Any>? {
        if (jsonArray == null) {
            return null
        }

        return (0 until jsonArray.length())
            .map { idx -> jsonArray.get(idx) }
            .mapNotNull { ob ->
                if (ob is JSONArray) {
                    jsonArrayToList(ob)
                } else if (ob is JSONObject) {
                    jsonObjectToMap(ob)
                } else {
                    if (ob == NULL) {
                        null
                    } else {
                        ob
                    }
                }
            }
    }

    @JvmStatic
    internal fun nullIfNullOrEmpty(possibleNull: String?): String? {
        return possibleNull.takeUnless { NULL == it || EMPTY == it }
    }
}
