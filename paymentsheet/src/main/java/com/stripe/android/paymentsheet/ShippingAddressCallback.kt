package com.stripe.android.paymentsheet

import com.stripe.android.paymentsheet.addresselement.ShippingAddress

/**
 * Callback that is invoked when the customer's [ShippingAddress] changes
 * TODO finalize this api
 */
fun interface ShippingAddressCallback {

    fun onShippingAddressCollected(shippingAddress: ShippingAddress?)
}