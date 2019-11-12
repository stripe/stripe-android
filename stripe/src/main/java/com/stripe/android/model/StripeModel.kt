package com.stripe.android.model

import org.json.JSONArray

/**
 * Model for a Stripe API object.
 */
abstract class StripeModel {

    abstract override fun hashCode(): Int

    abstract override fun equals(other: Any?): Boolean

    internal companion object {
        internal fun jsonArrayToList(jsonArray: JSONArray?): List<String> {
            return jsonArray?.let {
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } ?: emptyList()
        }
    }
}
