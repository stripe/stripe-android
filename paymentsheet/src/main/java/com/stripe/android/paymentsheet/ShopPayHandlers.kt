package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.LineItem
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.ShippingRate
import kotlinx.parcelize.Parcelize

data class ShopPayHandlers(
    val shippingRateHandler: ShippingRateHandler,
    val shippingContactHandler: ShippingContactHandler
)

/**
 * Update to be sent to the wallet UI when shipping rates change.
 */
@Parcelize
data class PaymentRequestShippingRateUpdate(
    val lineItems: List<LineItem>?,
    val shippingRates: List<ShippingRate>?,
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
 * Selected shipping rate in the wallet UI.
 */
@Parcelize
data class SelectedShippingRate(
    val name: String,
    val rate: String
) : Parcelable

// Handler for updating shipping rates
fun interface ShippingRateHandler {
    fun onRateSelected(
        selectedRate: SelectedShippingRate,
        updateCallback: (PaymentRequestShippingRateUpdate?) -> Unit
    )
}

// Handler for updating shipping contact information
fun interface ShippingContactHandler {
    fun onAddressSelected(
        address: SelectedPartialAddress,
        updateCallback: (PaymentRequestShippingContactUpdate?) -> Unit
    )
}
