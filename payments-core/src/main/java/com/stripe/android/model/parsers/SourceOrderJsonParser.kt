package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.SourceOrder
import org.json.JSONArray
import org.json.JSONObject

internal class SourceOrderJsonParser : ModelJsonParser<SourceOrder> {
    private val itemJsonParser = ItemJsonParser()

    override fun parse(json: JSONObject): SourceOrder {
        val itemsJson = json.optJSONArray(FIELD_ITEMS) ?: JSONArray()

        val items = (0 until itemsJson.length())
            .map { idx -> itemsJson.optJSONObject(idx) }
            .mapNotNull {
                itemJsonParser.parse(it)
            }
        return SourceOrder(
            amount = StripeJsonUtils.optInteger(json, FIELD_AMOUNT),
            currency = StripeJsonUtils.optString(json, FIELD_CURRENCY),
            email = StripeJsonUtils.optString(json, FIELD_EMAIL),
            items = items,
            shipping = json.optJSONObject(FIELD_SHIPPING)?.let {
                ShippingJsonParser().parse(it)
            }
        )
    }

    internal class ItemJsonParser : ModelJsonParser<SourceOrder.Item> {
        override fun parse(json: JSONObject): SourceOrder.Item? {
            val type =
                SourceOrder.Item.Type.fromCode(StripeJsonUtils.optString(json, FIELD_TYPE))
            return if (type != null) {
                SourceOrder.Item(
                    type = type,
                    amount = StripeJsonUtils.optInteger(json, FIELD_AMOUNT),
                    currency = StripeJsonUtils.optString(json, FIELD_CURRENCY),
                    description = StripeJsonUtils.optString(json, FIELD_DESCRIPTION),
                    quantity = StripeJsonUtils.optInteger(json, FIELD_QUANTITY)
                )
            } else {
                null
            }
        }

        private companion object {
            private const val FIELD_TYPE = "type"
            private const val FIELD_AMOUNT = "amount"
            private const val FIELD_CURRENCY = "currency"
            private const val FIELD_DESCRIPTION = "description"
            private const val FIELD_QUANTITY = "quantity"
        }
    }

    internal class ShippingJsonParser : ModelJsonParser<SourceOrder.Shipping> {
        override fun parse(json: JSONObject): SourceOrder.Shipping {
            return SourceOrder.Shipping(
                address = json.optJSONObject(FIELD_ADDRESS)?.let {
                    AddressJsonParser().parse(it)
                },
                carrier = StripeJsonUtils.optString(json, FIELD_CARRIER),
                name = StripeJsonUtils.optString(json, FIELD_NAME),
                phone = StripeJsonUtils.optString(json, FIELD_PHONE),
                trackingNumber = StripeJsonUtils.optString(json, FIELD_TRACKING_NUMBER)
            )
        }

        private companion object {
            private const val FIELD_ADDRESS = "address"
            private const val FIELD_CARRIER = "carrier"
            private const val FIELD_NAME = "name"
            private const val FIELD_PHONE = "phone"
            private const val FIELD_TRACKING_NUMBER = "tracking_number"
        }
    }

    private companion object {
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_ITEMS = "items"
        private const val FIELD_SHIPPING = "shipping"
    }
}
