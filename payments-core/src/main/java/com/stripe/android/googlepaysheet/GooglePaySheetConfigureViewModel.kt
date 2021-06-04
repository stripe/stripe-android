package com.stripe.android.googlepaysheet

import androidx.lifecycle.ViewModel

internal class GooglePaySheetConfigureViewModel : ViewModel() {
    private var _args: StripeGooglePayContract.Args? = null

    fun setArgs(args: StripeGooglePayContract.Args) {
        _args = args
    }

    val args: StripeGooglePayContract.Args get() = requireNotNull(_args)
}
