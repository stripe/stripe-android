package com.stripe.example.paymentsheet

import org.json.JSONObject

internal data class EphemeralKey(
    val key: String,
    val customer: String
) {
    companion object {
        fun fromJson(json: JSONObject): EphemeralKey {
            val secret = json.getString("secret")
            val associatedObjectArray = json.getJSONArray("associated_objects")
            val typeObject = associatedObjectArray.getJSONObject(0)
            val customerId = typeObject.getString("id")

            return EphemeralKey(
                key = secret,
                customer = customerId
            )
        }
    }
}
