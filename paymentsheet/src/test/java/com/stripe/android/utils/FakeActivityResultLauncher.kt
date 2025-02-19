package com.stripe.android.utils

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine

class FakeActivityResultLauncher<I> : ActivityResultLauncher<I>() {
    private val _calls = Turbine<Call<I>>()
    val calls: ReceiveTurbine<Call<I>> = _calls

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        _calls.add(Call(input))
    }

    override fun unregister() {
        throw NotImplementedError("Not implemented!")
    }

    override val contract: ActivityResultContract<I, *>
        get() = throw NotImplementedError("Not implemented!")

    data class Call<I>(
        val input: I,
    )
}
