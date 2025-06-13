package com.stripe.android.paymentsheet

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.LineItem
import com.stripe.android.paymentsheet.PaymentSheet.ShopPayConfiguration.ShippingRate
import kotlinx.parcelize.Parcelize

/**
 * Handler blocks for Shop Pay.
 */
data class ShopPayHandlers(
    val shippingMethodUpdateHandler: ShippingMethodHandler,
    val shippingContactHandler: ShippingContactHandler
)

/**
 * Type used to describe convey changes in the Shop Pay wallet UI when a shipping rate update occurs.
 */
@Parcelize
data class ShippingRateUpdate(
    val lineItems: List<LineItem>,
    val shippingRates: List<ShippingRate>,
) : Parcelable

/**
 * Type used to describe convey changes in the Shop Pay wallet UI when a shipping contact update occurs.
 */
@Parcelize
data class ShippingContactUpdate(
    val lineItems: List<LineItem>,
    val shippingRates: List<ShippingRate>
) : Parcelable

/**
 * Describes the address selected by the customer.
 */
@Parcelize
data class SelectedPartialAddress(
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
) : Parcelable

/**
 * The shipping rate selected by the customer.
 */
@Parcelize
data class SelectedShippingRate(
    val shippingRate: ShippingRate
) : Parcelable

/**
 * This handler is called when the user selects a new shipping option.
 *
 * Use this to get shipping method updates if you've configured shipping method options.
 *
 * @see onRateSelected
 */
fun interface ShippingMethodHandler {
    /**
     * @param selectedRate The [SelectedShippingRate] that was selected by the user.
     * @param updateCallback A completion handler. You must call this handler with a
     * [ShippingRateUpdate] with your updates. To reject this selection, pass `null`
     * into this handler.
     *
     * **Note**: If you do not call the completion handler, the app will hang.
     */
    fun onRateSelected(
        selectedRate: SelectedShippingRate,
        updateCallback: (ShippingRateUpdate?) -> Unit
    )
}

/**
 * This handler is called when the user selects a new shipping address.
 *
 * Use this to get shipping contact updates if you've configured shipping contact options.
 *
 * @see onAddressSelected
 */
fun interface ShippingContactHandler {
    /**
     * @param address The [SelectedPartialAddress] that was selected by the user.
     * @param updateCallback A completion handler. You must call this handler with a
     * [ShippingContactUpdate] with your updates. To reject this selection,
     * pass `null` into this handler.
     *
     * **Note**: If you do not call the completion handler, the app will hang.
     */
    fun onAddressSelected(
        address: SelectedPartialAddress,
        updateCallback: (ShippingContactUpdate?) -> Unit
    )
}
