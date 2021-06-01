package com.stripe.android.googlepaysheet

import com.stripe.android.googlepay.StripeGooglePayContract

internal fun interface GooglePaySheetResultCallback {
    fun onResult(result: StripeGooglePayContract.Result)
}
