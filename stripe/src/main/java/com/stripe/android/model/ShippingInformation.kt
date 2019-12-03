package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.parsers.ShippingInformationJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model representing a shipping address object
 */
@Parcelize
data class ShippingInformation constructor(
    val address: Address? = null,
    val name: String? = null,
    val phone: String? = null
) : StripeModel(), StripeParamsModel, Parcelable {

    override fun toParamMap(): Map<String, Any> {
        return listOf(
            PARAM_NAME to name,
            PARAM_PHONE to phone,
            PARAM_ADDRESS to address?.toParamMap()
        )
            .mapNotNull { (first, second) -> second?.let { Pair(first, it) } }
            .toMap()
    }

    companion object {
        private const val PARAM_ADDRESS = "address"
        private const val PARAM_NAME = "name"
        private const val PARAM_PHONE = "phone"

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): ShippingInformation? {
            return jsonObject?.let {
                ShippingInformationJsonParser().parse(it)
            }
        }
    }
}
