package com.stripe.android.model

import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a
 * [receiver](https://stripe.com/docs/api/sources/object#source_object-receiver) object
 * in the Sources API. Present if the [Source] is a receiver.
 */
data class SourceReceiver private constructor(
    val address: String?,
    val amountCharged: Long,
    val amountReceived: Long,
    val amountReturned: Long
) : StripeModel() {

    companion object {
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_AMOUNT_CHARGED = "amount_charged"
        private const val FIELD_AMOUNT_RECEIVED = "amount_received"
        private const val FIELD_AMOUNT_RETURNED = "amount_returned"

        @JvmStatic
        fun fromString(jsonString: String?): SourceReceiver? {
            if (jsonString == null) {
                return null
            }

            return try {
                fromJson(JSONObject(jsonString))
            } catch (ignored: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceReceiver? {
            return if (jsonObject == null) {
                null
            } else SourceReceiver(
                StripeJsonUtils.optString(jsonObject, FIELD_ADDRESS),
                jsonObject.optLong(FIELD_AMOUNT_CHARGED),
                jsonObject.optLong(FIELD_AMOUNT_RECEIVED),
                jsonObject.optLong(FIELD_AMOUNT_RETURNED)
            )
        }
    }
}
