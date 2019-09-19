package com.stripe.android.model

import androidx.annotation.CallSuper
import java.util.Objects
import org.json.JSONArray
import org.json.JSONObject

abstract class StripeSourceTypeModel internal constructor(builder: BaseBuilder) : StripeModel() {
    private val additionalFields =
        builder.additionalFields?.let { it } ?: emptyMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return if (other is StripeSourceTypeModel) {
            typedEquals(other)
        } else false
    }

    @CallSuper
    internal fun typedEquals(model: StripeSourceTypeModel): Boolean {
        return additionalFields == model.additionalFields
    }

    override fun hashCode(): Int {
        return Objects.hash(additionalFields)
    }

    abstract class BaseBuilder {
        var additionalFields: Map<String, Any>? = null

        fun setAdditionalFields(additionalFields: Map<String, Any>): BaseBuilder {
            this.additionalFields = additionalFields
            return this
        }
    }

    companion object {
        private const val NULL = "null"

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
            val map = (0 until keys.length())
                .map { keys.getString(it) }
                .filterNot { keysToOmit.contains(it) }
                .mapNotNull { key ->
                    val value = jsonObject.opt(key)
                    if (NULL == value || value == null) {
                        return null
                    }
                    key to value
                }
                .toMap()

            return if (map.isEmpty()) {
                return null
            } else {
                map
            }
        }
    }
}
