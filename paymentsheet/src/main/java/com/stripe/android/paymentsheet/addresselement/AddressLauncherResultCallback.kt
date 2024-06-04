package com.stripe.android.paymentsheet.addresselement

/**
 * Callback that is invoked when a [AddressLauncherResult] is available.
 */
fun interface AddressLauncherResultCallback {
    fun onAddressLauncherResult(addressLauncherResult: AddressLauncherResult)
}
