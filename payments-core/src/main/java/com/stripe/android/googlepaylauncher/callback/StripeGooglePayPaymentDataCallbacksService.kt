package com.stripe.android.googlepaylauncher.callback

import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacksService

internal class StripeGooglePayPaymentDataCallbacksService : BasePaymentDataCallbacksService() {
    override fun createPaymentDataCallbacks(): BasePaymentDataCallbacks {
        return StripeGooglePayPaymentDataCallbacks(applicationContext)
    }
}
