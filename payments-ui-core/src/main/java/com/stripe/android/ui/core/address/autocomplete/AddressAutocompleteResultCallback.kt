package com.stripe.android.ui.core.address.autocomplete

import com.stripe.android.model.Address

/**
 * Callback that is invoked when the customer's [ShippingAddress] changes.
 */
fun interface AddressAutocompleteResultCallback {

    /**
     * @param address The new [ShippingAddress]. If null, the customer has not yet
     * selected a [ShippingAddress].
     */
    fun onAddress(address: Address?)
}
