package com.stripe.android.model

import com.stripe.android.model.StripeJsonUtils.optBoolean
import com.stripe.android.model.StripeJsonUtils.optInteger
import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONException
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
    val url: String?
) : StripeModel() {

    fun getSourceById(sourceId: String): CustomerSource? {
        return sources.firstOrNull { it.id == sourceId }
    }

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_DEFAULT_SOURCE = "default_source"
        private const val FIELD_SHIPPING = "shipping"
        private const val FIELD_SOURCES = "sources"

        private const val FIELD_DATA = "data"
        private const val FIELD_HAS_MORE = "has_more"
        private const val FIELD_TOTAL_COUNT = "total_count"
        private const val FIELD_URL = "url"

        private const val VALUE_LIST = "list"
        private const val VALUE_CUSTOMER = "customer"

        private const val VALUE_APPLE_PAY = "apple_pay"

        @JvmStatic
        fun fromString(jsonString: String?): Customer? {
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
        fun fromJson(jsonObject: JSONObject): Customer? {
            val objectType = optString(jsonObject, FIELD_OBJECT)
            if (VALUE_CUSTOMER != objectType) {
                return null
            }
            val id = optString(jsonObject, FIELD_ID)
            val defaultSource = optString(jsonObject, FIELD_DEFAULT_SOURCE)
            val shippingInformation =
                ShippingInformation.fromJson(jsonObject.optJSONObject(FIELD_SHIPPING))
            val sourcesJson = jsonObject.optJSONObject(FIELD_SOURCES)

            val hasMore: Boolean
            val totalCount: Int?
            val url: String?
            val sources: List<CustomerSource>?
            if (sourcesJson != null && VALUE_LIST == optString(sourcesJson, FIELD_OBJECT)) {
                hasMore = optBoolean(sourcesJson, FIELD_HAS_MORE)
                totalCount = optInteger(sourcesJson, FIELD_TOTAL_COUNT)
                url = optString(sourcesJson, FIELD_URL)

                val dataArray = sourcesJson.optJSONArray(FIELD_DATA) ?: JSONArray()
                sources =
                    (0 until dataArray.length())
                        .map { idx -> dataArray.getJSONObject(idx) }
                        .mapNotNull { json -> CustomerSource.fromJson(json) }
                        .filterNot { source -> VALUE_APPLE_PAY == source.tokenizationMethod }
            } else {
                hasMore = false
                totalCount = null
                url = null
                sources = emptyList()
            }

            return Customer(id, defaultSource, shippingInformation, sources, hasMore, totalCount,
                url)
        }
    }
}
