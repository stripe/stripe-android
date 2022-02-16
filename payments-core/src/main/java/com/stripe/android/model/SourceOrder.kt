package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.SourceOrder.Item.Type
import kotlinx.parcelize.Parcelize

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
) : StripeModel {
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
    ) : StripeModel {
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
    ) : StripeModel
}
