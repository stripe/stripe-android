package com.stripe.android.core.model

import androidx.annotation.RestrictTo
import androidx.annotation.Size
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A set of JSON parsing utility functions.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object StripeJsonUtils {
    private const val NULL = "null"

    /**
     * Calls through to [JSONObject.optBoolean] only in the case that the
     * key exists. This returns `null` if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `false` if the key is not present
     */
    @JvmSynthetic
    fun optBoolean(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Boolean {
        return jsonObject.has(fieldName) && jsonObject.optBoolean(fieldName, false)
    }

    /**
     * Calls through to [JSONObject.optInt] only in the case that the
     * key exists. This returns `null` if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmSynthetic
    fun optInteger(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Int? {
        return if (!jsonObject.has(fieldName)) {
            null
        } else {
            jsonObject.optInt(fieldName)
        }
    }

    /**
     * Calls through to [JSONObject.optLong] only in the case that the
     * key exists. This returns `null` if the key is not in the object.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmSynthetic
    fun optLong(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Long? {
        return if (!jsonObject.has(fieldName)) {
            null
        } else {
            jsonObject.optLong(fieldName)
        }
    }

    @JvmSynthetic
    fun optDouble(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Double? {
        return jsonObject.optDouble(fieldName).takeUnless { it.isNaN() }
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
    @JvmSynthetic
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
     * Calls through to [JSONObject.optJSONObject] and then uses [jsonObjectToMap] on the result.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmSynthetic
    fun optMap(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Map<String, Any?>? {
        return jsonObject.optJSONObject(fieldName)?.let {
            jsonObjectToMap(it)
        }
    }

    /**
     * Calls through to [JSONObject.optJSONObject] and then uses [jsonObjectToStringMap] on the result.
     *
     * @param jsonObject the input object
     * @param fieldName the required field name
     * @return the value stored in the requested field, or `null` if the key is not present
     */
    @JvmSynthetic
    fun optHash(
        jsonObject: JSONObject,
        @Size(min = 1) fieldName: String
    ): Map<String, String>? {
        return jsonObject.optJSONObject(fieldName)?.let {
            jsonObjectToStringMap(it)
        }
    }

    /**
     * Convert a [JSONObject] to a [Map].
     *
     * @param jsonObject a [JSONObject] to be converted
     * @return a [Map] representing the input, or `null` if the input is `null`
     */
    @JvmSynthetic
    fun jsonObjectToMap(jsonObject: JSONObject?): Map<String, Any?>? {
        if (jsonObject == null) {
            return null
        }

        val keys = jsonObject.names() ?: JSONArray()
        return (0 until keys.length())
            .map { idx -> keys.getString(idx) }
            .mapNotNull { key ->
                jsonObject.opt(key)?.let { value ->
                    if (value != NULL) {
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
    @JvmSynthetic
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
    @JvmSynthetic
    fun jsonArrayToList(jsonArray: JSONArray?): List<Any>? {
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

    /**
     * Converts a string-keyed [Map] into a [JSONObject]. This will cause a
     * [ClassCastException] if any sub-map has keys that are not [Strings][String].
     *
     * @param mapObject the [Map] that you'd like in JSON form
     * @return a [JSONObject] representing the input map, or `null` if the input
     * object is `null`
     */
    @Suppress("NestedBlockDepth")
    fun mapToJsonObject(mapObject: Map<String, *>?): JSONObject? {
        if (mapObject == null) {
            return null
        }
        val jsonObject = JSONObject()
        for (key in mapObject.keys) {
            val value = mapObject[key] ?: continue

            try {
                if (value is Map<*, *>) {
                    try {
                        val mapValue = value as Map<String, Any>
                        jsonObject.put(key, mapToJsonObject(mapValue))
                    } catch (classCastException: ClassCastException) {
                        // don't include the item in the JSONObject if the keys are not Strings
                    }
                } else if (value is List<*>) {
                    jsonObject.put(key, listToJsonArray(value as List<Any>))
                } else if (value is Number || value is Boolean) {
                    jsonObject.put(key, value)
                } else {
                    jsonObject.put(key, value.toString())
                }
            } catch (jsonException: JSONException) {
                // Simply skip this value
            }
        }
        return jsonObject
    }

    /**
     * Converts a [List] into a [JSONArray]. A [ClassCastException] will be
     * thrown if any object in the list (or any sub-list or sub-map) is a [Map] whose keys
     * are not [Strings][String].
     *
     * @param values a [List] of values to be put in a [JSONArray]
     * @return a [JSONArray], or `null` if the input was `null`
     */
    private fun listToJsonArray(values: List<*>?): JSONArray? {
        if (values == null) {
            return null
        }

        val jsonArray = JSONArray()
        values.forEach { objVal ->
            val jsonVal =
                if (objVal is Map<*, *>) {
                    // We are ignoring type erasure here and crashing on bad input.
                    // Now that this method is not public, we have more control on what is
                    // passed to it.

                    mapToJsonObject(objVal as Map<String, Any>)
                } else if (objVal is List<*>) {
                    listToJsonArray(objVal)
                } else if (objVal is Number || objVal is Boolean) {
                    objVal
                } else {
                    objVal.toString()
                }
            jsonArray.put(jsonVal)
        }
        return jsonArray
    }

    @JvmSynthetic
    fun nullIfNullOrEmpty(possibleNull: String?): String? {
        return possibleNull?.let { s ->
            s.takeUnless { NULL == it || it.isEmpty() }
        }
    }
}
