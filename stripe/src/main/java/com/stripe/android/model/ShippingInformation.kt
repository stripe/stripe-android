package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model representing a shipping address object
 */
@Parcelize
data class ShippingInformation constructor(
    val address: Address?,
    val name: String?,
    val phone: String?
) : StripeModel(), StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        return listOf(
            FIELD_NAME to name,
            FIELD_PHONE to phone,
            FIELD_ADDRESS to address?.toParamMap()
        )
            .mapNotNull { (first, second) -> second?.let { Pair(first, it) } }
            .toMap()
    }

    companion object {
        private const val FIELD_ADDRESS = "address"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): ShippingInformation? {
            return if (jsonObject == null) {
                null
            } else ShippingInformation(
                Address.fromJson(jsonObject.optJSONObject(FIELD_ADDRESS)),
                optString(jsonObject, FIELD_NAME),
                optString(jsonObject, FIELD_PHONE)
            )
        }
    }
}
