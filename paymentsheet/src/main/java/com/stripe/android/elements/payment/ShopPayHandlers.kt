package com.stripe.android.elements.payment

import android.os.Parcelable
import com.stripe.android.elements.payment.PaymentSheet.ShopPayConfiguration.LineItem
import com.stripe.android.elements.payment.PaymentSheet.ShopPayConfiguration.ShippingRate
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

/**
 * Handler blocks for Shop Pay.
 */
@ShopPayPreview
class ShopPayHandlers(
    val shippingMethodUpdateHandler: ShippingMethodHandler,
    val shippingContactHandler: ShippingContactHandler
) {
    /**
     * Describes shipping rate updates Shop Pay should within its UI
     */
    @Poko
    @Parcelize
    class ShippingRateUpdate(
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>,
    ) : Parcelable

    /**
     * Describes shipping contact updates Shop Pay should make within its UI
     */
    @Poko
    @Parcelize
    class ShippingContactUpdate(
        val lineItems: List<LineItem>,
        val shippingRates: List<ShippingRate>
    ) : Parcelable

    /**
     * Describes the address selected by the customer.
     */
    @Poko
    @Parcelize
    class SelectedAddress(
        val city: String,
        val state: String,
        val postalCode: String,
        val country: String
    ) : Parcelable

    /**
     * The shipping rate selected by the customer.
     */
    @Poko
    @Parcelize
    class SelectedShippingRate(
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
         * @return A [ShippingRateUpdate] with your updates. To reject this selection, return `null`.
         *
         * **Note**: If this function does not complete, the app will hang.
         */
        suspend fun onRateSelected(
            selectedRate: SelectedShippingRate,
        ): ShippingRateUpdate?
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
         * @param address The [SelectedAddress] that was selected by the user.
         * @return A [ShippingContactUpdate] with your updates. To reject this selection,
         * return `null`.
         *
         * **Note**: If this function does not complete, the app will hang.
         */
        suspend fun onAddressSelected(
            address: SelectedAddress,
        ): ShippingContactUpdate?
    }
}
