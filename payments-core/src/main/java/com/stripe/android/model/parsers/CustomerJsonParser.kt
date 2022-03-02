package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.StripeJsonUtils.optString
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerPaymentSource
import com.stripe.android.model.TokenizationMethod
import org.json.JSONArray
import org.json.JSONObject

internal class CustomerJsonParser : ModelJsonParser<Customer> {
    private val customerSourceJsonParser = CustomerPaymentSourceJsonParser()

    override fun parse(json: JSONObject): Customer? {
        val objectType = optString(json, FIELD_OBJECT)
        if (VALUE_CUSTOMER != objectType) {
            return null
        }
        val id = optString(json, FIELD_ID)
        val defaultSource = optString(json, FIELD_DEFAULT_SOURCE)
        val shippingInformation = json.optJSONObject(FIELD_SHIPPING)?.let {
            ShippingInformationJsonParser().parse(it)
        }
        val sourcesJson = json.optJSONObject(FIELD_SOURCES)

        val hasMore: Boolean
        val totalCount: Int?
        val url: String?
        val sources: List<CustomerPaymentSource>
        if (sourcesJson != null && VALUE_LIST == optString(sourcesJson, FIELD_OBJECT)) {
            hasMore = StripeJsonUtils.optBoolean(sourcesJson, FIELD_HAS_MORE)
            totalCount = StripeJsonUtils.optInteger(sourcesJson, FIELD_TOTAL_COUNT)
            url = optString(sourcesJson, FIELD_URL)

            val dataArray = sourcesJson.optJSONArray(FIELD_DATA) ?: JSONArray()
            sources =
                (0 until dataArray.length())
                    .map { idx -> dataArray.getJSONObject(idx) }
                    .mapNotNull { customerSourceJsonParser.parse(it) }
                    .filterNot { it.tokenizationMethod == TokenizationMethod.ApplePay }
        } else {
            hasMore = false
            totalCount = null
            url = null
            sources = emptyList()
        }

        return Customer(
            id = id,
            defaultSource = defaultSource,
            shippingInformation = shippingInformation,
            sources = sources,
            hasMore = hasMore,
            totalCount = totalCount,
            url = url,
            description = optString(json, FIELD_DESCRIPTION),
            email = optString(json, FIELD_EMAIL),
            liveMode = json.optBoolean(FIELD_LIVEMODE, false)
        )
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_DESCRIPTION = "description"
        private const val FIELD_DEFAULT_SOURCE = "default_source"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_SHIPPING = "shipping"
        private const val FIELD_SOURCES = "sources"

        private const val FIELD_DATA = "data"
        private const val FIELD_HAS_MORE = "has_more"
        private const val FIELD_TOTAL_COUNT = "total_count"
        private const val FIELD_URL = "url"

        private const val VALUE_LIST = "list"
        private const val VALUE_CUSTOMER = "customer"
    }
}
