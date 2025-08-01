package com.stripe.android.elements.payment

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Configuration related to Shop Pay, which only applies when using wallet buttons.
 *
 * @param shopId The corresponding store's shopId.
 * @param billingAddressRequired Whether or not billing address is required. Defaults to `true`.
 * @param emailRequired Whether or not email is required. Defaults to `true`.
 * @param shippingAddressRequired Whether or not to collect the customer's shipping address.
 * @param lineItems An array of [LineItem] objects. These are shown as line items in the
 * payment interface, if line items are supported. You can represent discounts as negative
 * amount [LineItem]s.
 * @param shippingRates A list of [ShippingRate] objects. The first shipping rate listed
 * appears in the payment interface as the default option.
 */
@Poko
@Parcelize
class ShopPayConfiguration(
    val shopId: String,
    val billingAddressRequired: Boolean = true,
    val emailRequired: Boolean = true,
    val shippingAddressRequired: Boolean,
    val allowedShippingCountries: List<String>,
    val lineItems: List<LineItem>,
    val shippingRates: List<ShippingRate>
) : Parcelable {
    /**
     * A type used to describe a single item for in the Shop Pay wallet UI.
     */
    @Poko
    @Parcelize
    class LineItem(
        val name: String,
        val amount: Int
    ) : Parcelable

    /**
     * A shipping rate option.
     */
    @Poko
    @Parcelize
    class ShippingRate(
        val id: String,
        val amount: Int,
        val displayName: String,
        val deliveryEstimate: DeliveryEstimate?
    ) : Parcelable

    /**
     * Type used to describe DeliveryEstimates for shipping.
     * See https://docs.stripe.com/js/elements_object/create_express_checkout_element#express_checkout_element_create-options-shippingRates-deliveryEstimate
     */
    sealed interface DeliveryEstimate : Parcelable {
        @Poko
        @Parcelize
        class Range(
            val maximum: DeliveryEstimateUnit?,
            val minimum: DeliveryEstimateUnit?
        ) : DeliveryEstimate

        @Poko
        @Parcelize
        class Text(
            val value: String
        ) : DeliveryEstimate

        @Poko
        @Parcelize
        class DeliveryEstimateUnit(
            val unit: TimeUnit,
            val value: Int
        ) : Parcelable {

            enum class TimeUnit {
                HOUR,
                DAY,
                BUSINESS_DAY,
                WEEK,
                MONTH
            }
        }
    }
}
