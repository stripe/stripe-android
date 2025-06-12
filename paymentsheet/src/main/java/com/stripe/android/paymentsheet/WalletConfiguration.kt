package com.stripe.android.paymentsheet

import android.os.Parcelable
import org.json.JSONObject
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
    ) : Parcelable {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("name", name)
                put("rate", rate)
            }
        }
    }

    /**
     * Selected partial address in the wallet UI.
     */
    @Parcelize
    data class SelectedPartialAddress(
        val city: String,
        val state: String,
        val postalCode: String,
        val country: String
    ) : Parcelable {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("city", city)
                put("state", state)
                put("postalCode", postalCode)
                put("country", country)
            }
        }
    }

    /**
     * Update to be sent to the wallet UI when shipping rates change.
     */
    @Parcelize
    data class PaymentRequestShippingRateUpdate(
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>
    ) : Parcelable {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("lineItems", JSONObject().apply {
                    lineItems.forEach { lineItem ->
                        put(lineItem.name, lineItem.toJson())
                    }
                })
                put("shippingRates", JSONObject().apply {
                    shippingRates.forEach { shippingRate ->
                        put(shippingRate.id, shippingRate.toJson())
                    }
                })
            }
        }
    }

    /**
     * Update to be sent to the wallet UI when shipping contact changes.
     */
    @Parcelize
    data class PaymentRequestShippingContactUpdate(
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>
    ) : Parcelable {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("lineItems", JSONObject().apply {
                    lineItems.forEach { lineItem ->
                        put(lineItem.name, lineItem.toJson())
                    }
                })
                put("shippingRates", JSONObject().apply {
                    shippingRates.forEach { shippingRate ->
                        put(shippingRate.id, shippingRate.toJson())
                    }
                })
            }
        }
    }

    /**
     * A line item in the order summary.
     */
    @Parcelize
    data class LineItem(
        val name: String,
        val amount: Int
    ) : Parcelable {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("name", name)
                put("amount", amount)
            }
        }
    }

    /**
     * A shipping rate option.
     */
    @Parcelize
    data class ShippingRate(
        val id: String,
        val amount: Int,
        val displayName: String,
        val deliveryEstimate: DeliveryEstimate? = null
    ) : Parcelable {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("amount", amount)
                put("displayName", displayName)
                deliveryEstimate?.let { estimate ->
                    when (val json = estimate.toJson()) {
                        is JSONObject -> put("deliveryEstimate", json)
                        is String -> put("deliveryEstimate", json)
                    }
                }
            }
        }
    }

        /**
     * Estimated delivery time range. Can be either an object with min/max bounds or a simple string.
     */
    sealed interface DeliveryEstimate : Parcelable {
        fun toJson(): Any

        /**
         * Object format with minimum and maximum delivery estimates.
         */
        @Parcelize
        data class Range(
            val maximum: DeliveryEstimateUnit,
            val minimum: DeliveryEstimateUnit
        ) : DeliveryEstimate {
            
            override fun toJson(): JSONObject {
                return JSONObject().apply {
                    put("maximum", maximum.toJson())
                    put("minimum", minimum.toJson())
                }
            }
        }

        /**
         * Simple string format for delivery estimate.
         */
        @Parcelize
        data class Text(
            val value: String
        ) : DeliveryEstimate {
            
            override fun toJson(): String = value
        }

        /**
         * A unit of time for delivery estimates.
         */
        @Parcelize
        data class DeliveryEstimateUnit(
            val unit: TimeUnit,
            val value: Int
        ) : Parcelable {
            
            fun toJson(): JSONObject {
                return JSONObject().apply {
                    put("unit", unit.name.lowercase())
                    put("value", value)
                }
            }

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