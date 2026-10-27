package com.stripe.android.googlepaylauncher.callback

import app.cash.turbine.Turbine
import com.google.android.gms.wallet.callback.OnCompleteListener

internal class FakeOnCompleteListener<T : Any> : OnCompleteListener<T> {
    val completions = Turbine<T>()

    override fun complete(result: T) {
        completions.add(result)
    }

    fun ensureAllEventsConsumed() {
        completions.ensureAllEventsConsumed()
    }
}
