package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.SourceOrder.Item.Type
import kotlinx.android.parcel.Parcelize
import org.json.JSONArray
import org.json.JSONObject

/**
 * Information about the items and shipping associated with the source.
 * Required for transactional credit (for example Klarna) sources before you can charge it.
 *
 * [API reference](https://stripe.com/docs/api/sources/object#source_object-source_order)
 */
@Parcelize
data class SourceOrder internal constructor(
    /**
     * A positive integer in the smallest currency unit (that is, 100 cents for $1.00, or 1 for ¥1,
     * Japanese Yen being a zero-decimal currency) representing the total amount for the order.
     */
    val amount: Int? = null,

    /**
     * Three-letter [ISO currency code](https://www.iso.org/iso-4217-currency-codes.html),
     * in lowercase. Must be a [supported currency](https://stripe.com/docs/currencies).
     */
    val currency: String? = null,

    /**
     * The email address of the customer placing the order.
     */
    val email: String? = null,

    /**
     * List of items constituting the order.
     */
    val items: List<Item> = emptyList(),

    /**
     * The shipping address for the order. Present if the order is for goods to be shipped.
     */
    val shipping: Shipping? = null
) : Parcelable {
    /**
     * List of items constituting the order.
     *
     * [API reference](https://stripe.com/docs/api/sources/object#source_object-source_order-items)
     */
    @Parcelize
    data class Item internal constructor(
        /**
         * The type of this order item. Must be [Type.Sku], [Type.Tax], or [Type.Shipping].
         */
        val type: Type,

        /**
         * The amount (price) for this order item.
         */
        val amount: Int? = null,

        /**
         * This currency of this order item. Required when [amount] is present.
         */
        val currency: String? = null,

        /**
         * Human-readable description for this order item.
         */
        val description: String? = null,

        /**
         * The quantity of this order item. When type is [Type.Sku], this is the number of
         * instances of the SKU to be ordered.
         */
        val quantity: Int? = null
    ) : Parcelable {
        enum class Type(private val code: String) {
            Sku("sku"),
            Tax("tax"),
            Shipping("shipping");

            internal companion object {
                @JvmSynthetic
                internal fun fromCode(code: String?): Type? {
                    return values().firstOrNull { it.code == code }
                }
            }
        }

        internal companion object {
            private const val FIELD_TYPE = "type"
            private const val FIELD_AMOUNT = "amount"
            private const val FIELD_CURRENCY = "currency"
            private const val FIELD_DESCRIPTION = "description"
            private const val FIELD_QUANTITY = "quantity"

            @JvmSynthetic
            internal fun fromJson(json: JSONObject): Item? {
                val type = Type.fromCode(StripeJsonUtils.optString(json, FIELD_TYPE))
                return if (type != null) {
                    Item(
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
        }
    }

    /**
     * The shipping address for the order. Present if the order is for goods to be shipped.
     *
     * [API reference](https://stripe.com/docs/api/sources/object#source_object-source_order-shipping)
     */
    @Parcelize
    data class Shipping internal constructor(
        /**
         * Shipping address.
         */
        val address: Address? = null,

        /**
         * The delivery service that shipped a physical product, such as Fedex, UPS, USPS, etc.
         */
        val carrier: String? = null,

        /**
         * Recipient name.
         */
        val name: String? = null,

        /**
         * Recipient phone (including extension).
         */
        val phone: String? = null,

        /**
         * The tracking number for a physical product, obtained from the delivery service.
         * If multiple tracking numbers were generated for this purchase, please separate
         * them with commas.
         */
        val trackingNumber: String? = null
    ) : Parcelable {
        internal companion object {
            private const val FIELD_ADDRESS = "address"
            private const val FIELD_CARRIER = "carrier"
            private const val FIELD_NAME = "name"
            private const val FIELD_PHONE = "phone"
            private const val FIELD_TRACKING_NUMBER = "tracking_number"

            @JvmSynthetic
            internal fun fromJson(json: JSONObject): Shipping? {
                return Shipping(
                    address = Address.fromJson(json.optJSONObject(FIELD_ADDRESS)),
                    carrier = StripeJsonUtils.optString(json, FIELD_CARRIER),
                    name = StripeJsonUtils.optString(json, FIELD_NAME),
                    phone = StripeJsonUtils.optString(json, FIELD_PHONE),
                    trackingNumber = StripeJsonUtils.optString(json, FIELD_TRACKING_NUMBER)
                )
            }
        }
    }

    internal companion object {
        private const val FIELD_AMOUNT = "amount"
        private const val FIELD_CURRENCY = "currency"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_ITEMS = "items"
        private const val FIELD_SHIPPING = "shipping"

        @JvmSynthetic
        internal fun fromJson(json: JSONObject): SourceOrder {
            val itemsJson = json.optJSONArray(FIELD_ITEMS) ?: JSONArray()

            val items = (0 until itemsJson.length())
                .map { idx -> itemsJson.optJSONObject(idx) }
                .mapNotNull {
                    Item.fromJson(it)
                }
            return SourceOrder(
                amount = StripeJsonUtils.optInteger(json, FIELD_AMOUNT),
                currency = StripeJsonUtils.optString(json, FIELD_CURRENCY),
                email = StripeJsonUtils.optString(json, FIELD_EMAIL),
                items = items,
                shipping = json.optJSONObject(FIELD_SHIPPING)?.let { Shipping.fromJson(it) }
            )
        }
    }
}
