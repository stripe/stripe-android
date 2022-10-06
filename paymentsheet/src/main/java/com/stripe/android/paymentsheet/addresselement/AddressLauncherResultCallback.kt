package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.RestrictTo

/**
 * Callback that is invoked when a [AddressLauncherResult] is available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AddressLauncherResultCallback {
    fun onAddressLauncherResult(addressLauncherResult: AddressLauncherResult)
}
