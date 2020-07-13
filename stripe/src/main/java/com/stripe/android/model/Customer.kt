package com.stripe.android.model

import com.stripe.android.model.parsers.CustomerJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model for a Stripe Customer object
 */
@Parcelize
data class Customer internal constructor(
    val id: String?,
    val defaultSource: String?,
    val shippingInformation: ShippingInformation?,
    val sources: List<CustomerSource>,
    val hasMore: Boolean,
    val totalCount: Int?,
    val url: String?,
    val description: String?,
    val email: String?,
    val liveMode: Boolean
) : StripeModel {

    fun getSourceById(sourceId: String): CustomerSource? {
        return sources.firstOrNull { it.id == sourceId }
    }

    companion object {
        @JvmStatic
        fun fromString(jsonString: String?): Customer? {
            if (jsonString == null) {
                return null
            }

            return runCatching { JSONObject(jsonString) }
                .getOrNull()?.let {
                    fromJson(it)
                }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject): Customer? {
            return CustomerJsonParser().parse(jsonObject)
        }
    }
}
