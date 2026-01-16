package com.stripe.android.googlepaylauncher

import androidx.annotation.NonNull
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacks
import com.google.android.gms.wallet.callback.BasePaymentDataCallbacksService

class GooglePayDynamicUpdateService : BasePaymentDataCallbacksService() {
    @NonNull
    override fun createPaymentDataCallbacks(): BasePaymentDataCallbacks {
        return GooglePayDynamicUpdateCallbacks
    }
}
