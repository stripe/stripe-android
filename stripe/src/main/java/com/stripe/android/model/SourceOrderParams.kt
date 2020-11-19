package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.SourceOrderParams.Item.Type
import kotlinx.parcelize.Parcelize

/**
 * Information about the items and shipping associated with the source. Required for transactional
 * credit (for example Klarna) sources before you can charge it.
 *
 * [API reference](https://stripe.com/docs/api/sources/create#create_source-source_order)
 */
@Parcelize
data class SourceOrderParams @JvmOverloads constructor(
    /**
     * List of items constituting the order.
     */
    val items: List<Item>? = null,

    /**
     * Shipping address for the order. Required if any of the SKUs are for products that have
     * `shippable` set to true.
     */
    val shipping: Shipping? = null
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return emptyMap<String, Any>()
            .plus(
                items?.let {
                    mapOf(PARAM_ITEMS to it.map { item -> item.toParamMap() })
                }.orEmpty()
            ).plus(
                shipping?.let { mapOf(PARAM_SHIPPING to it.toParamMap()) }.orEmpty()
            )
    }

    /**
     * List of items constituting the order.
     *
     * [API reference](https://stripe.com/docs/api/sources/create#create_source-source_order-items)
     */
    @Parcelize
    data class Item(
        /**
         * Optional. The type of this order item.
         * Must be [Type.Sku], [Type.Tax], or [Type.Shipping].
         */
        val type: Type? = null,

        /**
         * Optional. The amount (price) for this order item.
         */
        val amount: Int? = null,

        /**
         * Optional. This currency of this order item. Required when amount is present.
         */
        val currency: String? = null,

        /**
         * Optional. Human-readable description for this order item.
         */
        val description: String? = null,

        /**
         * Optional. The ID of the SKU being ordered.
         */
        val parent: String? = null,

        /**
         * Optional. The quantity of this order item. When type is [Type.Sku], this is the number of
         * instances of the SKU to be ordered.
         */
        val quantity: Int? = null
    ) : StripeParamsModel, Parcelable {

        override fun toParamMap(): Map<String, Any> {
            return emptyMap<String, Any>()
                .plus(
                    amount?.let { mapOf(PARAM_AMOUNT to it) }.orEmpty()
                )
                .plus(
                    currency?.let { mapOf(PARAM_CURRENCY to it) }.orEmpty()
                )
                .plus(
                    description?.let { mapOf(PARAM_DESCRIPTION to it) }.orEmpty()
                )
                .plus(
                    parent?.let { mapOf(PARAM_PARENT to it) }.orEmpty()
                )
                .plus(
                    quantity?.let { mapOf(PARAM_QUANTITY to it) }.orEmpty()
                )
                .plus(
                    type?.let { mapOf(PARAM_TYPE to it.code) }.orEmpty()
                )
        }

        enum class Type(internal val code: String) {
            Sku("sku"),
            Tax("tax"),
            Shipping("shipping")
        }

        private companion object {
            private const val PARAM_AMOUNT = "amount"
            private const val PARAM_CURRENCY = "currency"
            private const val PARAM_DESCRIPTION = "description"
            private const val PARAM_PARENT = "parent"
            private const val PARAM_QUANTITY = "quantity"
            private const val PARAM_TYPE = "type"
        }
    }

    /**
     * Shipping address for the order.
     * Required if any of the SKUs are for products that have `shippable` set to true.
     *
     * [API reference](https://stripe.com/docs/api/sources/create#create_source-source_order-shipping)
     */
    @Parcelize
    data class Shipping(
        /**
         * Required. Shipping address.
         */
        val address: Address,

        /**
         * Optional. The delivery service that shipped a physical product,
         * such as Fedex, UPS, USPS, etc.
         */
        val carrier: String? = null,

        /**
         * Optional. Recipient name.
         */
        val name: String? = null,

        /**
         * Optional. Recipient phone (including extension).
         */
        val phone: String? = null,

        /**
         * Optional. The tracking number for a physical product, obtained from the delivery service.
         * If multiple tracking numbers were generated for this purchase, please separate
         * them with commas.
         */
        val trackingNumber: String? = null
    ) : StripeParamsModel, Parcelable {

        override fun toParamMap(): Map<String, Any> {
            return mapOf(
                PARAM_ADDRESS to address.toParamMap()
            ).plus(
                carrier?.let { mapOf(PARAM_CARRIER to it) }.orEmpty()
            ).plus(
                name?.let { mapOf(PARAM_NAME to it) }.orEmpty()
            ).plus(
                phone?.let { mapOf(PARAM_PHONE to it) }.orEmpty()
            ).plus(
                trackingNumber?.let { mapOf(PARAM_TRACKING_NUMBER to it) }.orEmpty()
            )
        }

        private companion object {
            private const val PARAM_ADDRESS = "address"
            private const val PARAM_CARRIER = "carrier"
            private const val PARAM_NAME = "name"
            private const val PARAM_PHONE = "phone"
            private const val PARAM_TRACKING_NUMBER = "tracking_number"
        }
    }

    private companion object {
        private const val PARAM_ITEMS = "items"
        private const val PARAM_SHIPPING = "shipping"
    }
}
