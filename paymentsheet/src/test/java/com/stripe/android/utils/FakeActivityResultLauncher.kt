package com.stripe.android.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

class FakeActivityResultLauncher<I> : ActivityResultLauncher<I>() {
    private val _calls = Turbine<Call<I>>()
    val calls: ReceiveTurbine<Call<I>> = _calls
    private val _unregisterCalls = Turbine<Unit>()
    val unregisterCalls: ReceiveTurbine<Unit> = _unregisterCalls

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        _calls.add(Call(input))
    }

    override fun unregister() {
        _unregisterCalls.add(Unit)
    }

    override val contract: ActivityResultContract<I, *>
        get() = throw NotImplementedError("Not implemented!")

    data class Call<I>(
        val input: I,
    )
}
