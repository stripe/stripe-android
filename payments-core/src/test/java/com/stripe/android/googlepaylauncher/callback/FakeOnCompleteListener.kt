package com.stripe.android.googlepaylauncher.callback

import app.cash.turbine.Turbine
import com.google.android.gms.wallet.callback.OnCompleteListener
import com.google.android.gms.wallet.callback.PaymentDataRequestUpdate

internal class FakeOnCompleteListener : OnCompleteListener<PaymentDataRequestUpdate> {
    val completions = Turbine<PaymentDataRequestUpdate>()

    override fun complete(result: PaymentDataRequestUpdate) {
        completions.add(result)
    }

    fun ensureAllEventsConsumed() {
        completions.ensureAllEventsConsumed()
    }
}
