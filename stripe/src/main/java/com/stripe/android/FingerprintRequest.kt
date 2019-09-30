package com.stripe.android

import com.stripe.android.exception.InvalidRequestException
import java.io.UnsupportedEncodingException
import java.util.Objects
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A class representing a fingerprint request.
 */
internal class FingerprintRequest(
    params: Map<String, Any>,
    private val guid: String
) : StripeRequest(Method.POST, URL, params, MIME_TYPE) {

    override fun getUserAgent(): String {
        return DEFAULT_USER_AGENT
    }

    @Throws(UnsupportedEncodingException::class, InvalidRequestException::class)
    override fun getOutputBytes(): ByteArray {
        val jsonData = mapToJsonObject(params)
            ?: throw InvalidRequestException(
                "Unable to create JSON data from " + "parameters. " +
                    "Please contact support@stripe.com for assistance.",
                null, null, 0, null, null, null, null
            )
        return jsonData.toString().toByteArray(charset(CHARSET))
    }

    override fun createHeaders(): Map<String, String> {
        return mapOf("Cookie" to "m=$guid")
    }

    override fun hashCode(): Int {
        return Objects.hash(baseHashCode, guid)
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is FingerprintRequest -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(obj: FingerprintRequest): Boolean {
        return super.typedEquals(obj) && guid == obj.guid
    }

    companion object {
        private const val MIME_TYPE = "application/json"
        private const val URL = "https://m.stripe.com/4"

        /**
         * Converts a string-keyed [Map] into a [JSONObject]. This will cause a
         * [ClassCastException] if any sub-map has keys that are not [Strings][String].
         *
         * @param mapObject the [Map] that you'd like in JSON form
         * @return a [JSONObject] representing the input map, or `null` if the input
         * object is `null`
         */
        private fun mapToJsonObject(mapObject: Map<String, *>?): JSONObject? {
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
    }
}
