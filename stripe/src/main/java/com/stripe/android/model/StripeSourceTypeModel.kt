package com.stripe.android.model

import org.json.JSONArray
import org.json.JSONObject

abstract class StripeSourceTypeModel : StripeModel() {
    abstract class BaseBuilder {
        var additionalFields: Map<String, Any>? = null

        fun setAdditionalFields(additionalFields: Map<String, Any>): BaseBuilder {
            this.additionalFields = additionalFields
            return this
        }
    }

    companion object {
        /**
         * Convert a [JSONObject] to a flat, string-keyed map.
         *
         * @param jsonObject the input [JSONObject] to be converted
         * @param omitKeys a set of keys to be omitted from the map
         * @return a [Map] representing the input, or `null` if the input is `null`
         * or if the output would be an empty map.
         */
        @JvmStatic
        fun jsonObjectToMapWithoutKeys(
            jsonObject: JSONObject?,
            omitKeys: Set<String>?
        ): Map<String, Any>? {
            if (jsonObject == null) {
                return null
            }

            val keysToOmit = omitKeys ?: emptySet()
            val keys = jsonObject.names() ?: JSONArray()
            return (0 until keys.length())
                .map { keys.getString(it) }
                .filterNot { keysToOmit.contains(it) }
                .mapNotNull { key ->
                    if (jsonObject.isNull(key)) {
                        null
                    } else {
                        key to jsonObject.opt(key)
                    }
                }
                .toMap()
                .takeIf { it.isNotEmpty() }
        }
    }
}
