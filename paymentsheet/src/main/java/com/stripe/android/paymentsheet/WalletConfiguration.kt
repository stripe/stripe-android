package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.model.Address
import com.stripe.android.paymentsheet.WalletConfiguration.PaymentRequestShippingContactUpdate
import com.stripe.android.paymentsheet.WalletConfiguration.PaymentRequestShippingRateUpdate
import com.stripe.android.paymentsheet.WalletConfiguration.SelectedPartialAddress
import com.stripe.android.paymentsheet.WalletConfiguration.SelectedShippingRate
import kotlinx.parcelize.Parcelize

fun interface PaymentRequestShippingRateUpdateHandler {
    fun onUpdate(update: PaymentRequestShippingRateUpdate)
}

fun interface PaymentRequestShippingContactUpdateHandler {
    fun onUpdate(update: PaymentRequestShippingContactUpdate)
}

/**
 * Callback type for shipping method updates.
 * @param selectedRate The shipping rate selected by the user
 * @param callback Callback to update the line items and shipping rates
 */
typealias ShippingMethodUpdateHandler = (SelectedShippingRate, PaymentRequestShippingRateUpdateHandler) -> Unit

/**
 * Callback type for shipping contact updates.
 * @param selectedAddress The address selected by the user
 * @param callback Callback to update the line items and shipping rates
 */
typealias ShippingContactUpdateHandler = (SelectedPartialAddress, PaymentRequestShippingContactUpdateHandler) -> Unit

/**
 * Configuration related to Wallets. Currently this applies to Google Pay and ShopPay.
 */
@Parcelize
data class WalletConfiguration(
    /**
     * Custom handlers for wallet-specific events.
     */
    val customHandlers: Handlers? = null
) : Parcelable {

    /**
     * Handlers for wallet-specific events.
     */
    @Parcelize
    data class Handlers(
        /**
         * Called when the shipping method is updated in the wallet UI.
         * The callback should update the line items and available shipping rates based on the selected shipping method.
         */
        val shippingMethodUpdateHandler: ShippingMethodUpdateHandler? = null,

        /**
         * Called when the shipping address is updated in the wallet UI.
         * The callback should update the line items and available shipping rates based on the selected address.
         */
        val shippingContactUpdateHandler: ShippingContactUpdateHandler? = null
    ) : Parcelable

    /**
     * Selected shipping rate in the wallet UI.
     */
    @Parcelize
    data class SelectedShippingRate(
        val name: String,
        val rate: String
    ) : Parcelable

    /**
     * Selected partial address in the wallet UI.
     */
    @Parcelize
    data class SelectedPartialAddress(
        val city: String,
        val state: String,
        val postalCode: String,
        val country: String
    ) : Parcelable

    /**
     * Update to be sent to the wallet UI when shipping rates change.
     */
    @Parcelize
    data class PaymentRequestShippingRateUpdate(
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>
    ) : Parcelable

    /**
     * Update to be sent to the wallet UI when shipping contact changes.
     */
    @Parcelize
    data class PaymentRequestShippingContactUpdate(
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>
    ) : Parcelable

    /**
     * A line item in the order summary.
     */
    @Parcelize
    data class LineItem(
        val name: String,
        val amount: Int
    ) : Parcelable

    /**
     * A shipping rate option.
     */
    @Parcelize
    data class ShippingRate(
        val id: String,
        val amount: Int,
        val displayName: String,
        val deliveryEstimate: DeliveryEstimate? = null
    ) : Parcelable

    /**
     * Estimated delivery time range.
     */
    @Parcelize
    data class DeliveryEstimate(
        val maximum: DeliveryEstimateUnit,
        val minimum: DeliveryEstimateUnit
    ) : Parcelable {

        /**
         * A unit of time for delivery estimates.
         */
        @Parcelize
        data class DeliveryEstimateUnit(
            val unit: TimeUnit,
            val value: Int
        ) : Parcelable {

            /**
             * Time unit for delivery estimates.
             */
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