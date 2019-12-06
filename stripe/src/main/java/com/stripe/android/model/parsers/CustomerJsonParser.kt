package com.stripe.android.model.parsers

import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerSource
import com.stripe.android.model.StripeJsonUtils
import org.json.JSONArray
import org.json.JSONObject

internal class CustomerJsonParser : ModelJsonParser<Customer> {
    private val customerSourceJsonParser = CustomerSourceJsonParser()

    override fun parse(json: JSONObject): Customer? {
        val objectType = StripeJsonUtils.optString(json, FIELD_OBJECT)
        if (VALUE_CUSTOMER != objectType) {
            return null
        }
        val id = StripeJsonUtils.optString(json, FIELD_ID)
        val defaultSource = StripeJsonUtils.optString(json, FIELD_DEFAULT_SOURCE)
        val shippingInformation = json.optJSONObject(FIELD_SHIPPING)?.let {
            ShippingInformationJsonParser().parse(it)
        }
        val sourcesJson = json.optJSONObject(FIELD_SOURCES)

        val hasMore: Boolean
        val totalCount: Int?
        val url: String?
        val sources: List<CustomerSource>?
        if (sourcesJson != null && VALUE_LIST == StripeJsonUtils.optString(sourcesJson, FIELD_OBJECT)) {
            hasMore = StripeJsonUtils.optBoolean(sourcesJson, FIELD_HAS_MORE)
            totalCount = StripeJsonUtils.optInteger(sourcesJson, FIELD_TOTAL_COUNT)
            url = StripeJsonUtils.optString(sourcesJson, FIELD_URL)

            val dataArray = sourcesJson.optJSONArray(FIELD_DATA) ?: JSONArray()
            sources =
                (0 until dataArray.length())
                    .map { idx -> dataArray.getJSONObject(idx) }
                    .mapNotNull { customerSourceJsonParser.parse(it) }
                    .filterNot { source -> VALUE_APPLE_PAY == source.tokenizationMethod }
        } else {
            hasMore = false
            totalCount = null
            url = null
            sources = emptyList()
        }

        return Customer(id, defaultSource, shippingInformation, sources, hasMore, totalCount, url)
    }

    private companion object {
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
    }
}
